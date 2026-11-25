package com.printforge.controller;

import com.printforge.dto.*;
import com.printforge.security.SessionCookies;
import com.printforge.service.AuthService;
import com.printforge.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
  private final AuthService service;
  private final SessionCookies sessionCookies;
  private final PasswordResetService passwordReset;

  /** Registers a customer and signs them in: the session goes into an httpOnly cookie. */
  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody AuthRequest request, HttpServletRequest http, HttpServletResponse response) {
    return startSession(service.register(request), http, response);
  }

  /** Signs in: the session goes into an httpOnly cookie; the body holds only the user. */
  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http, HttpServletResponse response) {
    return startSession(service.login(request, http.getRemoteAddr()), http, response);
  }

  /** The signed-in user; how the frontend confirms a session cookie is still valid. */
  @GetMapping("/me") public AuthResponse me(Authentication auth) { return service.currentUser(auth.getName()); }

  /**
   * The CSRF token the browser must echo in the X-XSRF-TOKEN header on state-changing requests
   * made with the session cookie. Reading it also (re)sets the matching CSRF cookie.
   */
  @GetMapping("/csrf") public Map<String, String> csrf(CsrfToken token) {
    return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
  }

  /**
   * Emails a password reset link if the account exists. Always 202 with the same body, so the
   * form cannot be used to find out who has an account.
   */
  @PostMapping("/forgot-password") @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
    passwordReset.requestReset(request.email(), http.getRemoteAddr());
    return Map.of("message", "If an account exists for that email, we've sent a link to reset the password.");
  }

  /** Sets a new password from a reset link; signs the user out everywhere. */
  @PostMapping("/reset-password") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    passwordReset.resetPassword(request.token(), request.password());
  }

  /** Ends every session of the caller: all tokens issued to them stop working, and the cookie is cleared. */
  @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(Authentication auth, HttpServletRequest http, HttpServletResponse response) {
    service.logout(auth.getName());
    sessionCookies.clear(http, response);
  }

  private AuthResponse startSession(AuthService.IssuedSession session, HttpServletRequest http, HttpServletResponse response) {
    sessionCookies.issue(http, response, session.token());
    return session.user();
  }
}
