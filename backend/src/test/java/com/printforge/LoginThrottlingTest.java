package com.printforge;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class LoginThrottlingTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  private ResultActions login(String email, String password, String ip) throws Exception {
    return mvc.perform(post("/api/auth/login").with(request -> { request.setRemoteAddr(ip); return request; })
      .contentType("application/json").content(json.writeValueAsString(Map.of("email", email, "password", password))));
  }

  @Test void repeatedFailuresLockTheAccountEvenForTheRightPassword() throws Exception {
    mvc.perform(post("/api/auth/register").contentType("application/json")
        .content(json.writeValueAsString(Map.of("name", "Throttled", "email", "throttled@example.com", "password", "Correct-Horse-1"))))
      .andExpect(status().isOk());

    for (int i = 0; i < 5; i++) login("throttled@example.com", "wrong-password", "10.1.0.1").andExpect(status().isUnauthorized());

    login("throttled@example.com", "Correct-Horse-1", "10.1.0.2")
      .andExpect(status().isTooManyRequests())
      .andExpect(header().exists("Retry-After"))
      .andExpect(jsonPath("$.message", containsString("Too many failed login attempts")));
  }

  @Test void unknownEmailsCountTowardsTheLimit() throws Exception {
    for (int i = 0; i < 5; i++) login("nobody@example.com", "guess", "10.2.0.1").andExpect(status().isUnauthorized());
    login("nobody@example.com", "guess", "10.2.0.1").andExpect(status().isTooManyRequests());
  }
}
