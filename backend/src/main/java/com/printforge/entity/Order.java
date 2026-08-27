package com.printforge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity @Getter @Setter @NoArgsConstructor
@Table(name = "customer_orders")
public class Order {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(unique = true, nullable = false) private String orderNumber;
  @ManyToOne(optional = false) private User customer;
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true) private List<OrderItem> items = new ArrayList<>();
  private BigDecimal subtotal; private BigDecimal deliveryFee; private BigDecimal totalAmount;
  @Enumerated(EnumType.STRING) private OrderStatus status = OrderStatus.PENDING;
  private String shippingName; private String shippingPhone; private String shippingAddress; private String shippingCity; private String shippingPostalCode;
  private Instant createdAt = Instant.now(); private Instant updatedAt = Instant.now();
}
