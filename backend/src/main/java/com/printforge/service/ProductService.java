package com.printforge.service;

import com.printforge.dto.ProductRequest;
import com.printforge.dto.ProductResponse;
import com.printforge.entity.Category;
import com.printforge.entity.Product;
import com.printforge.repository.CategoryRepository;
import com.printforge.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@Service @RequiredArgsConstructor
public class ProductService {
  private final ProductRepository products;
  private final CategoryRepository categories;
  private final ProductImageService productImages;

  public List<ProductResponse> find(String search, String category) {
    List<Product> result;
    if (search != null && !search.isBlank()) result = products.findByActiveTrueAndNameContainingIgnoreCaseOrderByCreatedAtDesc(search);
    else if (category != null && !category.isBlank()) result = products.findByActiveTrueAndCategory_NameIgnoreCaseOrderByCreatedAtDesc(category);
    else result = products.findByActiveTrueOrderByCreatedAtDesc();
    return result.stream().map(ProductResponse::from).toList();
  }

  public ProductResponse findById(Long id) { return ProductResponse.from(products.findById(id).orElseThrow(() -> new EntityNotFoundException("Product not found"))); }

  public List<ProductResponse> findAllForAdmin() { return products.findAllByOrderByCreatedAtDesc().stream().map(ProductResponse::from).toList(); }

  @Transactional
  public ProductResponse create(ProductRequest request) {
    Category category = categories.findById(request.categoryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
    Product product = new Product();
    apply(product, request, category);
    return ProductResponse.from(products.save(product));
  }

  @Transactional
  public ProductResponse update(Long id, ProductRequest request) {
    Product product = products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    Category category = categories.findById(request.categoryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
    apply(product, request, category);
    product.setUpdatedAt(Instant.now());
    return ProductResponse.from(product);
  }

  @Transactional
  public void delete(Long id) {
    Product product = products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    products.delete(product);
    productImages.delete(id);
  }

  private void apply(Product product, ProductRequest request, Category category) {
    product.setName(request.name());
    product.setDescription(request.description());
    product.setPrice(request.price());
    product.setStockQuantity(request.stockQuantity());
    product.setMaterial(request.material());
    product.setColor(request.color());
    product.setImageUrl(request.imageUrl());
    product.setCategory(category);
    product.setActive(request.active() == null || request.active());
  }
}
