package com.printforge;

import tools.jackson.databind.JsonNode;
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

/** Two admins editing the same product must not silently overwrite each other. */
@SpringBootTest @AutoConfigureMockMvc
class ProductEditConflictTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired TestTokens tokens;

  private String admin() { return "Bearer " + tokens.tokenFor("edit-conflict-admin@example.com", Role.ADMIN); }

  private Product newProduct(String name) {
    Category category = categories.save(new Category(name + " Category", "desc"));
    Product product = new Product();
    product.setName(name);
    product.setDescription("desc");
    product.setPrice(new BigDecimal("100.00"));
    product.setStockQuantity(10);
    product.setMaterial("PLA");
    product.setColor("Black");
    product.setCategory(category);
    return products.save(product);
  }

  /** What an admin's edit page loads. */
  private JsonNode open(Product product) throws Exception {
    return json.readTree(mvc.perform(get("/api/admin/products/" + product.getId()).header("Authorization", admin()))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  }

  /** Saves the form as loaded in {@code form}, with a new name and price. */
  private ResultActions save(Product product, JsonNode form, String name, String price) throws Exception {
    Map<String, Object> body = new HashMap<>(Map.of("name", name, "description", "desc", "price", price,
      "stockQuantity", form.get("stockQuantity").asInt(), "material", "PLA", "color", "Black", "imageUrl", "",
      "categoryId", product.getCategory().getId(), "active", true));
    body.put("expectedStockQuantity", form.get("stockQuantity").asInt());
    body.put("expectedEditVersion", form.get("editVersion").asLong());
    return mvc.perform(put("/api/products/" + product.getId()).header("Authorization", admin())
      .contentType("application/json").content(json.writeValueAsString(body)));
  }

  @Test void secondSaveFromAStaleFormIsRejected() throws Exception {
    Product product = newProduct("Conflict Stand");
    JsonNode alice = open(product);
    JsonNode bob = open(product);

    save(product, alice, "Alice's Name", "150.00").andExpect(status().isOk()).andExpect(jsonPath("$.editVersion").value(alice.get("editVersion").asLong() + 1));
    save(product, bob, "Bob's Name", "90.00")
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("changed by someone else")));

    Product stored = products.findById(product.getId()).orElseThrow();
    assertEquals("Alice's Name", stored.getName(), "the first save must survive");
    assertEquals(0, new BigDecimal("150.00").compareTo(stored.getPrice()));
  }

  @Test void reloadingAfterAConflictLetsTheSaveThrough() throws Exception {
    Product product = newProduct("Conflict Reload");
    JsonNode stale = open(product);
    save(product, open(product), "Changed Elsewhere", "120.00").andExpect(status().isOk());
    save(product, stale, "Mine", "130.00").andExpect(status().isConflict());

    save(product, open(product), "Mine", "130.00").andExpect(status().isOk());
    assertEquals("Mine", products.findById(product.getId()).orElseThrow().getName());
  }

  @Test void salesDoNotCountAsAnEditConflict() throws Exception {
    Product product = newProduct("Conflict Sale");
    JsonNode form = open(product);
    var order = Map.of("items", List.of(Map.of("productId", product.getId(), "quantity", 2)),
      "shippingName", "Jane", "shippingPhone", "0771234567", "shippingAddress", "1 Road", "shippingCity", "Colombo", "shippingPostalCode", "00300");
    mvc.perform(post("/api/orders").header("Authorization", "Bearer " + tokens.tokenFor("edit-conflict-customer@example.com", Role.CUSTOMER))
        .contentType("application/json").content(json.writeValueAsString(order))).andExpect(status().isOk());

    // Only the name changes; the sale moved stock but is not another admin's edit.
    save(product, form, "Renamed After Sale", "100.00").andExpect(status().isOk()).andExpect(jsonPath("$.stockQuantity").value(8));
  }

  @Test void archivingCountsAsAnEdit() throws Exception {
    Product product = newProduct("Conflict Archive");
    var order = Map.of("items", List.of(Map.of("productId", product.getId(), "quantity", 1)),
      "shippingName", "Jane", "shippingPhone", "0771234567", "shippingAddress", "1 Road", "shippingCity", "Colombo", "shippingPostalCode", "00300");
    mvc.perform(post("/api/orders").header("Authorization", "Bearer " + tokens.tokenFor("edit-conflict-customer@example.com", Role.CUSTOMER))
        .contentType("application/json").content(json.writeValueAsString(order))).andExpect(status().isOk());
    JsonNode form = open(product);
    mvc.perform(delete("/api/products/" + product.getId()).header("Authorization", admin())).andExpect(jsonPath("$.archived").value(true));

    // The open form says active=true; saving it would silently un-archive the product.
    save(product, form, "Conflict Archive", "100.00").andExpect(status().isConflict());
  }
}
