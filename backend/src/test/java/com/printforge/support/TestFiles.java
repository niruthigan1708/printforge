package com.printforge.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Minimal but structurally valid upload contents, now that uploads are sniffed. */
public final class TestFiles {
  private TestFiles() { }

  private static final byte[] PNG_MAGIC = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };

  /** PNG signature followed by a marker, so tests can tell two "photos" apart. */
  public static byte[] png(String marker) {
    byte[] tail = marker.getBytes(StandardCharsets.UTF_8);
    byte[] out = new byte[PNG_MAGIC.length + tail.length];
    System.arraycopy(PNG_MAGIC, 0, out, 0, PNG_MAGIC.length);
    System.arraycopy(tail, 0, out, PNG_MAGIC.length, tail.length);
    return out;
  }

  /** A ZIP containing the 3D/3dmodel.model part every 3MF package has. */
  public static byte[] threeMf() {
    try (var bytes = new ByteArrayOutputStream(); var zip = new ZipOutputStream(bytes)) {
      zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
      zip.write("<Types/>".getBytes(StandardCharsets.UTF_8));
      zip.putNextEntry(new ZipEntry("3D/3dmodel.model"));
      zip.write("<model unit=\"millimeter\"/>".getBytes(StandardCharsets.UTF_8));
      zip.finish();
      return bytes.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** A binary STL with the given number of (zeroed) triangles. */
  public static byte[] binaryStl(int triangles) {
    ByteBuffer buffer = ByteBuffer.allocate(84 + 50 * triangles).order(ByteOrder.LITTLE_ENDIAN);
    buffer.position(80);
    buffer.putInt(triangles);
    return buffer.array();
  }
}
