package com.printforge.service;

import com.printforge.dto.*;
import com.printforge.entity.Role;
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

@Service @RequiredArgsConstructor
public class AuthService {
  private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt; private final LoginAttemptService loginAttempts;
  public AuthResponse register(AuthRequest request) {
    if (users.existsByEmailIgnoreCase(request.email())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
    User user = new User(); user.setName(request.name()); user.setEmail(request.email().toLowerCase()); user.setPassword(encoder.encode(request.password())); user.setRole(Role.CUSTOMER);
    user = users.save(user); return AuthResponse.from(jwt.create(user), user);
  }
  /** Revokes every token issued to this user (all devices), by bumping their token version. */
  @Transactional
  public void logout(String email) {
    users.findByEmailIgnoreCase(email).ifPresent(user -> user.setTokenVersion(user.getTokenVersion() + 1));
  }

  public AuthResponse login(LoginRequest request, String clientIp) {
    // Checked before the password, so a locked-out guesser learns nothing from further tries.
    loginAttempts.checkAllowed(request.email(), clientIp);
    User user = users.findByEmailIgnoreCase(request.email()).orElse(null);
    // Unknown emails count as failures too, so the limit can't be dodged by probing addresses.
    if (user == null || !encoder.matches(request.password(), user.getPassword())) {
      loginAttempts.recordFailure(request.email(), clientIp);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
    loginAttempts.recordSuccess(request.email());
    return AuthResponse.from(jwt.create(user), user);
  }
}
