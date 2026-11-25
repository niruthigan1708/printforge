package com.printforge.service;

import com.printforge.dto.CustomPrintRequestResponse;
import com.printforge.dto.DashboardResponse;
import com.printforge.dto.OrderResponse;
import com.printforge.entity.OrderStatus;
import com.printforge.repository.CustomPrintRequestRepository;
import com.printforge.repository.OrderRepository;
import com.printforge.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dashboard figures come from aggregate queries (counts, a SUM, two top-5 lookups) rather than
 * loading every order and request into memory, so the page stays fast as history grows.
 */
@Service @RequiredArgsConstructor
public class DashboardService {
  private final ProductRepository products;
  private final OrderRepository orders;
  private final CustomPrintRequestRepository customPrints;

  @Transactional(readOnly = true)
  public DashboardResponse summary() {
    return new DashboardResponse(
      products.count(),
      orders.count(),
      orders.countByStatus(OrderStatus.PENDING),
      customPrints.count(),
      orders.sumTotalAmountExcludingStatus(OrderStatus.CANCELLED),
      orders.findTop5ByOrderByCreatedAtDesc().stream().map(OrderResponse::from).toList(),
      customPrints.findTop5ByOrderByCreatedAtDesc().stream().map(CustomPrintRequestResponse::from).toList());
  }
}
