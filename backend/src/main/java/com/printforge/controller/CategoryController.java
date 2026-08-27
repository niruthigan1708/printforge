package com.printforge.controller;

import com.printforge.dto.CategoryResponse;
import com.printforge.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController @RequestMapping("/api/categories") @RequiredArgsConstructor
public class CategoryController {
  private final CategoryRepository categories;
  @GetMapping public List<CategoryResponse> list() { return categories.findAll().stream().map(CategoryResponse::from).toList(); }
}
