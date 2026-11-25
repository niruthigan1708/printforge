package com.printforge.service;

import com.printforge.config.ShopSettings;
import com.printforge.dto.*;
import com.printforge.entity.*;
import com.printforge.repository.*;
import lombok.RequiredArgsConstructor;
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
    order.setSubtotal(subtotal); order.setDeliveryFee(shopSettings.deliveryFee()); order.setTotalAmount(subtotal.add(order.getDeliveryFee())); order.setShippingName(request.shippingName()); order.setShippingPhone(request.shippingPhone()); order.setShippingAddress(request.shippingAddress()); order.setShippingCity(request.shippingCity()); order.setShippingPostalCode(request.shippingPostalCode());
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
  public List<OrderResponse> mine(String email) { User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")); return orders.findByCustomerOrderByCreatedAtDesc(user).stream().map(OrderResponse::from).toList(); }

  @Transactional(readOnly = true)
  public OrderResponse getMine(String email, Long id) { User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")); return OrderResponse.from(orders.findByIdAndCustomer(id, user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"))); }

  @Transactional(readOnly = true)
  public List<OrderResponse> all() { return orders.findAll().stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).map(OrderResponse::from).toList(); }

  @Transactional
  public OrderResponse updateStatus(Long id, OrderStatus status) {
    // Lock the order so two concurrent cancellations cannot both restock it.
    Order order = orders.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    OrderStatus current = order.getStatus();
    if (current == status) return OrderResponse.from(order);
    if (current.isTerminal()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change the status of a " + current + " order");
    if (!current.canTransitionTo(status)) {
      String reason = status == OrderStatus.CANCELLED ? "an order cannot be cancelled once it has shipped" : "orders can only move forward";
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot move an order from " + current + " to " + status + ": " + reason);
    }
    if (status == OrderStatus.CANCELLED) restock(order);
    order.setStatus(status);
    order.setUpdatedAt(Instant.now());
    return OrderResponse.from(order);
  }

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
