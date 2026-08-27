package com.printforge.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Generates a branded SVG tile (color + product name) for products with no photo on file. */
public final class PlaceholderImage {
  private PlaceholderImage() { }

  private static final String[][] TONES = {
    { "#ff5c35", "#ffffff" },
    { "#d7f36b", "#171717" },
    { "#cfe9ff", "#171717" },
    { "#e9e0d5", "#171717" },
    { "#171717", "#ffffff" },
  };

  public static byte[] svg(String label, long seed) {
    String[] tone = TONES[Math.floorMod(seed, TONES.length)];
    String escaped = label.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    List<String> lines = wrap(escaped, 16);
    int startY = 300 - (lines.size() - 1) * 20;
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < lines.size(); i++) {
      text.append("<tspan x=\"300\" y=\"").append(startY + i * 40).append("\">").append(lines.get(i)).append("</tspan>");
    }
    String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"600\" height=\"600\" viewBox=\"0 0 600 600\">"
      + "<rect width=\"600\" height=\"600\" fill=\"" + tone[0] + "\"/>"
      + "<text font-family=\"Arial, sans-serif\" font-size=\"32\" font-weight=\"700\" fill=\"" + tone[1] + "\" text-anchor=\"middle\" dominant-baseline=\"middle\">" + text + "</text>"
      + "</svg>";
    return svg.getBytes(StandardCharsets.UTF_8);
  }

  private static List<String> wrap(String text, int maxCharsPerLine) {
    List<String> lines = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (String word : text.split(" ")) {
      if (current.length() > 0 && current.length() + 1 + word.length() > maxCharsPerLine) {
        lines.add(current.toString());
        current = new StringBuilder();
      }
      if (current.length() > 0) current.append(' ');
      current.append(word);
    }
    if (current.length() > 0) lines.add(current.toString());
    return lines;
  }
}
