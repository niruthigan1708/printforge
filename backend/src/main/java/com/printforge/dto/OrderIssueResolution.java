package com.printforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.printforge.dto.FieldLimits.*;

/** How the studio dealt with a non-delivery report; shown to the customer. */
public record OrderIssueResolution(@NotBlank @Size(max = LONG_TEXT) String resolution) { }
