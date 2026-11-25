package com.printforge;

import tools.jackson.databind.ObjectMapper;
import com.printforge.entity.Category;
import com.printforge.entity.Role;
import com.printforge.repository.CategoryRepository;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class ProductManagementTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CategoryRepository categories;
  @Autowired TestTokens tokens;

  @Test void adminCanCreateAndAnyoneCanRetrieveTheProduct() throws Exception {
    Category category = categories.save(new Category("Desk Accessories", "Desk stuff"));
    String adminToken = tokens.tokenFor("product-admin@example.com", Role.ADMIN);
    var body = Map.of("name", "Test Stand", "description", "A stand", "price", "1999.00", "stockQuantity", 10, "material", "PLA", "color", "Black", "imageUrl", "", "categoryId", category.getId(), "active", true);

    String response = mvc.perform(post("/api/products").header("Authorization", "Bearer " + adminToken).contentType("application/json").content(json.writeValueAsString(body)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("Test Stand"))
      .andReturn().getResponse().getContentAsString();

    Long id = json.readTree(response).get("id").asLong();
    mvc.perform(get("/api/products/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.stockQuantity").value(10));
  }

  private Long createProduct(String adminToken, String name, String categoryName) throws Exception {
    Category category = categories.save(new Category(categoryName, "desc"));
    var body = Map.of("name", name, "description", "desc", "price", "500.00", "stockQuantity", 10, "material", "PLA", "color", "Black", "imageUrl", "", "categoryId", category.getId(), "active", true);
    String response = mvc.perform(post("/api/products").header("Authorization", "Bearer " + adminToken).contentType("application/json").content(json.writeValueAsString(body)))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return json.readTree(response).get("id").asLong();
  }

  @Test void deletingAnUnorderedProductRemovesIt() throws Exception {
    String adminToken = tokens.tokenFor("delete-admin@example.com", Role.ADMIN);
    Long id = createProduct(adminToken, "Never Ordered", "Delete Category A");

    mvc.perform(delete("/api/products/" + id).header("Authorization", "Bearer " + adminToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.archived").value(false));
    mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
  }

  @Test void deletingAnOrderedProductArchivesItInsteadOfFailing() throws Exception {
    String adminToken = tokens.tokenFor("delete-admin@example.com", Role.ADMIN);
    String customerToken = tokens.tokenFor("delete-customer@example.com", Role.CUSTOMER);
    Long id = createProduct(adminToken, "Already Ordered", "Delete Category B");
    var order = Map.of("items", List.of(Map.of("productId", id, "quantity", 1)),
      "shippingName", "Jane", "shippingPhone", "0771234567", "shippingAddress", "1 Road", "shippingCity", "Colombo", "shippingPostalCode", "00300");
    mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken).contentType("application/json").content(json.writeValueAsString(order)))
      .andExpect(status().isOk());

    mvc.perform(delete("/api/products/" + id).header("Authorization", "Bearer " + adminToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.archived").value(true));
    // Archived: hidden from the public catalogue and detail page, still visible to admins.
    mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
    mvc.perform(get("/api/products")).andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
    mvc.perform(get("/api/admin/products/" + id).header("Authorization", "Bearer " + adminToken))
      .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
  }

  @Test void inactiveProductsAreHiddenFromThePublicButNotFromAdmins() throws Exception {
    String adminToken = tokens.tokenFor("visibility-admin@example.com", Role.ADMIN);
    String customerToken = tokens.tokenFor("visibility-customer@example.com", Role.CUSTOMER);
    Category category = categories.save(new Category("Visibility Category", "desc"));
    var body = Map.of("name", "Hidden Stand", "description", "desc", "price", "500.00", "stockQuantity", 10, "material", "PLA", "color", "Black", "imageUrl", "", "categoryId", category.getId(), "active", false);
    String response = mvc.perform(post("/api/products").header("Authorization", "Bearer " + adminToken).contentType("application/json").content(json.writeValueAsString(body)))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    Long id = json.readTree(response).get("id").asLong();

    mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
    mvc.perform(get("/api/products/" + id).header("Authorization", "Bearer " + customerToken)).andExpect(status().isNotFound());
    mvc.perform(get("/api/admin/products/" + id).header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
    mvc.perform(get("/api/admin/products/" + id).header("Authorization", "Bearer " + customerToken)).andExpect(status().isForbidden());
    // Photos stay reachable so carts and past orders can still show them.
    mvc.perform(get("/api/products/" + id + "/image")).andExpect(status().isOk());
  }

  @Test void searchAndCategoryFiltersAreAppliedTogether() throws Exception {
    String adminToken = tokens.tokenFor("filter-admin@example.com", Role.ADMIN);
    createProduct(adminToken, "Zephyr Stand", "Zephyr Gaming");
    createProduct(adminToken, "Zephyr Stand Desk", "Zephyr Desk");

    mvc.perform(get("/api/products").param("search", "zephyr").param("category", "Zephyr Gaming"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].name").value("Zephyr Stand"));
    mvc.perform(get("/api/products").param("search", "zephyr"))
      .andExpect(jsonPath("$.length()").value(2));
  }

  @Test void nonAdminCannotCreateProduct() throws Exception {
    Category category = categories.save(new Category("Gaming", "Gaming stuff"));
    String customerToken = tokens.tokenFor("product-customer@example.com", Role.CUSTOMER);
    var body = Map.of("name", "Blocked Product", "description", "desc", "price", "999.00", "stockQuantity", 5, "material", "PLA", "color", "Red", "imageUrl", "", "categoryId", category.getId(), "active", true);

    mvc.perform(post("/api/products").header("Authorization", "Bearer " + customerToken).contentType("application/json").content(json.writeValueAsString(body)))
      .andExpect(status().isForbidden());
  }
}
