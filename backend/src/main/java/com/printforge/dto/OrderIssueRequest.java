package com.printforge.dto;

import jakarta.validation.constraints.Size;

import static com.printforge.dto.FieldLimits.*;

/** A customer's "I haven't received this" report; the note is optional. */
public record OrderIssueRequest(@Size(max = LONG_TEXT) String note) { }
