package com.printforge.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Checks that an upload's bytes really are the format its extension claims, so a renamed
 * executable, HTML page or archive is not stored and later served or handed to an admin as a
 * "model" or "photo". These are structural sniff tests, not full parsers.
 */
public final class FileSignatures {
  private FileSignatures() { }

  private static final int SNIFF_BYTES = 64 * 1024;

  /** Opens the upload's content; lets callers pass MultipartFile::getInputStream. */
  @FunctionalInterface
  public interface Source { InputStream open() throws IOException; }

  // ---- 3D models ---------------------------------------------------------------------------

  public static boolean looksLikeModel(String extension, long size, Source source) throws IOException {
    return switch (extension.toLowerCase(Locale.ROOT)) {
      case "stl" -> looksLikeStl(size, source);
      case "3mf" -> looksLike3mf(source);
      case "obj" -> looksLikeObj(source);
      default -> false;
    };
  }

  /** Binary STL: 80-byte header, uint32 triangle count, 50 bytes per triangle. ASCII STL: "solid ...". */
  static boolean looksLikeStl(long size, Source source) throws IOException {
    byte[] head = readHead(source, SNIFF_BYTES);
    if (size >= 84 && head.length >= 84) {
      long triangles = Integer.toUnsignedLong(ByteBuffer.wrap(head, 80, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
      if (size == 84 + 50 * triangles) return true;
    }
    String text = new String(head, StandardCharsets.US_ASCII).stripLeading();
    return text.regionMatches(true, 0, "solid", 0, 5) && isMostlyText(head);
  }

  /** 3MF is a ZIP (OPC) package whose model part lives at 3D/3dmodel.model. */
  static boolean looksLike3mf(Source source) throws IOException {
    byte[] head = readHead(source, 4);
    if (!startsWith(head, new byte[] { 'P', 'K', 3, 4 })) return false;
    try (ZipInputStream zip = new ZipInputStream(source.open())) {
      ZipEntry entry;
      for (int seen = 0; seen < 200 && (entry = zip.getNextEntry()) != null; seen++) {
        if (entry.getName().equalsIgnoreCase("3D/3dmodel.model")) return true;
      }
    } catch (IOException | IllegalArgumentException malformed) {
      return false;
    }
    return false;
  }

  /** OBJ is plain text with geometry statements such as "v 1 2 3" and "f 1 2 3". */
  static boolean looksLikeObj(Source source) throws IOException {
    byte[] head = readHead(source, SNIFF_BYTES);
    if (!isMostlyText(head)) return false;
    for (String line : new String(head, StandardCharsets.UTF_8).split("\\R")) {
      String trimmed = line.strip();
      if (trimmed.matches("^(v|vt|vn|f|o|g|mtllib|usemtl)\\s.*")) return true;
    }
    return false;
  }

  // ---- images ------------------------------------------------------------------------------

  /** True if the bytes are the image format named by the extension (jpg/jpeg, png, gif, webp). */
  public static boolean looksLikeImage(String extension, Source source) throws IOException {
    byte[] head = readHead(source, 12);
    return switch (extension.toLowerCase(Locale.ROOT)) {
      case "jpg", "jpeg" -> startsWith(head, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
      case "png" -> startsWith(head, new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' });
      case "gif" -> startsWith(head, "GIF87a".getBytes(StandardCharsets.US_ASCII)) || startsWith(head, "GIF89a".getBytes(StandardCharsets.US_ASCII));
      case "webp" -> head.length >= 12 && startsWith(head, "RIFF".getBytes(StandardCharsets.US_ASCII))
        && Arrays.equals(Arrays.copyOfRange(head, 8, 12), "WEBP".getBytes(StandardCharsets.US_ASCII));
      default -> false;
    };
  }

  // ---- helpers -----------------------------------------------------------------------------

  private static byte[] readHead(Source source, int max) throws IOException {
    try (InputStream in = source.open()) { return in.readNBytes(max); }
  }

  private static boolean startsWith(byte[] data, byte[] prefix) {
    return data.length >= prefix.length && Arrays.equals(Arrays.copyOf(data, prefix.length), prefix);
  }

  /** No NUL bytes and almost no control characters other than whitespace. */
  private static boolean isMostlyText(byte[] data) {
    if (data.length == 0) return false;
    int control = 0;
    for (byte b : data) {
      if (b == 0) return false;
      if (b < 0x20 && b != '\n' && b != '\r' && b != '\t' && b != '\f') control++;
    }
    return control <= data.length / 100;
  }
}
