package com.printforge;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class OrderWorkflowTest {
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
    product.setPrice(new BigDecimal("1000.00"));
    product.setStockQuantity(stock);
    product.setMaterial("PLA");
    product.setColor("Black");
    product.setImageUrl("");
    product.setCategory(category);
    return products.save(product);
  }

  private Map<String, Object> orderBody(Long productId, int quantity) {
    return Map.of("items", List.of(Map.of("productId", productId, "quantity", quantity)),
      "shippingName", "Jane Doe", "shippingPhone", "0771234567", "shippingAddress", "12 Lake Road", "shippingCity", "Colombo", "shippingPostalCode", "00300");
  }

  @Test void placingAnOrderReducesStock() throws Exception {
    Product product = newProduct("Stock Test Stand", 5);
    String token = tokens.tokenFor("order-customer@example.com", Role.CUSTOMER);

    mvc.perform(post("/api/orders").header("Authorization", "Bearer " + token).contentType("application/json").content(json.writeValueAsString(orderBody(product.getId(), 3))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.orderNumber").value(org.hamcrest.Matchers.startsWith("PF-")));

    Product refreshed = products.findById(product.getId()).orElseThrow();
    org.junit.jupiter.api.Assertions.assertEquals(2, refreshed.getStockQuantity());
  }

  @Test void orderingMoreThanAvailableStockIsRejected() throws Exception {
    Product product = newProduct("Low Stock Stand", 1);
    String token = tokens.tokenFor("order-customer-2@example.com", Role.CUSTOMER);

    mvc.perform(post("/api/orders").header("Authorization", "Bearer " + token).contentType("application/json").content(json.writeValueAsString(orderBody(product.getId(), 5))))
      .andExpect(status().isConflict());
  }

  @Test void customersCannotViewOtherCustomersOrders() throws Exception {
    Product product = newProduct("Private Order Stand", 5);
    String ownerToken = tokens.tokenFor("order-owner@example.com", Role.CUSTOMER);
    String intruderToken = tokens.tokenFor("order-intruder@example.com", Role.CUSTOMER);

    String response = mvc.perform(post("/api/orders").header("Authorization", "Bearer " + ownerToken).contentType("application/json").content(json.writeValueAsString(orderBody(product.getId(), 1))))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    Long orderId = json.readTree(response).get("id").asLong();

    mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + intruderToken)).andExpect(status().isNotFound());
  }

  @Test void anonymousRequestsAreUnauthorized() throws Exception {
    mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
  }
}
