package com.printforge.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(@NotBlank String name, @NotBlank String description, @NotNull @DecimalMin("0.01") BigDecimal price, @Min(0) int stockQuantity, @NotBlank String material, @NotBlank String color, String imageUrl, @NotNull Long categoryId, Boolean active) { }
