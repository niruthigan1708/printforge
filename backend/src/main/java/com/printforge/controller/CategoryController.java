package com.printforge.controller;

import com.printforge.dto.CategoryRequest;
import com.printforge.dto.CategoryResponse;
import com.printforge.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/categories") @RequiredArgsConstructor
public class CategoryController {
  private final CategoryService service;
  @GetMapping public List<CategoryResponse> list() { return service.list(); }

  @PostMapping @PreAuthorize("hasRole('ADMIN')")
  public CategoryResponse create(@Valid @RequestBody CategoryRequest request) { return service.create(request); }

  @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
  public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) { return service.update(id, request); }

  @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long id) { service.delete(id); }
}
