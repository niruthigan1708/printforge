package com.printforge.service;

import com.printforge.dto.CustomPrintRequestResponse;
import com.printforge.dto.DashboardResponse;
import com.printforge.dto.OrderResponse;
import com.printforge.entity.Order;
import com.printforge.entity.OrderStatus;
import com.printforge.repository.CustomPrintRequestRepository;
import com.printforge.repository.OrderRepository;
import com.printforge.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Comparator;

@Service @RequiredArgsConstructor
public class DashboardService {
  private final ProductRepository products;
  private final OrderRepository orders;
  private final CustomPrintRequestRepository customPrints;

  @Transactional(readOnly = true)
  public DashboardResponse summary() {
    var allOrders = orders.findAll();
    long pending = allOrders.stream().filter(order -> order.getStatus() == OrderStatus.PENDING).count();
    BigDecimal revenue = allOrders.stream().filter(order -> order.getStatus() != OrderStatus.CANCELLED).map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    var recentOrders = allOrders.stream().sorted(Comparator.comparing(Order::getCreatedAt).reversed()).limit(5).map(OrderResponse::from).toList();
    var recentRequests = customPrints.findAllByOrderByCreatedAtDesc().stream().limit(5).map(CustomPrintRequestResponse::from).toList();
    return new DashboardResponse(products.count(), allOrders.size(), pending, customPrints.count(), revenue, recentOrders, recentRequests);
  }
}
