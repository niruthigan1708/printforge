package com.printforge.dto;

import com.printforge.entity.User;
import java.time.Instant;

public record AccountResponse(Long id, String name, String email, String role, Instant memberSince) {
  public static AccountResponse from(User user) { return new AccountResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.getCreatedAt()); }
}
