package com.printforge.controller;

import com.printforge.dto.ProductRequest;
import com.printforge.dto.ProductResponse;
import com.printforge.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/products") @RequiredArgsConstructor
public class ProductController {
  private final ProductService service;
  @GetMapping public List<ProductResponse> list(@RequestParam(required = false) String search, @RequestParam(required = false) String category) { return service.find(search, category); }
  @GetMapping("/{id}") public ProductResponse get(@PathVariable Long id) { return service.findById(id); }

  @PostMapping @PreAuthorize("hasRole('ADMIN')")
  public ProductResponse create(@Valid @RequestBody ProductRequest request) { return service.create(request); }

  @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
  public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) { return service.update(id, request); }

  @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long id) { service.delete(id); }
}
