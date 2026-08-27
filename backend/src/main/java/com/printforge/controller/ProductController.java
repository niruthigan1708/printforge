package com.printforge.controller;

import com.printforge.dto.ProductRequest;
import com.printforge.dto.ProductResponse;
import com.printforge.service.ProductImageService;
import com.printforge.service.ProductService;
import com.printforge.util.PlaceholderImage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;
import java.time.Duration;
import java.util.List;

@RestController @RequestMapping("/api/products") @RequiredArgsConstructor
public class ProductController {
  private final ProductService service;
  private final ProductImageService productImages;

  @GetMapping public List<ProductResponse> list(@RequestParam(required = false) String search, @RequestParam(required = false) String category) { return service.find(search, category); }
  @GetMapping("/{id}") public ProductResponse get(@PathVariable Long id) { return service.findById(id); }

  @GetMapping("/{id}/image")
  public ResponseEntity<?> image(@PathVariable Long id) {
    ProductResponse product = service.findById(id);
    var stored = productImages.find(id);
    if (stored.isPresent()) {
      Resource resource = stored.get();
      return ResponseEntity.ok().contentType(productImages.mediaTypeFor(resource)).cacheControl(CacheControl.maxAge(Duration.ofDays(1))).body(resource);
    }
    if (product.imageUrl() != null && !product.imageUrl().isBlank()) {
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(product.imageUrl())).build();
    }
    byte[] svg = PlaceholderImage.svg(product.name(), id);
    return ResponseEntity.ok().contentType(MediaType.valueOf("image/svg+xml")).cacheControl(CacheControl.maxAge(Duration.ofHours(1))).body(svg);
  }

  @PostMapping(value = "/{id}/image", consumes = "multipart/form-data") @PreAuthorize("hasRole('ADMIN')")
  public ProductResponse uploadImage(@PathVariable Long id, @RequestParam MultipartFile file) {
    ProductResponse product = service.findById(id);
    productImages.store(id, file);
    return product;
  }

  @PostMapping @PreAuthorize("hasRole('ADMIN')")
  public ProductResponse create(@Valid @RequestBody ProductRequest request) { return service.create(request); }

  @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
  public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) { return service.update(id, request); }

  @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long id) { service.delete(id); }
}
