package com.printforge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.printforge.dto.FieldLimits.*;

public record AuthRequest(
  @NotBlank @Size(max = PERSON_NAME) String name,
  @Email @NotBlank @Size(max = EMAIL) String email,
  @NotNull @Size(min = 8, max = PASSWORD_MAX) String password) { }
