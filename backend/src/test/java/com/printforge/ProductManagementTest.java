package com.printforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printforge.entity.Category;
import com.printforge.entity.Role;
import com.printforge.repository.CategoryRepository;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
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

  @Test void nonAdminCannotCreateProduct() throws Exception {
    Category category = categories.save(new Category("Gaming", "Gaming stuff"));
    String customerToken = tokens.tokenFor("product-customer@example.com", Role.CUSTOMER);
    var body = Map.of("name", "Blocked Product", "description", "desc", "price", "999.00", "stockQuantity", 5, "material", "PLA", "color", "Red", "imageUrl", "", "categoryId", category.getId(), "active", true);

    mvc.perform(post("/api/products").header("Authorization", "Bearer " + customerToken).contentType("application/json").content(json.writeValueAsString(body)))
      .andExpect(status().isForbidden());
  }
}
