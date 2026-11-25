package com.printforge.controller;

import com.printforge.dto.ProductDeleteResponse;
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
  @GetMapping("/{id}") public ProductResponse get(@PathVariable Long id) { return service.findActiveById(id); }

  @GetMapping("/{id}/image")
  public ResponseEntity<?> image(@PathVariable Long id, @RequestParam(name = "v", required = false) String version) {
    // Photos stay available for archived products: carts and past orders still show them.
    ProductResponse product = service.findById(id);
    // A versioned URL (?v=<imageVersion>) names one exact photo, so it can be cached for good;
    // when the photo changes the version changes and clients fetch a new URL. Unversioned URLs
    // must be revalidated every time, or a replaced photo stays stale in browsers.
    CacheControl cache = version != null && version.equals(product.imageVersion())
      ? CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable()
      : CacheControl.noCache();
    var stored = productImages.find(id);
    if (stored.isPresent()) {
      Resource resource = stored.get();
      var response = ResponseEntity.ok().contentType(productImages.mediaTypeFor(resource)).cacheControl(cache);
      // Last-Modified lets an unversioned revalidation come back as a cheap 304.
      long modified = lastModified(resource);
      if (modified > 0) response.lastModified(modified);
      return response.body(resource);
    }
    if (product.imageUrl() != null && !product.imageUrl().isBlank()) {
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(product.imageUrl())).cacheControl(cache).build();
    }
    byte[] svg = PlaceholderImage.svg(product.name(), id);
    return ResponseEntity.ok().contentType(MediaType.valueOf("image/svg+xml")).cacheControl(cache).body(svg);
  }

  private static long lastModified(Resource resource) {
    try { return resource.lastModified(); } catch (java.io.IOException e) { return -1; }
  }

  @PostMapping(value = "/{id}/image", consumes = "multipart/form-data") @PreAuthorize("hasRole('ADMIN')")
  public ProductResponse uploadImage(@PathVariable Long id, @RequestParam MultipartFile file) {
    service.findById(id);
    productImages.store(id, file);
    // Bump the image version so every versioned URL handed out from now on is new.
    return service.markImageChanged(id);
  }

  @PostMapping @PreAuthorize("hasRole('ADMIN')")
  public ProductResponse create(@Valid @RequestBody ProductRequest request) { return service.create(request); }

  @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
  public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) { return service.update(id, request); }

  @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
  public ProductDeleteResponse delete(@PathVariable Long id) { return service.delete(id); }
}
