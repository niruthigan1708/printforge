package com.printforge.service;

import com.printforge.dto.CategoryRequest;
import com.printforge.dto.CategoryResponse;
import com.printforge.entity.Category;
import com.printforge.repository.CategoryRepository;
import com.printforge.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service @RequiredArgsConstructor
public class CategoryService {
  private final CategoryRepository categories;
  private final ProductRepository products;

  public List<CategoryResponse> list() {
    return categories.findAll().stream().map(CategoryResponse::from).toList();
  }

  @Transactional
  public CategoryResponse create(CategoryRequest request) {
    if (categories.existsByNameIgnoreCase(request.name())) throw new ResponseStatusException(HttpStatus.CONFLICT, "A category with this name already exists");
    Category category = new Category(request.name(), request.description());
    return CategoryResponse.from(categories.save(category));
  }

  @Transactional
  public CategoryResponse update(Long id, CategoryRequest request) {
    Category category = categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    if (!category.getName().equalsIgnoreCase(request.name()) && categories.existsByNameIgnoreCase(request.name())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A category with this name already exists");
    }
    category.setName(request.name());
    category.setDescription(request.description());
    return CategoryResponse.from(category);
  }

  @Transactional
  public void delete(Long id) {
    Category category = categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    if (products.existsByCategory(category)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a category that still has products");
    categories.delete(category);
  }
}
