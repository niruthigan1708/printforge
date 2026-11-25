package com.printforge.controller;

import com.printforge.dto.*;
import com.printforge.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/orders") @RequiredArgsConstructor
public class OrderController {
  private final OrderService service;
  @PostMapping public OrderResponse create(Authentication auth, @Valid @RequestBody CreateOrderRequest request) { return service.create(auth.getName(), request); }
  /** The caller's orders, newest first, paged: ?page=0&size=20 (max 100). */
  @GetMapping public PageResponse<OrderResponse> mine(Authentication auth, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.mine(auth.getName(), PageResponse.newestFirst(page, size));
  }
  @GetMapping("/{id}") public OrderResponse get(Authentication auth, @PathVariable Long id) { return service.getMine(auth.getName(), id); }
  /** Customers can cancel their own order while it is still PENDING. */
  @PutMapping("/{id}/cancel") public OrderResponse cancel(Authentication auth, @PathVariable Long id) { return service.cancelMine(auth.getName(), id); }
}
