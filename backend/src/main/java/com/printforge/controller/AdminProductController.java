package com.printforge.controller;

import com.printforge.dto.ProductResponse;
import com.printforge.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController @RequestMapping("/api/admin/products") @RequiredArgsConstructor
public class AdminProductController {
  private final ProductService service;
  @GetMapping public List<ProductResponse> all() { return service.findAllForAdmin(); }
  /** Unlike the public GET /api/products/{id}, this also returns inactive (archived) products. */
  @GetMapping("/{id}") public ProductResponse get(@PathVariable Long id) { return service.findById(id); }
}
