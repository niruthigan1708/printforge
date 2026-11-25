package com.printforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.printforge.dto.FieldLimits.PERSON_NAME;

/** The details a user may change themselves. Email and role are deliberately not editable here. */
public record UpdateAccountRequest(@NotBlank @Size(max = PERSON_NAME) String name) { }
