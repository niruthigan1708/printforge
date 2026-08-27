package com.printforge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

/** Stores product photos on the local filesystem, one file per product — same pattern as custom-print uploads. */
@Service
public class ProductImageService {
  private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp", "gif");
  private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

  private final Path root;

  public ProductImageService(@Value("${app.product-image-dir:uploads/products}") String dir) {
    this.root = Path.of(dir).toAbsolutePath().normalize();
    try {
      Files.createDirectories(root);
    } catch (IOException e) {
      throw new IllegalStateException("Could not create product image directory", e);
    }
  }

  public void store(Long productId, MultipartFile file) {
    if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An image file is required");
    if (file.getSize() > MAX_SIZE_BYTES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image exceeds the maximum allowed size of 5MB");
    String originalName = Path.of(file.getOriginalFilename() == null ? "photo" : file.getOriginalFilename()).getFileName().toString();
    String extension = extensionOf(originalName);
    if (!ALLOWED_EXTENSIONS.contains(extension)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image type. Please upload a JPG, PNG, WEBP, or GIF file");
    delete(productId);
    Path destination = root.resolve(productId + "." + extension);
    try {
      Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store the uploaded image");
    }
  }

  public Optional<Resource> find(Long productId) {
    try (var stream = Files.list(root)) {
      return stream.filter(path -> path.getFileName().toString().startsWith(productId + ".")).findFirst().map(FileSystemResource::new);
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  public void delete(Long productId) {
    try (var stream = Files.list(root)) {
      stream.filter(path -> path.getFileName().toString().startsWith(productId + ".")).forEach(path -> {
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
      });
    } catch (IOException ignored) { }
  }

  public MediaType mediaTypeFor(Resource resource) {
    String name = resource.getFilename() == null ? "" : resource.getFilename().toLowerCase();
    if (name.endsWith(".png")) return MediaType.IMAGE_PNG;
    if (name.endsWith(".webp")) return MediaType.valueOf("image/webp");
    if (name.endsWith(".gif")) return MediaType.IMAGE_GIF;
    return MediaType.IMAGE_JPEG;
  }

  private String extensionOf(String fileName) {
    int dot = fileName.lastIndexOf('.');
    if (dot < 0 || dot == fileName.length() - 1) return "";
    return fileName.substring(dot + 1).toLowerCase();
  }
}
