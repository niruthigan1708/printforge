package com.printforge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Getter @Setter @NoArgsConstructor
public class OrderItem {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(optional = false) private Order order;
  @ManyToOne(optional = false) private Product product;
  private String productName; private int quantity; private BigDecimal unitPrice; private BigDecimal subtotal;
}
