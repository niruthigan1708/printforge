package com.printforge.service;

import com.printforge.dto.AccountResponse;
import com.printforge.dto.AuthResponse;
import com.printforge.entity.User;
import com.printforge.repository.UserRepository;
import com.printforge.security.JwtService;
import com.printforge.security.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** The signed-in user's own account: viewing it, renaming, and changing the password. */
@Service @RequiredArgsConstructor
public class AccountService {
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;
  private final LoginAttemptService passwordAttempts;

  public AccountResponse get(String email) { return AccountResponse.from(find(email)); }

  @Transactional
  public AuthResponse rename(String email, String name) {
    User user = find(email);
    user.setName(name.trim());
    return AuthResponse.from(user);
  }

  /**
   * Changes the password after checking the current one (throttled like logins, so a stolen
   * session cannot be used to guess it), then signs out every other session by bumping the token
   * version. Returns a fresh token so the caller's own session continues.
   */
  @Transactional
  public AuthService.IssuedSession changePassword(String email, String currentPassword, String newPassword, String clientIp) {
    passwordAttempts.checkAllowed(email, clientIp);
    User user = find(email);
    if (!encoder.matches(currentPassword, user.getPassword())) {
      passwordAttempts.recordFailure(email, clientIp);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your current password is incorrect");
    }
    passwordAttempts.recordSuccess(email);
    user.setPassword(encoder.encode(newPassword));
    user.setTokenVersion(user.getTokenVersion() + 1);
    return new AuthService.IssuedSession(jwt.create(user), AuthResponse.from(user));
  }

  private User find(String email) {
    return users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required"));
  }
}
