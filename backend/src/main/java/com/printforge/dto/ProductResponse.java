package com.printforge.dto;

import com.printforge.entity.Product;
import java.math.BigDecimal;

public record ProductResponse(Long id, String name, String description, BigDecimal price, int stockQuantity, String material, String color, String imageUrl, String category, boolean active, String imageVersion) {
  public static ProductResponse from(Product product) {
    return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(), product.getStockQuantity(), product.getMaterial(), product.getColor(), product.getImageUrl(), product.getCategory().getName(), product.isActive(), imageVersion(product));
  }

  /**
   * Changes whenever anything the image endpoint serves could change (uploaded photo, image URL,
   * or the name drawn on the placeholder), because each of those bumps updatedAt. Clients append
   * it as ?v= so a cached photo can be kept for a long time without ever going stale.
   */
  private static String imageVersion(Product product) {
    return product.getUpdatedAt() == null ? "0" : Long.toString(product.getUpdatedAt().toEpochMilli());
  }
}
