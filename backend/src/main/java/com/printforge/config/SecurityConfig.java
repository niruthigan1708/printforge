package com.printforge.config;

import com.printforge.security.JwtAuthenticationFilter;
import com.printforge.security.SessionCookies;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.util.matcher.RequestMatcher;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;

@Configuration @EnableWebSecurity @EnableMethodSecurity
public class SecurityConfig {
  private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

  @Bean SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
    return http
      .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens()).requireCsrfProtectionMatcher(csrfRequired()))
      .cors(cors -> {})
      // On top of Spring Security's defaults (nosniff, X-Frame-Options DENY, no-cache for API
      // responses, HSTS over HTTPS): the API only serves JSON and images, so its own responses
      // may load or run nothing. This also neutralises scripts in an SVG opened directly.
      .headers(headers -> headers
        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"))
        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()")))
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        // Logging out and reading the current user need a session; the rest of /api/auth is public.
        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/shop/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated())
      .exceptionHandling(handling -> handling
        .authenticationEntryPoint((request, response, ex) -> writeJsonError(response, 401, "Authentication is required", null))
        .accessDeniedHandler((request, response, ex) -> {
          if (ex instanceof CsrfException) {
            // A distinct code lets the frontend fetch a fresh token and retry once.
            writeJsonError(response, 403, "Your security token is missing or expired. Please try again.", "CSRF");
          } else {
            writeJsonError(response, 403, "You do not have permission to perform this action", null);
          }
        }))
      .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
  }

  /**
   * Double-submit CSRF tokens: the token is kept in an httpOnly cookie, the frontend reads it from
   * GET /api/auth/csrf and echoes it in the X-XSRF-TOKEN header. SameSite=Lax on both cookies
   * already stops most cross-site requests; this also covers multipart uploads, which browsers
   * send cross-site without a CORS preflight.
   */
  private static CookieCsrfTokenRepository csrfTokens() {
    CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();
    repository.setCookiePath("/api");
    repository.setCookieCustomizer(cookie -> cookie.httpOnly(true).sameSite("Lax"));
    return repository;
  }

  /**
   * CSRF only threatens requests the browser authenticates automatically, i.e. state-changing
   * requests carrying the session cookie. Requests with an explicit Authorization header (API
   * clients) cannot be forged by another site, and anonymous ones (login, register) have no
   * session to abuse.
   */
  private static RequestMatcher csrfRequired() {
    return request -> !SAFE_METHODS.contains(request.getMethod())
      && request.getHeader(HttpHeaders.AUTHORIZATION) == null
      && SessionCookies.read(request).isPresent();
  }

  private static void writeJsonError(HttpServletResponse response, int status, String message, String code) throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    String codeField = code == null ? "" : ",\"code\":\"" + code + "\"";
    response.getWriter().write("{\"message\":\"" + message + "\",\"status\":" + status + codeField + ",\"timestamp\":\"" + Instant.now() + "\"}");
  }
}
