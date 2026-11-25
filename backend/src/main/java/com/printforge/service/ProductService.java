package com.printforge.service;

import com.printforge.dto.ProductDeleteResponse;
import com.printforge.dto.ProductRequest;
import com.printforge.dto.ProductResponse;
import com.printforge.entity.Category;
import com.printforge.entity.Product;
import com.printforge.repository.CategoryRepository;
import com.printforge.repository.OrderItemRepository;
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
  private final OrderItemRepository orderItems;

  public List<ProductResponse> find(String search, String category) {
    boolean hasSearch = search != null && !search.isBlank();
    boolean hasCategory = category != null && !category.isBlank();
    List<Product> result;
    if (hasSearch && hasCategory) result = products.findByActiveTrueAndNameContainingIgnoreCaseAndCategory_NameIgnoreCaseOrderByCreatedAtDesc(search.trim(), category.trim());
    else if (hasSearch) result = products.findByActiveTrueAndNameContainingIgnoreCaseOrderByCreatedAtDesc(search.trim());
    else if (hasCategory) result = products.findByActiveTrueAndCategory_NameIgnoreCaseOrderByCreatedAtDesc(category.trim());
    else result = products.findByActiveTrueOrderByCreatedAtDesc();
    return result.stream().map(ProductResponse::from).toList();
  }

  /** Public lookup: archived/inactive products are hidden, exactly as in the catalogue listing. */
  public ProductResponse findActiveById(Long id) {
    return products.findById(id).filter(Product::isActive).map(ProductResponse::from).orElseThrow(() -> new EntityNotFoundException("Product not found"));
  }

  /** Admin lookup: includes inactive products so they can still be edited or reactivated. */
  public ProductResponse findById(Long id) { return ProductResponse.from(products.findById(id).orElseThrow(() -> new EntityNotFoundException("Product not found"))); }

  public List<ProductResponse> findAllForAdmin() { return products.findAllByOrderByCreatedAtDesc().stream().map(ProductResponse::from).toList(); }

  @Transactional
  public ProductResponse create(ProductRequest request) {
    Category category = categories.findById(request.categoryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
    Product product = new Product();
    apply(product, request, category);
    product.setStockQuantity(request.stockQuantity());
    return ProductResponse.from(products.save(product));
  }

  @Transactional
  public ProductResponse update(Long id, ProductRequest request) {
    // Same row lock checkout takes, so an order cannot change the stock between the check below
    // and the save.
    Product product = products.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    Category category = categories.findById(request.categoryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
    applyStock(product, request);
    apply(product, request, category);
    product.setUpdatedAt(Instant.now());
    return ProductResponse.from(product);
  }

  /**
   * Saving the edit form used to write back whatever stock number the form was opened with,
   * silently undoing any sales or cancellations that happened while it was open.
   */
  private void applyStock(Product product, ProductRequest request) {
    Integer expected = request.expectedStockQuantity();
    int current = product.getStockQuantity();
    if (expected == null) {                          // no baseline sent: explicit overwrite
      product.setStockQuantity(request.stockQuantity());
      return;
    }
    if (request.stockQuantity() == expected) return; // stock field untouched: keep the live value
    if (current != expected) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock changed from " + expected + " to " + current
        + " while you were editing (orders were placed or cancelled). Review the current stock and save again.");
    }
    product.setStockQuantity(request.stockQuantity());
  }

  /** Records that the product's photo changed, which bumps its image version. */
  @Transactional
  public ProductResponse markImageChanged(Long id) {
    Product product = products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    product.setUpdatedAt(Instant.now());
    return ProductResponse.from(product);
  }

  @Transactional
  public ProductDeleteResponse delete(Long id) {
    Product product = products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    if (orderItems.existsByProduct(product)) {
      // Order items reference the product, so a hard delete would violate the foreign key.
      // Archive it instead: hidden from the shop, kept for order history.
      product.setActive(false);
      product.setUpdatedAt(Instant.now());
      return ProductDeleteResponse.archivedInstead();
    }
    products.delete(product);
    productImages.delete(id);
    return ProductDeleteResponse.deleted();
  }

  private void apply(Product product, ProductRequest request, Category category) {
    product.setName(request.name());
    product.setDescription(request.description());
    product.setPrice(request.price());
    product.setMaterial(request.material());
    product.setColor(request.color());
    product.setImageUrl(request.imageUrl());
    product.setCategory(category);
    product.setActive(request.active() == null || request.active());
  }
}
