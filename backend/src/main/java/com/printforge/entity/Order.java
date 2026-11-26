package com.printforge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity @Getter @Setter @NoArgsConstructor
@Table(name = "customer_orders")
public class Order {
  /**
   * How long after delivery a customer can report that the order never arrived. After that, a
   * delivered order the customer has not confirmed counts as received.
   */
  public static final Duration RECEIPT_WINDOW = Duration.ofDays(14);

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(unique = true, nullable = false) private String orderNumber;
  @ManyToOne(optional = false) private User customer;
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true) private List<OrderItem> items = new ArrayList<>();
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("createdAt ASC, id ASC") private List<OrderEvent> events = new ArrayList<>();
  private BigDecimal subtotal; private BigDecimal deliveryFee; private BigDecimal totalAmount;
  @Enumerated(EnumType.STRING) private OrderStatus status = OrderStatus.PENDING;
  private String shippingName; private String shippingPhone; private String shippingAddress; private String shippingCity; private String shippingPostalCode;
  @Column(length = 100) private String trackingNumber;
  private Instant deliveredAt;
  private Instant receivedAt;
  private Instant issueReportedAt;
  @Column(length = 2000) private String issueNote;
  private Instant issueResolvedAt;
  @Column(length = 2000) private String issueResolution;
  private Instant createdAt = Instant.now(); private Instant updatedAt = Instant.now();

  public void record(OrderEvent.Type type, User actor, String note) { events.add(OrderEvent.of(this, type, actor, note)); }

  public boolean hasOpenIssue() { return issueReportedAt != null && issueResolvedAt == null; }

  /** End of the window for reporting a problem; null until delivered. */
  public Instant receiptDeadline() { return deliveredAt == null ? null : deliveredAt.plus(RECEIPT_WINDOW); }

  /** Delivered, not confirmed, no open problem, and the reporting window has passed. */
  public boolean isAutoReceived(Instant now) {
    return status == OrderStatus.DELIVERED && receivedAt == null && !hasOpenIssue() && deliveredAt != null && now.isAfter(receiptDeadline());
  }

  /** The customer may confirm receipt of a delivered order once, unless it already counts as received. */
  public boolean customerCanConfirmReceipt(Instant now) {
    return status == OrderStatus.DELIVERED && receivedAt == null && !isAutoReceived(now);
  }

  /** The customer may report non-delivery within the window, if not confirmed and nothing is open. */
  public boolean customerCanReportIssue(Instant now) {
    return status == OrderStatus.DELIVERED && receivedAt == null && !hasOpenIssue() && deliveredAt != null && !now.isAfter(receiptDeadline());
  }
}
