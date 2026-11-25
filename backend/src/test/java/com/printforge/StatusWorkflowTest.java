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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Order and custom print requests may only move through their defined workflows. */
@SpringBootTest @AutoConfigureMockMvc
class StatusWorkflowTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired TestTokens tokens;

  // ---- helpers -------------------------------------------------------------------------------

  private String admin() { return "Bearer " + tokens.tokenFor("workflow-admin@example.com", Role.ADMIN); }
  private String customer() { return "Bearer " + tokens.tokenFor("workflow-customer@example.com", Role.CUSTOMER); }

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

  private long placeOrder(Long productId, int quantity) throws Exception {
    var body = Map.of("items", List.of(Map.of("productId", productId, "quantity", quantity)),
      "shippingName", "Jane", "shippingPhone", "0771234567", "shippingAddress", "1 Road", "shippingCity", "Colombo", "shippingPostalCode", "00300");
    String response = mvc.perform(post("/api/orders").header("Authorization", customer()).contentType("application/json").content(json.writeValueAsString(body)))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return json.readTree(response).get("id").asLong();
  }

  private ResultActions setOrderStatus(long orderId, String status) throws Exception {
    return mvc.perform(put("/api/admin/orders/" + orderId + "/status").header("Authorization", admin()).contentType("application/json").content("{\"status\":\"" + status + "\"}"));
  }

  private long submitRequest() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "model.stl", "model/stl", "solid".getBytes());
    String response = mvc.perform(multipart("/api/custom-prints").file(file).param("material", "PLA").param("color", "Black").param("quantity", "1").header("Authorization", customer()))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return json.readTree(response).get("id").asLong();
  }

  private ResultActions setRequestStatus(long requestId, String status) throws Exception {
    return mvc.perform(put("/api/admin/custom-prints/" + requestId + "/status").header("Authorization", admin()).contentType("application/json").content("{\"status\":\"" + status + "\"}"));
  }

  private ResultActions quote(long requestId) throws Exception {
    return mvc.perform(put("/api/admin/custom-prints/" + requestId + "/quote").header("Authorization", admin()).contentType("application/json").content("{\"amount\":\"2500.00\",\"notes\":\"ok\"}"));
  }

  private int stockOf(Product product) { return products.findById(product.getId()).orElseThrow().getStockQuantity(); }

  // ---- orders --------------------------------------------------------------------------------

  @Test void ordersMoveForwardAndMaySkipSteps() throws Exception {
    long order = placeOrder(newProduct("Forward Stand", 5).getId(), 1);

    setOrderStatus(order, "PRINTING").andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("PRINTING"))
      .andExpect(jsonPath("$.nextStatuses", contains("READY", "SHIPPED", "DELIVERED", "CANCELLED")));
  }

  @Test void ordersCannotMoveBackwards() throws Exception {
    long order = placeOrder(newProduct("Backwards Stand", 5).getId(), 1);
    setOrderStatus(order, "SHIPPED").andExpect(status().isOk());

    setOrderStatus(order, "PENDING").andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("only move forward")));
  }

  @Test void shippedOrdersCannotBeCancelled() throws Exception {
    long order = placeOrder(newProduct("Shipped Stand", 5).getId(), 1);
    setOrderStatus(order, "SHIPPED").andExpect(status().isOk())
      .andExpect(jsonPath("$.nextStatuses", contains("DELIVERED")));

    setOrderStatus(order, "CANCELLED").andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("cannot be cancelled once it has shipped")));
  }

  @Test void cancellingAnOrderReturnsItsStockExactlyOnce() throws Exception {
    Product product = newProduct("Restock Stand", 5);
    long order = placeOrder(product.getId(), 3);
    assertEquals(2, stockOf(product));

    setOrderStatus(order, "CANCELLED").andExpect(status().isOk()).andExpect(jsonPath("$.nextStatuses", empty()));
    assertEquals(5, stockOf(product));

    // Repeating the same status is a no-op and must not restock again.
    setOrderStatus(order, "CANCELLED").andExpect(status().isOk());
    assertEquals(5, stockOf(product));
    setOrderStatus(order, "CONFIRMED").andExpect(status().isConflict());
  }

  // ---- custom print requests -----------------------------------------------------------------

  @Test void fullCustomPrintWorkflowSucceeds() throws Exception {
    long request = submitRequest();
    setRequestStatus(request, "UNDER_REVIEW").andExpect(status().isOk()).andExpect(jsonPath("$.canQuote").value(true));
    quote(request).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("QUOTED"));
    mvc.perform(put("/api/custom-prints/" + request + "/accept").header("Authorization", customer())).andExpect(status().isOk());
    setRequestStatus(request, "IN_PRODUCTION").andExpect(status().isOk());
    setRequestStatus(request, "COMPLETED").andExpect(status().isOk()).andExpect(jsonPath("$.nextStatuses", empty()));
  }

  @Test void adminCannotAnswerAQuoteForTheCustomer() throws Exception {
    long request = submitRequest();
    quote(request).andExpect(status().isOk());

    setRequestStatus(request, "ACCEPTED").andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("Only the customer")));
    setRequestStatus(request, "REJECTED").andExpect(status().isConflict());
  }

  @Test void quotedStatusRequiresTheQuoteAction() throws Exception {
    long request = submitRequest();
    setRequestStatus(request, "QUOTED").andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("quote action")));
  }

  @Test void productionCannotStartBeforeTheCustomerAccepts() throws Exception {
    long request = submitRequest();
    setRequestStatus(request, "IN_PRODUCTION").andExpect(status().isConflict());
    quote(request).andExpect(status().isOk());
    setRequestStatus(request, "IN_PRODUCTION").andExpect(status().isConflict());
  }

  @Test void answeredQuotesCannotBeReQuoted() throws Exception {
    long accepted = submitRequest();
    quote(accepted).andExpect(status().isOk());
    mvc.perform(put("/api/custom-prints/" + accepted + "/accept").header("Authorization", customer())).andExpect(status().isOk());
    quote(accepted).andExpect(status().isConflict());

    long rejected = submitRequest();
    quote(rejected).andExpect(status().isOk());
    mvc.perform(put("/api/custom-prints/" + rejected + "/reject").header("Authorization", customer())).andExpect(status().isOk());
    quote(rejected).andExpect(status().isConflict());
    setRequestStatus(rejected, "CANCELLED").andExpect(status().isConflict());
  }

  @Test void quotesCanBeRevisedBeforeTheCustomerAnswers() throws Exception {
    long request = submitRequest();
    quote(request).andExpect(status().isOk());
    quote(request).andExpect(status().isOk()).andExpect(jsonPath("$.canQuote").value(true));
  }
}
