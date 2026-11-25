package com.printforge;

import tools.jackson.databind.ObjectMapper;
import com.printforge.config.ShopSettings;
import com.printforge.entity.Category;
import com.printforge.entity.Product;
import com.printforge.entity.Role;
import com.printforge.repository.CategoryRepository;
import com.printforge.repository.ProductRepository;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Customers must be charged exactly the total they were shown. */
@SpringBootTest @AutoConfigureMockMvc
class CartPricingTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired TestTokens tokens;

  private Product newProduct(String name, String price, boolean active) {
    Category category = categories.save(new Category(name + " Category", "desc"));
    Product product = new Product();
    product.setName(name);
    product.setDescription("desc");
    product.setPrice(new BigDecimal(price));
    product.setStockQuantity(10);
    product.setMaterial("PLA");
    product.setColor("Black");
    product.setActive(active);
    product.setCategory(category);
    return products.save(product);
  }

  private ResultActions order(Product product, int quantity, String expectedTotal) throws Exception {
    Map<String, Object> body = new HashMap<>(Map.of("items", List.of(Map.of("productId", product.getId(), "quantity", quantity)),
      "shippingName", "Jane", "shippingPhone", "0771234567", "shippingAddress", "1 Road", "shippingCity", "Colombo", "shippingPostalCode", "00300"));
    if (expectedTotal != null) body.put("expectedTotal", expectedTotal);
    return mvc.perform(post("/api/orders").header("Authorization", "Bearer " + tokens.tokenFor("pricing-customer@example.com", Role.CUSTOMER))
      .contentType("application/json").content(json.writeValueAsString(body)));
  }

  private int stockOf(Product product) { return products.findById(product.getId()).orElseThrow().getStockQuantity(); }

  @Test void shopSettingsArePublic() throws Exception {
    mvc.perform(get("/api/shop/settings"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.deliveryFee").value(350))
      .andExpect(jsonPath("$.freeDeliveryThreshold").value(5000));
  }

  @Test void productsCanBeFetchedByIdsAndInactiveOnesAreLeftOut() throws Exception {
    Product a = newProduct("Ids A", "100.00", true);
    Product b = newProduct("Ids B", "200.00", true);
    Product hidden = newProduct("Ids Hidden", "300.00", false);

    mvc.perform(get("/api/products").param("ids", a.getId() + "," + b.getId() + "," + hidden.getId() + ",999999"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[*].name", containsInAnyOrder("Ids A", "Ids B")));
  }

  @Test void orderMatchingTheShownTotalSucceeds() throws Exception {
    Product product = newProduct("Price Match", "1000.00", true);
    order(product, 2, "2350.00").andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(2350.00));
  }

  @Test void orderIsRejectedWhenThePriceChangedAfterItWasShown() throws Exception {
    Product product = newProduct("Price Changed", "1000.00", true);
    product.setPrice(new BigDecimal("1200.00"));
    products.save(product);

    // Customer's cart still shows 2 x 1000 + 350.
    order(product, 2, "2350.00")
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("2750.00")));
    assertEquals(10, stockOf(product), "a rejected order must not take stock");
  }

  @Test void ordersWithoutAShownTotalArePricedAsBefore() throws Exception {
    Product product = newProduct("No Expected Total", "1000.00", true);
    order(product, 1, null).andExpect(status().isOk()).andExpect(jsonPath("$.deliveryFee").value(350));
  }

  @Test void deliveryIsFreeAtOrAboveTheThreshold() throws Exception {
    Product product = newProduct("Free Delivery", "2500.00", true);
    order(product, 2, "5000.00").andExpect(status().isOk())
      .andExpect(jsonPath("$.deliveryFee").value(0))
      .andExpect(jsonPath("$.totalAmount").value(5000.00));
  }

  @Test void thresholdCanBeSwitchedOff() {
    ShopSettings off = new ShopSettings(new BigDecimal("350"), "0");
    assertEquals(null, off.freeDeliveryThreshold());
    assertEquals(0, new BigDecimal("350").compareTo(off.deliveryFeeFor(new BigDecimal("99999"))));
  }
}
