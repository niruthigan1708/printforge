package com.printforge.controller;

import com.printforge.dto.PageResponse;
import com.printforge.dto.ProductResponse;
import com.printforge.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/admin/products") @RequiredArgsConstructor
public class AdminProductController {
  private final ProductService service;
  /** Newest first, paged: ?page=0&size=20 (max 100); includes inactive products. */
  @GetMapping public PageResponse<ProductResponse> all(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return service.findAllForAdmin(PageResponse.newestFirst(page, size)); }
  /** Unlike the public GET /api/products/{id}, this also returns inactive (archived) products. */
  @GetMapping("/{id}") public ProductResponse get(@PathVariable Long id) { return service.findById(id); }
}
