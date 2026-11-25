package com.printforge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.printforge.dto.FieldLimits.*;

public record LoginRequest(@Email @NotBlank @Size(max = EMAIL) String email, @NotBlank @Size(max = 200) String password) { }
