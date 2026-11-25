package com.printforge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final String[] allowedOrigins;

  /** Comma-separated list of origins allowed to call the API, e.g. "https://shop.example.com,http://localhost:3000". */
  public WebConfig(@Value("${app.cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
    this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(origin -> !origin.isEmpty()).toArray(String[]::new);
    if (this.allowedOrigins.length == 0) throw new IllegalStateException("app.cors.allowed-origins must list at least one origin");
  }

  @Override public void addCorsMappings(CorsRegistry registry) {
    // allowCredentials: the frontend runs on another origin and must send the session cookie.
    // Safe only because allowedOrigins is an explicit list, never "*".
    registry.addMapping("/api/**").allowedOrigins(allowedOrigins).allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").allowCredentials(true);
  }
}
