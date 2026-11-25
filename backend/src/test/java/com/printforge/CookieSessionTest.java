package com.printforge;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Browser sessions: an httpOnly cookie for authentication, CSRF tokens for writes. */
@SpringBootTest @AutoConfigureMockMvc
class CookieSessionTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  /** Registers a fresh customer and returns the session cookie from the response. */
  private Cookie register(String email) throws Exception {
    MockHttpServletResponse response = mvc.perform(post("/api/auth/register").contentType("application/json")
        .content(json.writeValueAsString(Map.of("name", "Cookie User", "email", email, "password", "Cookie-Pass-1"))))
      .andExpect(status().isOk()).andReturn().getResponse();
    Cookie session = response.getCookie("pf_session");
    assertNotNull(session, "register must set the session cookie");
    return session;
  }

  /** Fetches a CSRF token for this session: the masked header value plus the cookie it pairs with. */
  private record Csrf(String header, String token, Cookie cookie) { }

  private Csrf csrf(Cookie session) throws Exception {
    MockHttpServletResponse response = mvc.perform(get("/api/auth/csrf").cookie(session)).andExpect(status().isOk()).andReturn().getResponse();
    JsonNode body = json.readTree(response.getContentAsString());
    Cookie cookie = response.getCookie("XSRF-TOKEN");
    assertNotNull(cookie, "reading the token must set the CSRF cookie");
    return new Csrf(body.get("headerName").asText(), body.get("token").asText(), cookie);
  }

  @Test void loginSetsAnHttpOnlyCookieAndNeverReturnsTheToken() throws Exception {
    register("cookie-login@example.com");
    mvc.perform(post("/api/auth/login").contentType("application/json")
        .content(json.writeValueAsString(Map.of("email", "cookie-login@example.com", "password", "Cookie-Pass-1"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.token").doesNotExist())
      .andExpect(jsonPath("$.email").value("cookie-login@example.com"))
      .andExpect(header().string("Set-Cookie", allOf(startsWith("pf_session="), containsString("HttpOnly"), containsString("SameSite=Lax"),
        containsString("Path=/api"), not(containsString("Secure")))));
  }

  @Test void cookieIsSecureOverHttps() throws Exception {
    mvc.perform(post("/api/auth/register").secure(true).contentType("application/json")
        .content(json.writeValueAsString(Map.of("name", "Https User", "email", "cookie-https@example.com", "password", "Cookie-Pass-1"))))
      .andExpect(header().string("Set-Cookie", containsString("Secure")));
  }

  @Test void theCookieAuthenticatesReads() throws Exception {
    Cookie session = register("cookie-reads@example.com");
    mvc.perform(get("/api/orders").cookie(session)).andExpect(status().isOk());
    mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("cookie-reads@example.com"));
    mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test void cookieAuthenticatedWritesRequireTheCsrfToken() throws Exception {
    Cookie session = register("cookie-csrf@example.com");

    // Without the token (what a forged cross-site request would look like): refused.
    mvc.perform(put("/api/orders/999999/cancel").cookie(session))
      .andExpect(status().isForbidden())
      .andExpect(jsonPath("$.code").value("CSRF"));

    // A made-up token: refused.
    Csrf csrf = csrf(session);
    mvc.perform(put("/api/orders/999999/cancel").cookie(session, csrf.cookie()).header(csrf.header(), "forged"))
      .andExpect(status().isForbidden());

    // The real token: the request reaches the controller (404: no such order).
    mvc.perform(put("/api/orders/999999/cancel").cookie(session, csrf.cookie()).header(csrf.header(), csrf.token()))
      .andExpect(status().isNotFound());
  }

  @Test void bearerTokenRequestsDoNotNeedCsrf() throws Exception {
    Cookie session = register("cookie-bearer@example.com");
    // An explicit Authorization header cannot be attached by another site, so no CSRF token is needed.
    mvc.perform(put("/api/orders/999999/cancel").header("Authorization", "Bearer " + session.getValue()))
      .andExpect(status().isNotFound());
  }

  @Test void logoutClearsTheCookieAndRevokesIt() throws Exception {
    Cookie session = register("cookie-logout@example.com");
    Csrf csrf = csrf(session);

    MockHttpServletResponse response = mvc.perform(post("/api/auth/logout").cookie(session, csrf.cookie()).header(csrf.header(), csrf.token()))
      .andExpect(status().isNoContent()).andReturn().getResponse();
    assertEquals(0, response.getCookie("pf_session").getMaxAge(), "the cookie must be cleared");

    // Even if a copy of the old cookie is replayed, the session is revoked.
    mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
  }

  @Test void corsAllowsCredentialsForTheConfiguredFrontend() throws Exception {
    mvc.perform(options("/api/auth/me").header("Origin", "http://localhost:3000").header("Access-Control-Request-Method", "GET"))
      .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
      .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
  }
}
