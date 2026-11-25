package com.printforge.dto;

/**
 * Result of deleting a product. Products that appear in past orders cannot be removed without
 * breaking order history, so they are archived (marked inactive) instead.
 */
public record ProductDeleteResponse(boolean archived, String message) {
  public static ProductDeleteResponse deleted() { return new ProductDeleteResponse(false, "Product deleted"); }
  public static ProductDeleteResponse archivedInstead() {
    return new ProductDeleteResponse(true, "Product has existing orders, so it was archived (hidden from the shop) instead of deleted");
  }
}
