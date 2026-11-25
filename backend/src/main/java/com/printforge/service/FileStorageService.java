package com.printforge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
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
    String originalName = displayName(file.getOriginalFilename());
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

  /**
   * Finds a stored upload on disk, or empty if it is gone. Requests store the absolute path at
   * upload time, which breaks when the upload directory moves (a changed UPLOAD_DIR, a moved
   * project folder), so fall back to the same file name inside the current upload directory.
   */
  public Optional<Path> locate(String storedPath) {
    if (storedPath == null || storedPath.isBlank()) return Optional.empty();
    try {
      Path recorded = Path.of(storedPath);
      if (Files.isRegularFile(recorded) && Files.isReadable(recorded)) return Optional.of(recorded);
      Path relocated = root.resolve(recorded.getFileName().toString()).normalize();
      if (relocated.startsWith(root) && Files.isRegularFile(relocated) && Files.isReadable(relocated)) return Optional.of(relocated);
    } catch (InvalidPathException e) {
      // a path recorded on another OS: treat as missing
    }
    return Optional.empty();
  }

  private static final int MAX_DISPLAY_NAME = 200;

  /**
   * The client-supplied name, kept only for display and the download filename (files are stored
   * under a random UUID). Strips any directory part and control characters without Path.of, which
   * throws on names that are illegal on the server's OS, and truncates it to fit the column while
   * keeping the extension.
   */
  static String displayName(String clientName) {
    String name = clientName == null ? "" : clientName;
    name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
    name = name.replaceAll("[\\p{Cntrl}]", "").trim();
    if (name.isEmpty()) name = "model";
    if (name.length() > MAX_DISPLAY_NAME) {
      int dot = name.lastIndexOf('.');
      String extension = dot > 0 && name.length() - dot <= 10 ? name.substring(dot) : "";
      name = name.substring(0, MAX_DISPLAY_NAME - extension.length()) + extension;
    }
    return name;
  }

  private String extensionOf(String fileName) {
    int dot = fileName.lastIndexOf('.');
    if (dot < 0 || dot == fileName.length() - 1) return "";
    return fileName.substring(dot + 1).toLowerCase();
  }

  public record StoredFile(String originalName, String storedPath, String extension) { }
}
