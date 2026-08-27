package com.printforge.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(String message, int status, Instant timestamp, Map<String, String> errors) {
  public ErrorResponse(String message, int status) { this(message, status, Instant.now(), null); }
  public ErrorResponse(String message, int status, Map<String, String> errors) { this(message, status, Instant.now(), errors); }
}
