package com.printforge.controller;

import com.printforge.dto.ProductResponse;
import com.printforge.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController @RequestMapping("/api/admin/products") @RequiredArgsConstructor
public class AdminProductController {
  private final ProductService service;
  @GetMapping public List<ProductResponse> all() { return service.findAllForAdmin(); }
}
