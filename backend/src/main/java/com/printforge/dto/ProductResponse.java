package com.printforge.dto;

import com.printforge.entity.Product;
import java.math.BigDecimal;

public record ProductResponse(Long id, String name, String description, BigDecimal price, int stockQuantity, String material, String color, String imageUrl, String category, boolean active) {
  public static ProductResponse from(Product product) {
    return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(), product.getStockQuantity(), product.getMaterial(), product.getColor(), product.getImageUrl(), product.getCategory().getName(), product.isActive());
  }
}
