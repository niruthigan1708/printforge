package com.printforge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.printforge.dto.FieldLimits.EMAIL;

public record ForgotPasswordRequest(@Email @NotBlank @Size(max = EMAIL) String email) { }
