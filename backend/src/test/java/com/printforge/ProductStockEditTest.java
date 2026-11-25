package com.printforge;

import tools.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Saving the product edit form must not undo stock changes made by orders while it was open. */
@SpringBootTest @AutoConfigureMockMvc
class ProductStockEditTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired TestTokens tokens;

  private Product newProduct(String name, int stock) {
    Category category = categories.save(new Category(name + " Category", "desc"));
    Product product = new Product();
    product.setName(name);
    product.setDescription("desc");
    product.setPrice(new BigDecimal("100.00"));
    product.setStockQuantity(stock);
    product.setMaterial("PLA");
    product.setColor("Black");
    product.setCategory(category);
    return products.save(product);
  }

  /** Simulates a customer buying while the admin has the edit form open. */
  private void sell(Product product, int quantity) throws Exception {
    var body = Map.of("items", List.of(Map.of("productId", product.getId(), "quantity", quantity)),
      "shippingName", "Jane", "shippingPhone", "0771234567", "shippingAddress", "1 Road", "shippingCity", "Colombo", "shippingPostalCode", "00300");
    mvc.perform(post("/api/orders").header("Authorization", "Bearer " + tokens.tokenFor("stock-edit-customer@example.com", Role.CUSTOMER))
        .contentType("application/json").content(json.writeValueAsString(body)))
      .andExpect(status().isOk());
  }

  /** Saves the edit form as the frontend does; {@code expected} is the stock the form was loaded with. */
  private ResultActions save(Product product, String newName, int stockField, Integer expected) throws Exception {
    Map<String, Object> body = new HashMap<>(Map.of("name", newName, "description", "desc", "price", "100.00", "stockQuantity", stockField,
      "material", "PLA", "color", "Black", "imageUrl", "", "categoryId", product.getCategory().getId(), "active", true));
    if (expected != null) body.put("expectedStockQuantity", expected);
    return mvc.perform(put("/api/products/" + product.getId()).header("Authorization", "Bearer " + tokens.tokenFor("stock-edit-admin@example.com", Role.ADMIN))
      .contentType("application/json").content(json.writeValueAsString(body)));
  }

  private int stockOf(Product product) { return products.findById(product.getId()).orElseThrow().getStockQuantity(); }

  @Test void untouchedStockFieldKeepsSalesMadeWhileEditing() throws Exception {
    Product product = newProduct("Edit Keeps Sales", 10);
    // Form opened at 10; 3 sell meanwhile; admin only renames.
    sell(product, 3);

    save(product, "Renamed Stand", 10, 10)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("Renamed Stand"))
      .andExpect(jsonPath("$.stockQuantity").value(7));
    assertEquals(7, stockOf(product));
  }

  @Test void editedStockIsSavedWhenNothingSoldMeanwhile() throws Exception {
    Product product = newProduct("Edit Restock", 10);

    save(product, "Edit Restock", 25, 10).andExpect(status().isOk()).andExpect(jsonPath("$.stockQuantity").value(25));
    assertEquals(25, stockOf(product));
  }

  @Test void editedStockIsRejectedWhenStockMovedMeanwhile() throws Exception {
    Product product = newProduct("Edit Conflict", 10);
    sell(product, 4);

    save(product, "Edit Conflict Renamed", 25, 10)
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("from 10 to 6")));
    Product unchanged = products.findById(product.getId()).orElseThrow();
    assertEquals(6, unchanged.getStockQuantity());
    assertEquals("Edit Conflict", unchanged.getName(), "a rejected save must not apply any other field either");
  }

  @Test void saveSucceedsAfterReloadingTheCurrentStock() throws Exception {
    Product product = newProduct("Edit Reload", 10);
    sell(product, 4);
    save(product, "Edit Reload", 25, 10).andExpect(status().isConflict());

    // "Load current stock" makes 6 the new baseline; the admin then enters 20.
    save(product, "Edit Reload", 20, 6).andExpect(status().isOk());
    assertEquals(20, stockOf(product));
  }

  @Test void withoutABaselineStockIsOverwrittenAsBefore() throws Exception {
    Product product = newProduct("Edit No Baseline", 10);
    sell(product, 4);

    save(product, "Edit No Baseline", 15, null).andExpect(status().isOk());
    assertEquals(15, stockOf(product));
  }
}
