package com.printforge.support;

import com.printforge.entity.Role;
import com.printforge.entity.User;
import com.printforge.repository.UserRepository;
import com.printforge.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TestTokens {
  @Autowired private UserRepository users;
  @Autowired private PasswordEncoder encoder;
  @Autowired private JwtService jwt;

  public String tokenFor(String email, Role role) {
    User user = users.findByEmailIgnoreCase(email).orElseGet(() -> {
      User created = new User();
      created.setName(email);
      created.setEmail(email);
      created.setPassword(encoder.encode("Password1"));
      created.setRole(role);
      return users.save(created);
    });
    return jwt.create(user);
  }
}
