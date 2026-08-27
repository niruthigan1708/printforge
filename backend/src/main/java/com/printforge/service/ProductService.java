package com.printforge.service;

import com.printforge.dto.ProductResponse;
import com.printforge.entity.Product;
import com.printforge.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service @RequiredArgsConstructor
public class ProductService {
  private final ProductRepository products;
  public List<ProductResponse> find(String search, String category) {
    List<Product> result;
    if (search != null && !search.isBlank()) result = products.findByActiveTrueAndNameContainingIgnoreCaseOrderByCreatedAtDesc(search);
    else if (category != null && !category.isBlank()) result = products.findByActiveTrueAndCategory_NameIgnoreCaseOrderByCreatedAtDesc(category);
    else result = products.findByActiveTrueOrderByCreatedAtDesc();
    return result.stream().map(ProductResponse::from).toList();
  }
  public ProductResponse findById(Long id) { return ProductResponse.from(products.findById(id).orElseThrow(() -> new EntityNotFoundException("Product not found"))); }
}
