package com.printforge;

import com.printforge.entity.Role;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Client mistakes must come back as 4xx with the standard error body, never as a 500. */
@SpringBootTest @AutoConfigureMockMvc
class ErrorHandlingTest {
  @Autowired MockMvc mvc;
  @Autowired TestTokens tokens;

  @Test void malformedJsonIsBadRequest() throws Exception {
    mvc.perform(post("/api/auth/login").contentType("application/json").content("{bad"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.status").value(400))
      .andExpect(jsonPath("$.message").exists());
  }

  @Test void unknownEnumValueIsBadRequest() throws Exception {
    String admin = tokens.tokenFor("errors-admin@example.com", Role.ADMIN);
    mvc.perform(put("/api/admin/orders/1/status").header("Authorization", "Bearer " + admin).contentType("application/json").content("{\"status\":\"FOO\"}"))
      .andExpect(status().isBadRequest());
  }

  @Test void nonNumericIdIsBadRequest() throws Exception {
    mvc.perform(get("/api/products/abc"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
  }

  @Test void missingMultipartFileIsBadRequest() throws Exception {
    String customer = tokens.tokenFor("errors-customer@example.com", Role.CUSTOMER);
    mvc.perform(multipart("/api/custom-prints").param("material", "PLA").param("color", "Black").param("quantity", "1").header("Authorization", "Bearer " + customer))
      .andExpect(status().isBadRequest());
  }

  @Test void unknownPathIsNotFoundForAuthenticatedUser() throws Exception {
    String customer = tokens.tokenFor("errors-customer-2@example.com", Role.CUSTOMER);
    mvc.perform(get("/api/does-not-exist").header("Authorization", "Bearer " + customer))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.status").value(404));
  }

  @Test void unsupportedMethodIsMethodNotAllowed() throws Exception {
    String admin = tokens.tokenFor("errors-admin-2@example.com", Role.ADMIN);
    mvc.perform(patch("/api/products/1").header("Authorization", "Bearer " + admin))
      .andExpect(status().isMethodNotAllowed())
      .andExpect(jsonPath("$.status").value(405));
  }
}
