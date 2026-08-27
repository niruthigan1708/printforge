package com.printforge.dto;

import com.printforge.entity.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, String orderNumber, BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal totalAmount, String status, String shippingName, String shippingPhone, String shippingAddress, String shippingCity, String shippingPostalCode, Instant createdAt, Instant updatedAt, String customerName, String customerEmail, List<Item> items) {
  public record Item(String name, int quantity, BigDecimal unitPrice, BigDecimal subtotal) { }
  public static OrderResponse from(Order order) {
    return new OrderResponse(order.getId(), order.getOrderNumber(), order.getSubtotal(), order.getDeliveryFee(), order.getTotalAmount(), order.getStatus().name(), order.getShippingName(), order.getShippingPhone(), order.getShippingAddress(), order.getShippingCity(), order.getShippingPostalCode(), order.getCreatedAt(), order.getUpdatedAt(), order.getCustomer().getName(), order.getCustomer().getEmail(), order.getItems().stream().map(item -> new Item(item.getProductName(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal())).toList());
  }
}
