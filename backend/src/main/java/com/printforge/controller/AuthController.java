package com.printforge.controller;

import com.printforge.dto.*;
import com.printforge.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
  private final AuthService service;
  @PostMapping("/register") public AuthResponse register(@Valid @RequestBody AuthRequest request) { return service.register(request); }
  @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) { return service.login(request, http.getRemoteAddr()); }
}
