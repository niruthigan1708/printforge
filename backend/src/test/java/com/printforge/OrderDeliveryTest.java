package com.printforge;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.printforge.entity.Category;
import com.printforge.entity.Order;
import com.printforge.entity.Product;
import com.printforge.entity.Role;
import com.printforge.repository.CategoryRepository;
import com.printforge.repository.OrderRepository;
import com.printforge.repository.ProductRepository;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * An order cannot be "delivered" without first being shipped with a tracking number; every change
 * is recorded; and the customer can confirm receipt or report that the order never arrived.
 */
@SpringBootTest @AutoConfigureMockMvc
class OrderDeliveryTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired OrderRepository orders;
  @Autowired TestTokens tokens;

  private String admin() { return "Bearer " + tokens.tokenFor("delivery-admin@example.com", Role.ADMIN); }
  private String customer() { return "Bearer " + tokens.tokenFor("delivery-customer@example.com", Role.CUSTOMER); }
  private String otherCustomer() { return "Bearer " + tokens.tokenFor("delivery-other@example.com", Role.CUSTOMER); }

  private long placeOrder() throws Exception {
    Category category = categories.save(new Category("Delivery Category " + System.nanoTime(), "desc"));
    Product product = new Product();
    product.setName("Delivery Stand");
    product.setDescription("desc");
    product.setPrice(new BigDecimal("100.00"));
    product.setStockQuantity(10);
    product.setMaterial("PLA");
    product.setColor("Black");
    product.setCategory(category);
    product = products.save(product);
    var body = Map.of("items", List.of(Map.of("productId", product.getId(), "quantity", 1)),
      "shippingName", "Jane", "shippingPhone", "0771234567", "shippingAddress", "1 Road", "shippingCity", "Colombo", "shippingPostalCode", "00300");
    String response = mvc.perform(post("/api/orders").header("Authorization", customer()).contentType("application/json").content(json.writeValueAsString(body)))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return json.readTree(response).get("id").asLong();
  }

  private ResultActions setStatus(long order, String status, String trackingNumber) throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("status", status);
    if (trackingNumber != null) body.put("trackingNumber", trackingNumber);
    return mvc.perform(put("/api/admin/orders/" + order + "/status").header("Authorization", admin()).contentType("application/json").content(json.writeValueAsString(body)));
  }

  private long deliveredOrder() throws Exception {
    long order = placeOrder();
    setStatus(order, "SHIPPED", "LK123456789").andExpect(status().isOk());
    setStatus(order, "DELIVERED", null).andExpect(status().isOk());
    return order;
  }

  private ResultActions customerPut(long order, String action, String body) throws Exception {
    var request = put("/api/orders/" + order + "/" + action).header("Authorization", customer());
    if (body != null) request = request.contentType("application/json").content(body);
    return mvc.perform(request);
  }

  private JsonNode customerView(long order) throws Exception {
    return json.readTree(mvc.perform(get("/api/orders/" + order).header("Authorization", customer()))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  }

  /** Moves the delivery time into the past, as if the order had been delivered that long ago. */
  private void deliveredAgo(long orderId, Duration ago) {
    Order order = orders.findById(orderId).orElseThrow();
    order.setDeliveredAt(Instant.now().minus(ago));
    orders.save(order);
  }

  // ---- 1. no "delivered" without a shipment -----------------------------------------------------

  @Test void anUnshippedOrderCannotBeMarkedDelivered() throws Exception {
    long order = placeOrder();
    setStatus(order, "DELIVERED", null).andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("must be shipped with a tracking number first")));
    setStatus(order, "READY", null).andExpect(status().isOk())
      .andExpect(jsonPath("$.nextStatuses", not(hasItem("DELIVERED"))));
    setStatus(order, "DELIVERED", null).andExpect(status().isConflict());
  }

  @Test void shippingNeedsATrackingNumber() throws Exception {
    long order = placeOrder();
    setStatus(order, "SHIPPED", null).andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message", containsString("tracking number is required")));
    setStatus(order, "SHIPPED", "   ").andExpect(status().isBadRequest());
    setStatus(order, "SHIPPED", "x".repeat(101)).andExpect(status().isBadRequest());

    setStatus(order, "SHIPPED", " LK123456789 ").andExpect(status().isOk())
      .andExpect(jsonPath("$.trackingNumber").value("LK123456789"))
      .andExpect(jsonPath("$.nextStatuses", contains("DELIVERED")));
    assertEquals("LK123456789", customerView(order).get("trackingNumber").asString());
  }

  @Test void deliveryStartsTheReceiptWindow() throws Exception {
    long order = deliveredOrder();
    JsonNode view = customerView(order);
    assertEquals("AWAITING", view.get("receipt").asString());
    assertTrue(view.get("canConfirmReceipt").asBoolean());
    assertTrue(view.get("canReportIssue").asBoolean());
    Instant deliveredAt = Instant.parse(view.get("deliveredAt").asString());
    assertEquals(deliveredAt.plus(Duration.ofDays(14)), Instant.parse(view.get("receiptDeadline").asString()));
  }

  // ---- 2. history -------------------------------------------------------------------------------

  @Test void everyChangeIsRecordedWithWhoMadeIt() throws Exception {
    long order = placeOrder();
    mvc.perform(put("/api/admin/orders/" + order + "/status").header("Authorization", admin()).contentType("application/json")
      .content("{\"status\":\"PRINTING\",\"note\":\"Starting on printer 2\"}")).andExpect(status().isOk());
    setStatus(order, "SHIPPED", "LK555").andExpect(status().isOk());

    JsonNode adminView = json.readTree(mvc.perform(get("/api/admin/orders/" + order).header("Authorization", admin()))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    JsonNode history = adminView.get("history");
    assertEquals(3, history.size());
    assertEquals("PLACED", history.get(0).get("type").asString());
    assertEquals("delivery-customer@example.com", history.get(0).get("actorEmail").asString());
    assertEquals("PRINTING", history.get(1).get("status").asString());
    assertEquals("Starting on printer 2", history.get(1).get("note").asString());
    assertEquals("delivery-admin@example.com", history.get(1).get("actorEmail").asString());
    assertEquals("SHIPPED", history.get(2).get("status").asString());
    assertEquals("Tracking number LK555", history.get(2).get("note").asString());

    // Customers see the same timeline, but staff are shown as the studio, not by name or email.
    JsonNode customerHistory = customerView(order).get("history");
    assertEquals(3, customerHistory.size());
    assertEquals("PrintForge studio", customerHistory.get(1).get("actorName").asString());
    assertTrue(customerHistory.get(1).get("actorEmail").isNull());
    assertTrue(customerHistory.get(0).get("actorEmail").isNull());
  }

  @Test void customerCancellationIsRecorded() throws Exception {
    long order = placeOrder();
    customerPut(order, "cancel", null).andExpect(status().isOk());
    JsonNode last = customerView(order).get("history").get(1);
    assertEquals("CANCELLED", last.get("status").asString());
    assertEquals("CUSTOMER", last.get("actorRole").asString());
  }

  // ---- 3. reporting non-delivery ----------------------------------------------------------------

  @Test void customerCanReportADeliveredOrderAsNotReceived() throws Exception {
    long order = deliveredOrder();
    customerPut(order, "not-received", "{\"note\":\"Nothing came, neighbours have not seen it\"}").andExpect(status().isOk())
      .andExpect(jsonPath("$.issue.open").value(true))
      .andExpect(jsonPath("$.issue.note").value("Nothing came, neighbours have not seen it"))
      .andExpect(jsonPath("$.canReportIssue").value(false));

    // A second report while one is open is refused.
    customerPut(order, "not-received", null).andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("already open")));

    long before = dashboardIssues();
    mvc.perform(put("/api/admin/orders/" + order + "/issue/resolve").header("Authorization", admin()).contentType("application/json")
      .content("{\"resolution\":\"Courier located it; re-delivered on Friday\"}")).andExpect(status().isOk())
      .andExpect(jsonPath("$.issue.open").value(false))
      .andExpect(jsonPath("$.issue.resolution").value("Courier located it; re-delivered on Friday"));
    assertEquals(before - 1, dashboardIssues());

    JsonNode view = customerView(order);
    assertEquals("Courier located it; re-delivered on Friday", view.get("issue").get("resolution").asString());
    List<String> types = view.get("history").valueStream().map(event -> event.get("type").asString()).toList();
    assertEquals(List.of("PLACED", "STATUS_CHANGED", "STATUS_CHANGED", "ISSUE_REPORTED", "ISSUE_RESOLVED"), types);
  }

  @Test void openIssuesAreCountedOnTheDashboard() throws Exception {
    long before = dashboardIssues();
    long order = deliveredOrder();
    customerPut(order, "not-received", null).andExpect(status().isOk());
    assertEquals(before + 1, dashboardIssues());
  }

  @Test void problemsCannotBeReportedBeforeDeliveryOrAfterTheWindow() throws Exception {
    long shipped = placeOrder();
    setStatus(shipped, "SHIPPED", "LK1").andExpect(status().isOk());
    customerPut(shipped, "not-received", null).andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("not been marked as delivered")));

    long late = deliveredOrder();
    deliveredAgo(late, Duration.ofDays(15));
    customerPut(late, "not-received", null).andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("14-day window")));
  }

  @Test void resolvingNeedsAnOpenIssueAndAnExplanation() throws Exception {
    long order = deliveredOrder();
    String path = "/api/admin/orders/" + order + "/issue/resolve";
    mvc.perform(put(path).header("Authorization", admin()).contentType("application/json").content("{\"resolution\":\"done\"}"))
      .andExpect(status().isConflict());
    customerPut(order, "not-received", null).andExpect(status().isOk());
    mvc.perform(put(path).header("Authorization", admin()).contentType("application/json").content("{\"resolution\":\"  \"}"))
      .andExpect(status().isBadRequest());
    mvc.perform(put(path).header("Authorization", customer()).contentType("application/json").content("{\"resolution\":\"done\"}"))
      .andExpect(status().isForbidden());
  }

  @Test void customersCannotActOnOtherCustomersOrders() throws Exception {
    long order = deliveredOrder();
    mvc.perform(put("/api/orders/" + order + "/received").header("Authorization", otherCustomer())).andExpect(status().isNotFound());
    mvc.perform(put("/api/orders/" + order + "/not-received").header("Authorization", otherCustomer())).andExpect(status().isNotFound());
  }

  // ---- 4. confirming receipt --------------------------------------------------------------------

  @Test void customerCanConfirmReceiptOnce() throws Exception {
    long notYet = placeOrder();
    customerPut(notYet, "received", null).andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("not been delivered yet")));

    long order = deliveredOrder();
    customerPut(order, "received", null).andExpect(status().isOk())
      .andExpect(jsonPath("$.receipt").value("CONFIRMED"))
      .andExpect(jsonPath("$.receivedAt").isNotEmpty())
      .andExpect(jsonPath("$.canConfirmReceipt").value(false))
      .andExpect(jsonPath("$.canReportIssue").value(false));
    // Repeating is harmless and records nothing new.
    customerPut(order, "received", null).andExpect(status().isOk());
    assertEquals(4, customerView(order).get("history").size());

    customerPut(order, "not-received", null).andExpect(status().isConflict())
      .andExpect(jsonPath("$.message", containsString("already confirmed")));
  }

  @Test void confirmingReceiptClosesAnOpenReport() throws Exception {
    long before = dashboardIssues();
    long order = deliveredOrder();
    customerPut(order, "not-received", null).andExpect(status().isOk());
    customerPut(order, "received", null).andExpect(status().isOk())
      .andExpect(jsonPath("$.issue.open").value(false))
      .andExpect(jsonPath("$.issue.resolution").value("The customer confirmed the order arrived."));
    assertEquals(before, dashboardIssues());
  }

  @Test void anUnconfirmedOrderCountsAsReceivedAfterTheWindow() throws Exception {
    long order = deliveredOrder();
    deliveredAgo(order, Duration.ofDays(15));
    JsonNode view = customerView(order);
    assertEquals("AUTO", view.get("receipt").asString());
    assertFalse(view.get("canConfirmReceipt").asBoolean());
    assertFalse(view.get("canReportIssue").asBoolean());
  }

  @Test void anOpenReportStopsTheOrderCountingAsReceived() throws Exception {
    long order = deliveredOrder();
    customerPut(order, "not-received", null).andExpect(status().isOk());
    deliveredAgo(order, Duration.ofDays(30));
    JsonNode view = customerView(order);
    assertEquals("AWAITING", view.get("receipt").asString());
    assertTrue(view.get("canConfirmReceipt").asBoolean(), "the customer can still say it turned up");
  }

  private long dashboardIssues() throws Exception {
    return json.readTree(mvc.perform(get("/api/admin/dashboard").header("Authorization", admin()))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("deliveryIssues").asLong();
  }
}
