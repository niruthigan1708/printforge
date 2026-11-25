package com.printforge;

import tools.jackson.databind.ObjectMapper;
import com.printforge.entity.Role;
import com.printforge.support.TestTokens;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** A signed-in user managing their own account. */
@SpringBootTest @AutoConfigureMockMvc
class AccountTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired TestTokens tokens;

  private String bearer(String email) { return "Bearer " + tokens.tokenFor(email, Role.CUSTOMER); }

  @Test void usersCanViewAndRenameTheirAccount() throws Exception {
    String auth = bearer("account-view@example.com");
    mvc.perform(get("/api/account").header("Authorization", auth))
      .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("account-view@example.com")).andExpect(jsonPath("$.memberSince").exists());

    mvc.perform(put("/api/account").header("Authorization", auth).contentType("application/json").content("{\"name\":\"  Renamed Person  \"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Renamed Person"));
    // Role cannot be changed through this endpoint, whatever is sent.
    mvc.perform(put("/api/account").header("Authorization", auth).contentType("application/json").content("{\"name\":\"X\",\"role\":\"ADMIN\"}"))
      .andExpect(jsonPath("$.role").value("CUSTOMER"));
  }

  @Test void accountRequiresASession() throws Exception {
    mvc.perform(get("/api/account")).andExpect(status().isUnauthorized());
  }

  @Test void changingThePasswordNeedsTheCurrentOne() throws Exception {
    String auth = bearer("account-wrong@example.com"); // password "Password1"
    mvc.perform(put("/api/account/password").header("Authorization", auth).contentType("application/json")
        .content(json.writeValueAsString(Map.of("currentPassword", "not-it", "newPassword", "Another-Pass-1"))))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Your current password is incorrect"));
  }

  @Test void changingThePasswordSignsOutOtherSessionsButKeepsThisOne() throws Exception {
    String otherDevice = bearer("account-change@example.com");
    String thisDevice = bearer("account-change@example.com");

    MockHttpServletResponse response = mvc.perform(put("/api/account/password").header("Authorization", thisDevice).contentType("application/json")
        .content(json.writeValueAsString(Map.of("currentPassword", "Password1", "newPassword", "Changed-Pass-77"))))
      .andExpect(status().isOk()).andReturn().getResponse();

    Cookie fresh = response.getCookie("pf_session");
    assertNotNull(fresh, "this session gets a fresh cookie");
    mvc.perform(get("/api/auth/me").cookie(fresh)).andExpect(status().isOk());
    mvc.perform(get("/api/auth/me").header("Authorization", otherDevice)).andExpect(status().isUnauthorized());

    mvc.perform(post("/api/auth/login").with(r -> { r.setRemoteAddr("10.8.0.1"); return r; }).contentType("application/json")
        .content(json.writeValueAsString(Map.of("email", "account-change@example.com", "password", "Changed-Pass-77"))))
      .andExpect(status().isOk());
  }

  @Test void newPasswordsMustMeetTheMinimumLength() throws Exception {
    mvc.perform(put("/api/account/password").header("Authorization", bearer("account-short@example.com")).contentType("application/json")
        .content(json.writeValueAsString(Map.of("currentPassword", "Password1", "newPassword", "short"))))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.newPassword").exists());
  }
}
