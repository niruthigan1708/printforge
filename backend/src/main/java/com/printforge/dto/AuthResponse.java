package com.printforge.dto;

import com.printforge.entity.User;

/**
 * The signed-in user. Deliberately contains no token: browser sessions live in an httpOnly
 * cookie that page scripts cannot read.
 */
public record AuthResponse(Long id, String name, String email, String role) {
  public static AuthResponse from(User user) { return new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name()); }
}
