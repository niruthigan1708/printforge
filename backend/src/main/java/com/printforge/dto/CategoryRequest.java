package com.printforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.printforge.dto.FieldLimits.*;

public record CategoryRequest(@NotBlank @Size(max = CATEGORY_NAME) String name, @Size(max = CATEGORY_DESCRIPTION) String description) { }
