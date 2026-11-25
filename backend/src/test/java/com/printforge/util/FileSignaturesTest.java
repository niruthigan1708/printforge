package com.printforge.util;

import com.printforge.support.TestFiles;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FileSignaturesTest {
  private static boolean model(String ext, byte[] bytes) throws IOException {
    return FileSignatures.looksLikeModel(ext, bytes.length, () -> new ByteArrayInputStream(bytes));
  }
  private static boolean image(String ext, byte[] bytes) throws IOException {
    return FileSignatures.looksLikeImage(ext, () -> new ByteArrayInputStream(bytes));
  }
  private static byte[] text(String s) { return s.getBytes(StandardCharsets.UTF_8); }

  @Test void acceptsRealModels() throws IOException {
    assertTrue(model("stl", text("solid cube\n facet normal 0 0 1\n endfacet\nendsolid cube\n")));
    assertTrue(model("stl", TestFiles.binaryStl(3)));
    assertTrue(model("STL", TestFiles.binaryStl(0)));
    assertTrue(model("3mf", TestFiles.threeMf()));
    assertTrue(model("obj", text("# exported\nmtllib cube.mtl\nv 0 0 0\nv 1 0 0\nf 1 2 3\n")));
  }

  @Test void rejectsFilesRenamedToModelExtensions() throws IOException {
    byte[] exe = { 'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0 };
    assertFalse(model("stl", exe));
    assertFalse(model("stl", text("<html><script>alert(1)</script></html>")));
    assertFalse(model("obj", text("just some notes, no geometry here")));
    assertFalse(model("obj", exe));
    assertFalse(model("3mf", text("PK but not really a zip")));
  }

  @Test void binaryStlMustMatchItsTriangleCount() throws IOException {
    byte[] stl = TestFiles.binaryStl(2);
    byte[] truncated = java.util.Arrays.copyOf(stl, stl.length - 10);
    assertFalse(model("stl", truncated), "size must equal 84 + 50 * triangles");
  }

  @Test void zipWithoutAModelPartIsNotA3mf() throws IOException {
    var bytes = new java.io.ByteArrayOutputStream();
    try (var zip = new java.util.zip.ZipOutputStream(bytes)) {
      zip.putNextEntry(new java.util.zip.ZipEntry("readme.txt"));
      zip.write(text("hello"));
    }
    assertFalse(model("3mf", bytes.toByteArray()));
  }

  @Test void imagesMustMatchTheirExtension() throws IOException {
    byte[] png = TestFiles.png("x");
    byte[] jpg = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10 };
    byte[] gif = text("GIF89a......");
    byte[] webp = text("RIFF\u0000\u0000\u0000\u0000WEBPVP8 ");

    assertTrue(image("png", png));
    assertTrue(image("jpg", jpg));
    assertTrue(image("jpeg", jpg));
    assertTrue(image("gif", gif));
    assertTrue(image("webp", webp));

    assertFalse(image("jpg", png), "a PNG renamed .jpg would be served with the wrong type");
    assertFalse(image("png", text("<svg onload=alert(1)>")));
    assertFalse(image("gif", new byte[0]));
  }
}
