package com.printforge.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

import static com.printforge.dto.FieldLimits.*;

public record QuoteRequest(
  @NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") BigDecimal amount,
  @Size(max = LONG_TEXT) String notes) { }
