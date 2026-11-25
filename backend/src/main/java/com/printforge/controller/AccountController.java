package com.printforge.controller;

import com.printforge.dto.AccountResponse;
import com.printforge.dto.AuthResponse;
import com.printforge.dto.ChangePasswordRequest;
import com.printforge.dto.UpdateAccountRequest;
import com.printforge.security.SessionCookies;
import com.printforge.service.AccountService;
import com.printforge.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** The signed-in user's own account (any role). */
@RestController @RequestMapping("/api/account") @RequiredArgsConstructor
public class AccountController {
  private final AccountService service;
  private final SessionCookies sessionCookies;

  @GetMapping public AccountResponse get(Authentication auth) { return service.get(auth.getName()); }

  @PutMapping public AuthResponse update(Authentication auth, @Valid @RequestBody UpdateAccountRequest request) {
    return service.rename(auth.getName(), request.name());
  }

  /** Changes the password; every other session is signed out, this one gets a fresh cookie. */
  @PutMapping("/password")
  public AuthResponse changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest request, HttpServletRequest http, HttpServletResponse response) {
    AuthService.IssuedSession session = service.changePassword(auth.getName(), request.currentPassword(), request.newPassword(), http.getRemoteAddr());
    sessionCookies.issue(http, response, session.token());
    return session.user();
  }
}
