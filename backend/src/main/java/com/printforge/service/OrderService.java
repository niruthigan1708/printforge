package com.printforge.service;

import com.printforge.dto.*;
import com.printforge.entity.*;
import com.printforge.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;

@Service @RequiredArgsConstructor
public class OrderService {
  private final OrderRepository orders; private final UserRepository users; private final ProductRepository products;
  @Transactional public OrderResponse create(String email, CreateOrderRequest request) {
    User customer = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    Order order = new Order(); order.setCustomer(customer); order.setOrderNumber("PF-" + (1001 + orders.count()));
    BigDecimal subtotal = BigDecimal.ZERO;
    for (CreateOrderRequest.Item requested : request.items()) {
      Product product = products.findById(requested.productId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
      if (!product.isActive() || product.getStockQuantity() < requested.quantity()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for " + product.getName());
      product.setStockQuantity(product.getStockQuantity() - requested.quantity());
      OrderItem item = new OrderItem(); item.setOrder(order); item.setProduct(product); item.setProductName(product.getName()); item.setQuantity(requested.quantity()); item.setUnitPrice(product.getPrice()); item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(requested.quantity()))); order.getItems().add(item); subtotal = subtotal.add(item.getSubtotal());
    }
    order.setSubtotal(subtotal); order.setDeliveryFee(BigDecimal.valueOf(350)); order.setTotalAmount(subtotal.add(order.getDeliveryFee())); order.setShippingName(request.shippingName()); order.setShippingPhone(request.shippingPhone()); order.setShippingAddress(request.shippingAddress()); order.setShippingCity(request.shippingCity()); order.setShippingPostalCode(request.shippingPostalCode());
    return OrderResponse.from(orders.save(order));
  }
  public List<OrderResponse> mine(String email) { User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")); return orders.findByCustomerOrderByCreatedAtDesc(user).stream().map(OrderResponse::from).toList(); }
  public OrderResponse getMine(String email, Long id) { User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")); return OrderResponse.from(orders.findByIdAndCustomer(id, user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"))); }
}
