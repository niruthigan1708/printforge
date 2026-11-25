package com.printforge.security;

import com.printforge.entity.User;
import com.printforge.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests carrying a valid bearer token. The token only identifies the user: the
 * account is re-read on every request, so a token is rejected once the user is deleted or has
 * logged out (token_version bumped), and the role always comes from the database, never from
 * the token, so a role change applies immediately.
 */
@Component @RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  private final UserRepository users;

  @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    // Browsers send the httpOnly session cookie; API clients may send a bearer token instead.
    String header = request.getHeader("Authorization");
    String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : SessionCookies.read(request).orElse(null);
    if (token != null) {
      try {
        Claims claims = jwt.parse(token);
        Number tokenVersion = claims.get(JwtService.TOKEN_VERSION_CLAIM, Number.class);
        User user = users.findByEmailIgnoreCase(claims.getSubject()).orElse(null);
        // Tokens issued before versioning, for deleted users, or before a logout are not accepted.
        if (tokenVersion != null && user != null && tokenVersion.intValue() == user.getTokenVersion()) {
          var auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      } catch (RuntimeException ignored) {
        // Invalid, expired or tampered token: continue unauthenticated; protected endpoints answer 401.
      }
    }
    chain.doFilter(request, response);
  }
}
