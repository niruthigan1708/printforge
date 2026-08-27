package com.printforge.controller;

import com.printforge.dto.ProductResponse;
import com.printforge.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/products") @RequiredArgsConstructor
public class ProductController {
  private final ProductService service;
  @GetMapping public List<ProductResponse> list(@RequestParam(required = false) String search, @RequestParam(required = false) String category) { return service.find(search, category); }
  @GetMapping("/{id}") public ProductResponse get(@PathVariable Long id) { return service.findById(id); }
}
