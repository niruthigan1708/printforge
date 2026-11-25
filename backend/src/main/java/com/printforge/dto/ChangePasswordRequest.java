package com.printforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.printforge.dto.FieldLimits.PASSWORD_MAX;

public record ChangePasswordRequest(@NotBlank @Size(max = 200) String currentPassword, @NotNull @Size(min = 8, max = PASSWORD_MAX) String newPassword) { }
