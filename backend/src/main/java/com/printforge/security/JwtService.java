package com.printforge.security;

import com.printforge.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import io.jsonwebtoken.Claims;

@Service
public class JwtService {
  private final SecretKey key;
  private final long expiration;
  /** The placeholder shipped in .env.example — publicly known, so it must never sign real tokens. */
  static final String EXAMPLE_SECRET = "change-me-to-a-long-random-value-at-least-32-chars";
  private static final int MIN_SECRET_BYTES = 32;
  /** Claim carrying the user's token_version at issue time. */
  public static final String TOKEN_VERSION_CLAIM = "tv";

  public JwtService(@Value("${JWT_SECRET:}") String secret, @Value("${JWT_EXPIRATION:86400000}") long expiration) {
    this.key = Keys.hmacShaKeyFor(validate(secret).getBytes(StandardCharsets.UTF_8)); this.expiration = expiration;
  }

  /** Fails startup instead of silently signing tokens with a missing, weak, or publicly known secret. */
  static String validate(String secret) {
    if (secret == null || secret.isBlank()) throw new IllegalStateException("JWT_SECRET is not set. Define it in backend/.env or the environment.");
    if (secret.equals(EXAMPLE_SECRET)) throw new IllegalStateException("JWT_SECRET is still the placeholder from .env.example. Replace it with a long random value.");
    if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes long.");
    return secret;
  }
  public String create(User user) {
    // No role claim: the filter reads the role from the database on every request, so a role
    // change applies immediately and a token can never grant more than the account has now.
    return Jwts.builder().subject(user.getEmail()).claim(TOKEN_VERSION_CLAIM, user.getTokenVersion()).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expiration)).signWith(key).compact();
  }
  public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
