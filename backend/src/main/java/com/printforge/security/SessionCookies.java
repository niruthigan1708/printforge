package com.printforge.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * The browser session: the JWT travels in an httpOnly cookie that page scripts cannot read, so a
 * script injected into the page (XSS) cannot steal it, unlike the localStorage token it replaces.
 *
 * <ul>
 *   <li>HttpOnly: invisible to JavaScript.</li>
 *   <li>SameSite=Lax: not sent on cross-site POSTs, the classic CSRF vector (CSRF tokens add a
 *       second layer, see SecurityConfig).</li>
 *   <li>Secure whenever the request arrived over HTTPS (including via a TLS-terminating proxy,
 *       thanks to forwarded-header support), so it is never sent over plain HTTP in production
 *       while still working on http://localhost.</li>
 *   <li>Path=/api: only sent to the API.</li>
 * </ul>
 */
@Component
public class SessionCookies {
  public static final String NAME = "pf_session";
  private final Duration maxAge;

  public SessionCookies(@Value("${JWT_EXPIRATION:86400000}") long expirationMillis) {
    this.maxAge = Duration.ofMillis(expirationMillis);
  }

  public void issue(HttpServletRequest request, HttpServletResponse response, String token) {
    response.addHeader(HttpHeaders.SET_COOKIE, cookie(request, token, maxAge).toString());
  }

  public void clear(HttpServletRequest request, HttpServletResponse response) {
    response.addHeader(HttpHeaders.SET_COOKIE, cookie(request, "", Duration.ZERO).toString());
  }

  public static Optional<String> read(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return Optional.empty();
    return Arrays.stream(cookies).filter(c -> NAME.equals(c.getName()) && !c.getValue().isBlank()).map(Cookie::getValue).findFirst();
  }

  private static ResponseCookie cookie(HttpServletRequest request, String value, Duration maxAge) {
    return ResponseCookie.from(NAME, value).httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/api").maxAge(maxAge).build();
  }
}
