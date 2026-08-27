package com.printforge.controller;

import com.printforge.dto.*;
import com.printforge.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/orders") @RequiredArgsConstructor
public class OrderController {
  private final OrderService service;
  @PostMapping public OrderResponse create(Authentication auth, @Valid @RequestBody CreateOrderRequest request) { return service.create(auth.getName(), request); }
  @GetMapping public List<OrderResponse> mine(Authentication auth) { return service.mine(auth.getName()); }
  @GetMapping("/{id}") public OrderResponse get(Authentication auth, @PathVariable Long id) { return service.getMine(auth.getName(), id); }
}
