package com.printforge.service;

import com.printforge.entity.PasswordResetToken;
import com.printforge.entity.User;
import com.printforge.repository.PasswordResetTokenRepository;
import com.printforge.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Forgot password": emails a one-time link that lets the user choose a new password.
 *
 * <ul>
 *   <li>The request always looks the same whether or not the email has an account, so it cannot
 *       be used to discover who is registered.</li>
 *   <li>Tokens are 256 random bits, stored only as a SHA-256 hash, valid for 30 minutes, single
 *       use; asking again invalidates earlier links.</li>
 *   <li>Requests are rate-limited per email and per IP, so the form cannot be used to flood
 *       someone's inbox.</li>
 *   <li>A successful reset signs the user out everywhere (their token version is bumped).</li>
 * </ul>
 */
@Service
public class PasswordResetService {
  static final Duration VALIDITY = Duration.ofMinutes(30);
  private static final Duration REQUEST_WINDOW = Duration.ofMinutes(15);
  private static final int MAX_REQUESTS_PER_EMAIL = 3;
  private static final int MAX_REQUESTS_PER_IP = 10;

  private final UserRepository users;
  private final PasswordResetTokenRepository tokens;
  private final PasswordEncoder encoder;
  private final PasswordResetMailer mailer;
  private final String frontendUrl;
  private final SecureRandom random = new SecureRandom();
  private final Map<String, Deque<Instant>> recentRequests = new ConcurrentHashMap<>();

  public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens, PasswordEncoder encoder, PasswordResetMailer mailer,
                              @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
    this.users = users;
    this.tokens = tokens;
    this.encoder = encoder;
    this.mailer = mailer;
    this.frontendUrl = frontendUrl.replaceAll("/+$", "");
  }

  /** Emails a reset link if the account exists. Callers must answer the same either way. */
  @Transactional
  public void requestReset(String email, String clientIp) {
    String normalized = email.trim().toLowerCase();
    if (!allow("email:" + normalized, MAX_REQUESTS_PER_EMAIL) | !allow("ip:" + clientIp, MAX_REQUESTS_PER_IP)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many reset requests. Please wait a few minutes and try again.");
    }
    User user = users.findByEmailIgnoreCase(normalized).orElse(null);
    if (user == null) return; // same response as for a real account

    Instant now = Instant.now();
    // Only the newest link works.
    tokens.findByUserAndUsedAtIsNull(user).forEach(old -> old.setUsedAt(now));

    byte[] secret = new byte[32];
    random.nextBytes(secret);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    PasswordResetToken record = new PasswordResetToken();
    record.setUser(user);
    record.setTokenHash(hash(token));
    record.setExpiresAt(now.plus(VALIDITY));
    tokens.save(record);

    mailer.send(user, frontendUrl + "/reset-password?token=" + token, (int) VALIDITY.toMinutes());
  }

  /** Sets a new password from a valid reset link, then signs the user out everywhere. */
  @Transactional
  public void resetPassword(String token, String newPassword) {
    Instant now = Instant.now();
    PasswordResetToken record = tokens.findByTokenHash(hash(token == null ? "" : token))
      .filter(candidate -> candidate.isUsable(now))
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "This reset link is invalid or has expired. Please request a new one."));
    User user = record.getUser();
    user.setPassword(encoder.encode(newPassword));
    user.setTokenVersion(user.getTokenVersion() + 1);
    tokens.findByUserAndUsedAtIsNull(user).forEach(open -> open.setUsedAt(now));
  }

  /** Sliding-window limit; returns false once {@code limit} requests were made within the window. */
  private boolean allow(String key, int limit) {
    Instant now = Instant.now();
    if (recentRequests.size() > 10_000) {
      // Keep memory bounded under a spray of random addresses: drop keys with no recent requests.
      recentRequests.entrySet().removeIf(entry -> { synchronized (entry.getValue()) {
        return entry.getValue().isEmpty() || entry.getValue().peekLast().isBefore(now.minus(REQUEST_WINDOW)); } });
    }
    Deque<Instant> times = recentRequests.computeIfAbsent(key, k -> new ArrayDeque<>());
    synchronized (times) {
      while (!times.isEmpty() && times.peekFirst().isBefore(now.minus(REQUEST_WINDOW))) times.pollFirst();
      if (times.size() >= limit) return false;
      times.addLast(now);
      return true;
    }
  }

  static String hash(String token) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
