package com.printforge.dto;

import com.printforge.entity.Order;
import com.printforge.entity.OrderEvent;
import com.printforge.entity.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * An order as the API returns it.
 *
 * @param receipt          for DELIVERED orders: CONFIRMED (the customer said so), AUTO (the reporting
 *                         window passed without a problem) or AWAITING; null otherwise
 * @param receiptDeadline  last moment the customer can report non-delivery; null until delivered
 * @param history          every recorded change, oldest first
 */
public record OrderResponse(Long id, String orderNumber, BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal totalAmount, String status,
  String shippingName, String shippingPhone, String shippingAddress, String shippingCity, String shippingPostalCode,
  Instant createdAt, Instant updatedAt, String customerName, String customerEmail, List<Item> items, List<String> nextStatuses, boolean canCancel,
  String trackingNumber, Instant deliveredAt, Instant receivedAt, String receipt, Instant receiptDeadline, boolean canConfirmReceipt, boolean canReportIssue,
  Issue issue, List<Event> history) {

  public record Item(String name, int quantity, BigDecimal unitPrice, BigDecimal subtotal) { }

  /** A non-delivery report. {@code open} until the studio resolves it (or the customer confirms receipt). */
  public record Issue(Instant reportedAt, String note, Instant resolvedAt, String resolution, boolean open) { }

  /** One history entry. Customers see studio staff as "PrintForge studio", never their names. */
  public record Event(String type, String status, String actorName, String actorEmail, String actorRole, String note, Instant createdAt) { }

  public static final String STUDIO = "PrintForge studio";

  /** The customer's view of their own order. */
  public static OrderResponse from(Order order) { return build(order, false); }

  /** The admin view, which names the staff member behind each change. */
  public static OrderResponse forAdmin(Order order) { return build(order, true); }

  private static OrderResponse build(Order order, boolean admin) {
    Instant now = Instant.now();
    String receipt = null;
    if (order.getStatus() == com.printforge.entity.OrderStatus.DELIVERED) {
      receipt = order.getReceivedAt() != null ? "CONFIRMED" : order.isAutoReceived(now) ? "AUTO" : "AWAITING";
    }
    Issue issue = order.getIssueReportedAt() == null ? null
      : new Issue(order.getIssueReportedAt(), order.getIssueNote(), order.getIssueResolvedAt(), order.getIssueResolution(), order.hasOpenIssue());
    return new OrderResponse(order.getId(), order.getOrderNumber(), order.getSubtotal(), order.getDeliveryFee(), order.getTotalAmount(), order.getStatus().name(),
      order.getShippingName(), order.getShippingPhone(), order.getShippingAddress(), order.getShippingCity(), order.getShippingPostalCode(),
      order.getCreatedAt(), order.getUpdatedAt(), order.getCustomer().getName(), order.getCustomer().getEmail(),
      order.getItems().stream().map(item -> new Item(item.getProductName(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal())).toList(),
      order.getStatus().nextStatuses().stream().map(Enum::name).toList(), order.getStatus().customerCanCancel(),
      order.getTrackingNumber(), order.getDeliveredAt(), order.getReceivedAt(), receipt, order.receiptDeadline(),
      order.customerCanConfirmReceipt(now), order.customerCanReportIssue(now), issue,
      order.getEvents().stream().map(event -> event(event, admin)).toList());
  }

  private static Event event(OrderEvent event, boolean admin) {
    String role = event.getActorRole() == null ? null : event.getActorRole().name();
    String name = event.getActorName();
    String email = event.getActorEmail();
    if (!admin && event.getActorRole() == Role.ADMIN) { name = STUDIO; email = null; }
    if (!admin && event.getActorRole() == Role.CUSTOMER) email = null;
    return new Event(event.getEventType().name(), event.getStatus() == null ? null : event.getStatus().name(), name, email, role, event.getNote(), event.getCreatedAt());
  }
}
