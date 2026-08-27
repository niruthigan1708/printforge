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
  public JwtService(@Value("${JWT_SECRET:change-me-to-a-long-random-value-at-least-32-chars}") String secret, @Value("${JWT_EXPIRATION:86400000}") long expiration) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expiration = expiration;
  }
  public String create(User user) {
    return Jwts.builder().subject(user.getEmail()).claim("role", user.getRole().name()).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expiration)).signWith(key).compact();
  }
  public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
