package com.printforge;

import tools.jackson.databind.ObjectMapper;
import com.printforge.entity.PasswordResetToken;
import com.printforge.entity.Role;
import com.printforge.entity.User;
import com.printforge.repository.PasswordResetTokenRepository;
import com.printforge.service.PasswordResetMailer;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.time.Instant;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Forgot-password: one-time emailed links that cannot be guessed, reused, or used to find accounts. */
@SpringBootTest @AutoConfigureMockMvc
class PasswordResetTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired TestTokens tokens;
  @Autowired PasswordResetTokenRepository resetTokens;
  @MockitoSpyBean PasswordResetMailer mailer;

  private ResultActions forgot(String email, String ip) throws Exception {
    return mvc.perform(post("/api/auth/forgot-password").with(r -> { r.setRemoteAddr(ip); return r; })
      .contentType("application/json").content(json.writeValueAsString(Map.of("email", email))));
  }

  private ResultActions reset(String token, String password) throws Exception {
    return mvc.perform(post("/api/auth/reset-password").contentType("application/json")
      .content(json.writeValueAsString(Map.of("token", token, "password", password))));
  }

  private ResultActions login(String email, String password) throws Exception {
    return mvc.perform(post("/api/auth/login").with(r -> { r.setRemoteAddr("10.9.9.9"); return r; })
      .contentType("application/json").content(json.writeValueAsString(Map.of("email", email, "password", password))));
  }

  /** The token from the most recent link emailed to this address. */
  private String emailedToken(String email) {
    ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
    verify(mailer, atLeastOnce()).send(argThat((User u) -> u.getEmail().equals(email)), link.capture(), anyInt());
    String url = link.getValue();
    assertTrue(url.startsWith("http://localhost:3000/reset-password?token="), url);
    return url.substring(url.indexOf("token=") + 6);
  }

  @Test void resetLinkSetsANewPasswordAndSignsOutEverywhere() throws Exception {
    String oldSession = tokens.tokenFor("reset-me@example.com", Role.CUSTOMER); // password "Password1"
    forgot("reset-me@example.com", "10.3.0.1").andExpect(status().isAccepted());

    reset(emailedToken("reset-me@example.com"), "Brand-New-Pass-9").andExpect(status().isNoContent());

    login("reset-me@example.com", "Password1").andExpect(status().isUnauthorized());
    login("reset-me@example.com", "Brand-New-Pass-9").andExpect(status().isOk());
    mvc.perform(get("/api/orders").header("Authorization", "Bearer " + oldSession)).andExpect(status().isUnauthorized());
  }

  @Test void linksAreSingleUse() throws Exception {
    tokens.tokenFor("reset-once@example.com", Role.CUSTOMER);
    forgot("reset-once@example.com", "10.3.0.2").andExpect(status().isAccepted());
    String token = emailedToken("reset-once@example.com");

    reset(token, "First-New-Pass-1").andExpect(status().isNoContent());
    reset(token, "Second-New-Pass-2").andExpect(status().isBadRequest()).andExpect(jsonPath("$.message", containsString("invalid or has expired")));
  }

  @Test void askingAgainInvalidatesTheEarlierLink() throws Exception {
    tokens.tokenFor("reset-twice@example.com", Role.CUSTOMER);
    forgot("reset-twice@example.com", "10.3.0.3").andExpect(status().isAccepted());
    String first = emailedToken("reset-twice@example.com");
    forgot("reset-twice@example.com", "10.3.0.3").andExpect(status().isAccepted());
    String second = emailedToken("reset-twice@example.com");

    reset(first, "Some-New-Pass-1").andExpect(status().isBadRequest());
    reset(second, "Some-New-Pass-1").andExpect(status().isNoContent());
  }

  @Test void expiredLinksAreRejected() throws Exception {
    tokens.tokenFor("reset-late@example.com", Role.CUSTOMER);
    forgot("reset-late@example.com", "10.3.0.4").andExpect(status().isAccepted());
    String token = emailedToken("reset-late@example.com");
    for (PasswordResetToken stored : resetTokens.findAll()) {
      if (stored.getUser().getEmail().equals("reset-late@example.com")) { stored.setExpiresAt(Instant.now().minusSeconds(1)); resetTokens.save(stored); }
    }

    reset(token, "Too-Late-Pass-1").andExpect(status().isBadRequest());
  }

  @Test void unknownEmailsGetTheSameAnswerAndNoEmail() throws Exception {
    tokens.tokenFor("reset-known@example.com", Role.CUSTOMER);
    String known = forgot("reset-known@example.com", "10.3.0.5")
      .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
    String unknown = forgot("nobody-here@example.com", "10.3.0.5")
      .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

    assertEquals(known, unknown, "the response must not reveal whether the account exists");
    verify(mailer, never()).send(argThat((User u) -> u.getEmail().equals("nobody-here@example.com")), anyString(), anyInt());
  }

  @Test void tokensAreStoredOnlyAsHashes() throws Exception {
    tokens.tokenFor("reset-hash@example.com", Role.CUSTOMER);
    forgot("reset-hash@example.com", "10.3.0.6").andExpect(status().isAccepted());
    String token = emailedToken("reset-hash@example.com");
    assertTrue(resetTokens.findAll().stream().noneMatch(t -> t.getTokenHash().equals(token)), "the raw token must never be stored");
  }

  @Test void requestsAreRateLimited() throws Exception {
    for (int i = 0; i < 3; i++) forgot("flood-target@example.com", "10.3.1." + i).andExpect(status().isAccepted());
    forgot("flood-target@example.com", "10.3.1.9").andExpect(status().isTooManyRequests());
  }

  @Test void garbageTokensAreRejected() throws Exception {
    reset("not-a-real-token", "Whatever-Pass-1").andExpect(status().isBadRequest());
  }
}
