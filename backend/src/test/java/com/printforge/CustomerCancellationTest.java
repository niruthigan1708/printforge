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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Customers can cancel their own orders and print requests, but only early enough. */
@SpringBootTest @AutoConfigureMockMvc
class CustomerCancellationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired TestTokens tokens;

  private String customer() { return "Bearer " + tokens.tokenFor("cancel-customer@example.com", Role.CUSTOMER); }
  private String otherCustomer() { return "Bearer " + tokens.tokenFor("cancel-other@example.com", Role.CUSTOMER); }
  private String admin() { return "Bearer " + tokens.tokenFor("cancel-admin@example.com", Role.ADMIN); }

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

  private long placeOrder(Product product, int quantity) throws Exception {
    var body = Map.of("items", List.of(Map.of("productId", product.getId(), "quantity", quantity)),
      "shippingName", "Jane", "shippingPhone", "0771234567", "shippingAddress", "1 Road", "shippingCity", "Colombo", "shippingPostalCode", "00300");
    String response = mvc.perform(post("/api/orders").header("Authorization", customer()).contentType("application/json").content(json.writeValueAsString(body)))
      .andExpect(status().isOk()).andExpect(jsonPath("$.canCancel").value(true)).andReturn().getResponse().getContentAsString();
    return json.readTree(response).get("id").asLong();
  }

  private long submitRequest() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "model.stl", "model/stl", "solid part".getBytes());
    String response = mvc.perform(multipart("/api/custom-prints").file(file).param("material", "PLA").param("color", "Black").param("quantity", "1").header("Authorization", customer()))
      .andExpect(status().isOk()).andExpect(jsonPath("$.canCancel").value(true)).andReturn().getResponse().getContentAsString();
    return json.readTree(response).get("id").asLong();
  }

  private ResultActions cancelOrder(long id, String token) throws Exception {
    return mvc.perform(put("/api/orders/" + id + "/cancel").header("Authorization", token));
  }

  private ResultActions cancelRequest(long id, String token) throws Exception {
    return mvc.perform(put("/api/custom-prints/" + id + "/cancel").header("Authorization", token));
  }

  @Test void pendingOrderCanBeCancelledAndItsStockReturns() throws Exception {
    Product product = newProduct("Cancel Pending", 5);
    long order = placeOrder(product, 2);
    assertEquals(3, products.findById(product.getId()).orElseThrow().getStockQuantity());

    cancelOrder(order, customer()).andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("CANCELLED"))
      .andExpect(jsonPath("$.canCancel").value(false));
    assertEquals(5, products.findById(product.getId()).orElseThrow().getStockQuantity());

    // Cancelling again is a no-op and must not restock twice.
    cancelOrder(order, customer()).andExpect(status().isOk());
    assertEquals(5, products.findById(product.getId()).orElseThrow().getStockQuantity());
  }

  @Test void confirmedOrderCannotBeCancelledByTheCustomer() throws Exception {
    long order = placeOrder(newProduct("Cancel Confirmed", 5), 1);
    mvc.perform(put("/api/admin/orders/" + order + "/status").header("Authorization", admin()).contentType("application/json").content("{\"status\":\"CONFIRMED\"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.canCancel").value(false));

    cancelOrder(order, customer()).andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("can no longer be cancelled")));
  }

  @Test void customersCannotCancelSomeoneElsesOrder() throws Exception {
    long order = placeOrder(newProduct("Cancel Other", 5), 1);
    cancelOrder(order, otherCustomer()).andExpect(status().isNotFound());
  }

  @Test void requestCanBeCancelledBeforeItIsQuoted() throws Exception {
    long request = submitRequest();
    mvc.perform(put("/api/admin/custom-prints/" + request + "/status").header("Authorization", admin()).contentType("application/json").content("{\"status\":\"UNDER_REVIEW\"}"))
      .andExpect(status().isOk());

    cancelRequest(request, customer()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test void quotedRequestPointsTheCustomerToDecline() throws Exception {
    long request = submitRequest();
    mvc.perform(put("/api/admin/custom-prints/" + request + "/quote").header("Authorization", admin()).contentType("application/json").content("{\"amount\":\"900.00\"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.canCancel").value(false));

    cancelRequest(request, customer()).andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("decline the quote")));
  }

  @Test void customersCannotCancelSomeoneElsesRequest() throws Exception {
    long request = submitRequest();
    cancelRequest(request, otherCustomer()).andExpect(status().isNotFound());
  }
}
