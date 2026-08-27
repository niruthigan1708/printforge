package com.printforge.controller;

import com.printforge.dto.OrderResponse;
import com.printforge.dto.OrderStatusUpdateRequest;
import com.printforge.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/admin/orders") @RequiredArgsConstructor
public class AdminOrderController {
  private final OrderService service;
  @GetMapping public List<OrderResponse> all() { return service.all(); }
  @PutMapping("/{id}/status") public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) { return service.updateStatus(id, request.status()); }
}
