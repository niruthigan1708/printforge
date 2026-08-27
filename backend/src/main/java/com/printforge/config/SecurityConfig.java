package com.printforge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.printforge.security.JwtAuthenticationFilter;
import java.time.Instant;

@Configuration @EnableWebSecurity @EnableMethodSecurity
public class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
  @Bean SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
    return http.csrf(csrf -> csrf.disable()).cors(cors -> {}).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated())
      .exceptionHandling(handling -> handling
        .authenticationEntryPoint((request, response, ex) -> writeJsonError(response, 401, "Authentication is required"))
        .accessDeniedHandler((request, response, ex) -> writeJsonError(response, 403, "You do not have permission to perform this action")))
      .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
  }

  private void writeJsonError(jakarta.servlet.http.HttpServletResponse response, int status, String message) throws java.io.IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.getWriter().write("{\"message\":\"" + message + "\",\"status\":" + status + ",\"timestamp\":\"" + Instant.now() + "\"}");
  }
}
