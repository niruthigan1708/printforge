package com.printforge.dto;

import com.printforge.entity.User;

public record AuthResponse(String token, Long id, String name, String email, String role) {
  public static AuthResponse from(String token, User user) { return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name()); }
}
