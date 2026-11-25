package com.printforge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/** A one-time password reset link. Only the SHA-256 hash of the token is stored. */
@Entity @Table(name = "password_reset_token") @Getter @Setter @NoArgsConstructor
public class PasswordResetToken {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
  @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
  @Column(name = "expires_at", nullable = false) private Instant expiresAt;
  @Column(name = "used_at") private Instant usedAt;
  @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

  public boolean isUsable(Instant now) { return usedAt == null && now.isBefore(expiresAt); }
}
