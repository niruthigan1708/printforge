package com.printforge.controller;

import com.printforge.dto.OrderIssueResolution;
import com.printforge.dto.OrderResponse;
import com.printforge.dto.OrderStatusUpdateRequest;
import com.printforge.dto.PageResponse;
import com.printforge.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/orders") @RequiredArgsConstructor
public class AdminOrderController {
  private final OrderService service;
  /** Newest first, paged: ?page=0&size=20 (max 100). */
  @GetMapping public PageResponse<OrderResponse> all(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return service.all(PageResponse.newestFirst(page, size)); }
  @GetMapping("/{id}") public OrderResponse get(@PathVariable Long id) { return service.get(id); }
  /** Moving to SHIPPED needs a trackingNumber; DELIVERED is only reachable from SHIPPED. */
  @PutMapping("/{id}/status") public OrderResponse updateStatus(Authentication auth, @PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) { return service.updateStatus(auth.getName(), id, request); }
  /** Closes a customer's "not received" report with an explanation the customer will see. */
  @PutMapping("/{id}/issue/resolve") public OrderResponse resolveIssue(Authentication auth, @PathVariable Long id, @Valid @RequestBody OrderIssueResolution request) { return service.resolveIssue(auth.getName(), id, request.resolution()); }
}
