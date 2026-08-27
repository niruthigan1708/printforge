package com.printforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class AuthFlowTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @Test void registrationCreatesCustomerAndReturnsToken() throws Exception {
    var body = Map.of("name", "Jane Doe", "email", "jane@example.com", "password", "SecurePass1");
    mvc.perform(post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(body)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.role").value("CUSTOMER"))
      .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test void registeringTheSameEmailTwiceIsRejected() throws Exception {
    var body = Map.of("name", "Dup User", "email", "dup@example.com", "password", "SecurePass1");
    mvc.perform(post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isOk());
    mvc.perform(post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isConflict());
  }

  @Test void loginWithWrongPasswordIsUnauthorized() throws Exception {
    var register = Map.of("name", "Login User", "email", "login@example.com", "password", "SecurePass1");
    mvc.perform(post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(register))).andExpect(status().isOk());
    var badLogin = Map.of("email", "login@example.com", "password", "WrongPassword");
    mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(badLogin))).andExpect(status().isUnauthorized());
  }
}
