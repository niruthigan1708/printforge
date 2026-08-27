package com.printforge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @NotBlank private String name;
  @Email @NotBlank @Column(unique = true, nullable = false) private String email;
  @NotBlank private String password;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role = Role.CUSTOMER;
  private Instant createdAt = Instant.now();
}
