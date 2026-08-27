package com.printforge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {
  private static final List<String> ALLOWED_EXTENSIONS = List.of("stl", "3mf", "obj");
  private static final long MAX_SIZE_BYTES = 20L * 1024 * 1024;

  private final Path root;

  public FileStorageService(@Value("${app.upload-dir:uploads/custom-prints}") String uploadDir) {
    this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    try {
      Files.createDirectories(root);
    } catch (IOException e) {
      throw new IllegalStateException("Could not create upload directory", e);
    }
  }

  public StoredFile store(MultipartFile file) {
    if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A 3D model file is required");
    if (file.getSize() > MAX_SIZE_BYTES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds the maximum allowed size of 20MB");
    String originalName = Path.of(file.getOriginalFilename() == null ? "model" : file.getOriginalFilename()).getFileName().toString();
    String extension = extensionOf(originalName);
    if (!ALLOWED_EXTENSIONS.contains(extension)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type. Please upload an STL, 3MF, or OBJ file");
    String storedName = UUID.randomUUID() + "." + extension;
    Path destination = root.resolve(storedName);
    try {
      Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store uploaded file");
    }
    return new StoredFile(originalName, destination.toString(), extension);
  }

  public Path resolve(String storedPath) {
    return Path.of(storedPath);
  }

  private String extensionOf(String fileName) {
    int dot = fileName.lastIndexOf('.');
    if (dot < 0 || dot == fileName.length() - 1) return "";
    return fileName.substring(dot + 1).toLowerCase();
  }

  public record StoredFile(String originalName, String storedPath, String extension) { }
}
