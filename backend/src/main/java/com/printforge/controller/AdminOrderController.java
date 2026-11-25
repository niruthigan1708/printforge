package com.printforge.controller;

import com.printforge.dto.OrderResponse;
import com.printforge.dto.OrderStatusUpdateRequest;
import com.printforge.dto.PageResponse;
import com.printforge.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/orders") @RequiredArgsConstructor
public class AdminOrderController {
  private final OrderService service;
  /** Newest first, paged: ?page=0&size=20 (max 100). */
  @GetMapping public PageResponse<OrderResponse> all(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return service.all(PageResponse.newestFirst(page, size)); }
  @PutMapping("/{id}/status") public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) { return service.updateStatus(id, request.status()); }
}
