package com.printforge.service;

import com.printforge.dto.*;
import com.printforge.entity.Role;
import com.printforge.entity.User;
import com.printforge.repository.UserRepository;
import com.printforge.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service @RequiredArgsConstructor
public class AuthService {
  private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
  public AuthResponse register(AuthRequest request) {
    if (users.existsByEmailIgnoreCase(request.email())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
    User user = new User(); user.setName(request.name()); user.setEmail(request.email().toLowerCase()); user.setPassword(encoder.encode(request.password())); user.setRole(Role.CUSTOMER);
    user = users.save(user); return AuthResponse.from(jwt.create(user), user);
  }
  public AuthResponse login(LoginRequest request) {
    User user = users.findByEmailIgnoreCase(request.email()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
    if (!encoder.matches(request.password(), user.getPassword())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    return AuthResponse.from(jwt.create(user), user);
  }
}
