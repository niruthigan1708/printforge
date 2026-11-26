package com.printforge.service;

import com.printforge.config.ShopSettings;
import com.printforge.dto.*;
import com.printforge.entity.*;
import com.printforge.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class OrderService {
  private final OrderRepository orders; private final UserRepository users; private final ProductRepository products; private final ShopSettings shopSettings;

  @Transactional public OrderResponse create(String email, CreateOrderRequest request) {
    User customer = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    Order order = new Order(); order.setCustomer(customer);
    // Temporary unique value; replaced with the id-based number once the row has an id.
    order.setOrderNumber("PF-PENDING-" + UUID.randomUUID());
    BigDecimal subtotal = BigDecimal.ZERO;
    // Merge repeated lines for the same product, then lock products in ascending id order:
    // the row locks stop concurrent orders from overselling, and a fixed lock order stops
    // two multi-item orders from deadlocking on each other.
    Map<Long, Integer> quantities = new TreeMap<>();
    for (CreateOrderRequest.Item requested : request.items()) quantities.merge(requested.productId(), requested.quantity(), Integer::sum);
    for (Map.Entry<Long, Integer> line : quantities.entrySet()) {
      int quantity = line.getValue();
      Product product = products.findByIdForUpdate(line.getKey()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
      if (!product.isActive()) throw new ResponseStatusException(HttpStatus.CONFLICT, product.getName() + " is no longer available");
      if (product.getStockQuantity() < quantity) throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for " + product.getName());
      product.setStockQuantity(product.getStockQuantity() - quantity);
      OrderItem item = new OrderItem(); item.setOrder(order); item.setProduct(product); item.setProductName(product.getName()); item.setQuantity(quantity); item.setUnitPrice(product.getPrice()); item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity))); order.getItems().add(item); subtotal = subtotal.add(item.getSubtotal());
    }
    order.setSubtotal(subtotal); order.setDeliveryFee(shopSettings.deliveryFeeFor(subtotal)); order.setTotalAmount(subtotal.add(order.getDeliveryFee())); order.setShippingName(request.shippingName()); order.setShippingPhone(request.shippingPhone()); order.setShippingAddress(request.shippingAddress()); order.setShippingCity(request.shippingCity()); order.setShippingPostalCode(request.shippingPostalCode());
    // The cart shows prices saved when items were added. If a price or the delivery fee changed
    // since, refuse rather than charge a different amount than the customer agreed to. Throwing
    // rolls back the stock deducted above.
    if (request.expectedTotal() != null && request.expectedTotal().compareTo(order.getTotalAmount()) != 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Prices have changed since you loaded your cart. Your new total is "
        + order.getTotalAmount().setScale(2, java.math.RoundingMode.HALF_UP) + "; please review your cart and place the order again.");
    }
    order.record(OrderEvent.Type.PLACED, customer, null);
    order = orders.saveAndFlush(order);
    order.setOrderNumber(orderNumberFor(order.getId()));
    return OrderResponse.from(order);
  }

  /**
   * Order numbers derive from the database-generated id, so concurrent orders can never collide.
   * (The old scheme, 1001 + count(), handed two simultaneous orders the same number.) Because
   * count() never exceeded id - 1, numbers issued under the old scheme are always lower than
   * any new one, so existing orders cannot clash either.
   */
  static String orderNumberFor(long id) { return "PF-" + (1000 + id); }
  @Transactional(readOnly = true)
  public PageResponse<OrderResponse> mine(String email, Pageable pageable) { User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")); return PageResponse.from(orders.findByCustomer(user, pageable), OrderResponse::from); }

  @Transactional(readOnly = true)
  public OrderResponse getMine(String email, Long id) { User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")); return OrderResponse.from(orders.findByIdAndCustomer(id, user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"))); }

  @Transactional(readOnly = true)
  public PageResponse<OrderResponse> all(Pageable pageable) { return PageResponse.from(orders.findAll(pageable), OrderResponse::forAdmin); }

  @Transactional(readOnly = true)
  public OrderResponse get(Long id) { return OrderResponse.forAdmin(orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"))); }

  /** A customer cancelling their own order; only allowed while it is still PENDING. */
  @Transactional
  public OrderResponse cancelMine(String email, Long id) {
    // Same lock as the admin status change, so a cancel and a confirm cannot interleave.
    Order order = lockMine(email, id);
    if (order.getStatus() == OrderStatus.CANCELLED) return OrderResponse.from(order);
    if (!order.getStatus().customerCanCancel()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is already " + order.getStatus() + " and can no longer be cancelled online. Please contact us.");
    }
    restock(order);
    order.setStatus(OrderStatus.CANCELLED);
    order.setUpdatedAt(Instant.now());
    order.record(OrderEvent.Type.STATUS_CHANGED, order.getCustomer(), "Cancelled by the customer");
    return OrderResponse.from(order);
  }

  /**
   * The customer confirms a delivered order arrived. Also closes an open non-delivery report,
   * since the customer has now said it arrived.
   */
  @Transactional
  public OrderResponse confirmReceipt(String email, Long id) {
    Order order = lockMine(email, id);
    if (order.getReceivedAt() != null) return OrderResponse.from(order);
    Instant now = Instant.now();
    if (!order.customerCanConfirmReceipt(now)) {
      String reason = order.getStatus() != OrderStatus.DELIVERED ? "it has not been delivered yet" : "it already counts as received";
      throw new ResponseStatusException(HttpStatus.CONFLICT, "This order cannot be confirmed: " + reason);
    }
    order.setReceivedAt(now);
    if (order.hasOpenIssue()) {
      order.setIssueResolvedAt(now);
      order.setIssueResolution("The customer confirmed the order arrived.");
    }
    order.setUpdatedAt(now);
    order.record(OrderEvent.Type.RECEIVED, order.getCustomer(), null);
    return OrderResponse.from(order);
  }

  /** The customer reports that a DELIVERED order never arrived, within {@link Order#RECEIPT_WINDOW} of delivery. */
  @Transactional
  public OrderResponse reportIssue(String email, Long id, String note) {
    Order order = lockMine(email, id);
    Instant now = Instant.now();
    if (!order.customerCanReportIssue(now)) {
      String reason;
      if (order.getStatus() != OrderStatus.DELIVERED) reason = "it has not been marked as delivered";
      else if (order.getReceivedAt() != null) reason = "you have already confirmed you received it";
      else if (order.hasOpenIssue()) reason = "a report is already open";
      else reason = "the " + Order.RECEIPT_WINDOW.toDays() + "-day window after delivery has passed. Please contact us";
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A problem cannot be reported for this order: " + reason);
    }
    String cleaned = blankToNull(note);
    order.setIssueReportedAt(now);
    order.setIssueNote(cleaned);
    order.setIssueResolvedAt(null);
    order.setIssueResolution(null);
    order.setUpdatedAt(now);
    order.record(OrderEvent.Type.ISSUE_REPORTED, order.getCustomer(), cleaned);
    return OrderResponse.from(order);
  }

  /** The studio closes a non-delivery report, explaining what was done (shown to the customer). */
  @Transactional
  public OrderResponse resolveIssue(String adminEmail, Long id, String resolution) {
    Order order = orders.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    if (!order.hasOpenIssue()) throw new ResponseStatusException(HttpStatus.CONFLICT, "This order has no open delivery problem");
    Instant now = Instant.now();
    order.setIssueResolvedAt(now);
    order.setIssueResolution(resolution.strip());
    order.setUpdatedAt(now);
    order.record(OrderEvent.Type.ISSUE_RESOLVED, actor(adminEmail), resolution.strip());
    return OrderResponse.forAdmin(order);
  }

  @Transactional
  public OrderResponse updateStatus(String adminEmail, Long id, OrderStatusUpdateRequest request) {
    OrderStatus status = request.status();
    // Lock the order so two concurrent cancellations cannot both restock it.
    Order order = orders.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    OrderStatus current = order.getStatus();
    if (current == status) return OrderResponse.forAdmin(order);
    if (current.isTerminal()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change the status of a " + current + " order");
    if (!current.canTransitionTo(status)) {
      String reason;
      if (status == OrderStatus.CANCELLED) reason = "an order cannot be cancelled once it has shipped";
      else if (status == OrderStatus.DELIVERED && current.ordinal() < OrderStatus.SHIPPED.ordinal()) reason = "it must be shipped with a tracking number first";
      else reason = "orders can only move forward";
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot move an order from " + current + " to " + status + ": " + reason);
    }
    String tracking = blankToNull(request.trackingNumber());
    if (status == OrderStatus.SHIPPED) {
      if (tracking == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A tracking number is required to mark an order as shipped");
      order.setTrackingNumber(tracking);
    }
    if (status == OrderStatus.CANCELLED) restock(order);
    Instant now = Instant.now();
    order.setStatus(status);
    if (status == OrderStatus.DELIVERED) order.setDeliveredAt(now);
    order.setUpdatedAt(now);
    String note = blankToNull(request.note());
    if (status == OrderStatus.SHIPPED) note = note == null ? "Tracking number " + tracking : "Tracking number " + tracking + ". " + note;
    if (note != null && note.length() > com.printforge.dto.FieldLimits.LONG_TEXT) note = note.substring(0, com.printforge.dto.FieldLimits.LONG_TEXT);
    order.record(OrderEvent.Type.STATUS_CHANGED, actor(adminEmail), note);
    return OrderResponse.forAdmin(order);
  }

  private Order lockMine(String email, Long id) {
    return orders.findByIdForUpdate(id)
      .filter(candidate -> candidate.getCustomer().getEmail().equalsIgnoreCase(email))
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
  }

  private User actor(String email) {
    return users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
  }

  private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.strip(); }

  /** Returns a cancelled order's units to stock, locking products in the same id order as checkout. */
  private void restock(Order order) {
    Map<Long, Integer> quantities = new TreeMap<>();
    for (OrderItem item : order.getItems()) quantities.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
    for (Map.Entry<Long, Integer> line : quantities.entrySet()) {
      // Archived products are restocked too, so the count is right if they are reactivated.
      products.findByIdForUpdate(line.getKey()).ifPresent(product -> {
        product.setStockQuantity(product.getStockQuantity() + line.getValue());
        product.setUpdatedAt(Instant.now());
      });
    }
  }
}
