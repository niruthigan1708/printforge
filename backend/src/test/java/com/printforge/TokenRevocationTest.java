package com.printforge;

import com.printforge.entity.Role;
import com.printforge.entity.User;
import com.printforge.repository.UserRepository;
import com.printforge.support.TestTokens;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A token is only as good as the account behind it right now. */
@SpringBootTest @AutoConfigureMockMvc
class TokenRevocationTest {
  @Autowired MockMvc mvc;
  @Autowired TestTokens tokens;
  @Autowired UserRepository users;
  @Value("${JWT_SECRET}") String secret;

  @Test void logoutRevokesEveryTokenTheUserHolds() throws Exception {
    String phone = tokens.tokenFor("revoke-me@example.com", Role.CUSTOMER);
    String laptop = tokens.tokenFor("revoke-me@example.com", Role.CUSTOMER);
    mvc.perform(get("/api/orders").header("Authorization", "Bearer " + phone)).andExpect(status().isOk());

    mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + laptop)).andExpect(status().isNoContent());

    mvc.perform(get("/api/orders").header("Authorization", "Bearer " + phone)).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/orders").header("Authorization", "Bearer " + laptop)).andExpect(status().isUnauthorized());
    // Logging in again issues a token that works.
    mvc.perform(get("/api/orders").header("Authorization", "Bearer " + tokens.tokenFor("revoke-me@example.com", Role.CUSTOMER))).andExpect(status().isOk());
  }

  @Test void logoutRequiresASession() throws Exception {
    mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
  }

  @Test void roleIsReadFromTheAccountNotTheToken() throws Exception {
    String token = tokens.tokenFor("demoted-admin@example.com", Role.ADMIN);
    mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

    User user = users.findByEmailIgnoreCase("demoted-admin@example.com").orElseThrow();
    user.setRole(Role.CUSTOMER);
    users.save(user);

    mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
  }

  @Test void deletedUsersTokensStopWorking() throws Exception {
    String token = tokens.tokenFor("deleted-user@example.com", Role.CUSTOMER);
    users.delete(users.findByEmailIgnoreCase("deleted-user@example.com").orElseThrow());

    mvc.perform(get("/api/orders").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
  }

  @Test void tokensWithoutAVersionAreRejected() throws Exception {
    tokens.tokenFor("legacy-token@example.com", Role.ADMIN);
    // Shape of tokens issued before versioning: a role claim and no version claim.
    String legacy = Jwts.builder().subject("legacy-token@example.com").claim("role", "ADMIN")
      .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
      .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();

    mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + legacy)).andExpect(status().isUnauthorized());
  }
}
