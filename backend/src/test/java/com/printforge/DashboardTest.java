package com.printforge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.printforge.entity.Category;
import com.printforge.entity.Product;
import com.printforge.entity.Role;
import com.printforge.repository.CategoryRepository;
import com.printforge.repository.ProductRepository;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Dashboard totals from aggregate queries must match what the orders actually add up to. */
@SpringBootTest @AutoConfigureMockMvc
class DashboardTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired TestTokens tokens;

  private String admin() { return "Bearer " + tokens.tokenFor("dashboard-admin@example.com", Role.ADMIN); }
  private String customer() { return "Bearer " + tokens.tokenFor("dashboard-customer@example.com", Role.CUSTOMER); }

  private JsonNode dashboard() throws Exception {
    return json.readTree(mvc.perform(get("/api/admin/dashboard").header("Authorization", admin()))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  }

  private long placeOrder(Product product) throws Exception {
    var body = Map.of("items", List.of(Map.of("productId", product.getId(), "quantity", 1)),
      "shippingName", "Jane", "shippingPhone", "0771234567", "shippingAddress", "1 Road", "shippingCity", "Colombo", "shippingPostalCode", "00300");
    return json.readTree(mvc.perform(post("/api/orders").header("Authorization", customer()).contentType("application/json").content(json.writeValueAsString(body)))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
  }

  @Test void totalsCountPendingOrdersAndExcludeCancelledRevenue() throws Exception {
    Category category = categories.save(new Category("Dashboard Category", "desc"));
    Product product = new Product();
    product.setName("Dashboard Stand");
    product.setDescription("desc");
    product.setPrice(new BigDecimal("1000.00"));
    product.setStockQuantity(10);
    product.setMaterial("PLA");
    product.setColor("Black");
    product.setCategory(category);
    product = products.save(product);

    JsonNode before = dashboard();
    long kept = placeOrder(product);
    long cancelled = placeOrder(product);
    mvc.perform(put("/api/orders/" + cancelled + "/cancel").header("Authorization", customer())).andExpect(status().isOk());
    JsonNode after = dashboard();

    assertEquals(before.get("totalOrders").asLong() + 2, after.get("totalOrders").asLong());
    assertEquals(before.get("pendingOrders").asLong() + 1, after.get("pendingOrders").asLong(), "only the kept order is pending");
    // 1000 + 350 delivery for the kept order; the cancelled one adds nothing.
    assertEquals(0, before.get("revenue").decimalValue().add(new BigDecimal("1350.00")).compareTo(after.get("revenue").decimalValue()));

    JsonNode recent = after.get("recentOrders");
    assertTrue(recent.size() <= 5);
    var recentIds = new java.util.HashSet<Long>();
    recent.forEach(order -> recentIds.add(order.get("id").asLong()));
    assertTrue(recentIds.containsAll(List.of(kept, cancelled)), "the two newest orders are listed");
  }
}
