package com.printforge.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * @param expectedStockQuantity on update, the stock value the editor was loaded with. When given,
 *   the server protects sales made while the form was open: an unchanged stock field keeps the
 *   current stock, and an edited one is rejected (409) if stock moved in the meantime. When
 *   omitted, {@code stockQuantity} overwrites the stock unconditionally. Ignored on create.
 */
public record ProductRequest(@NotBlank String name, @NotBlank String description, @NotNull @DecimalMin("0.01") BigDecimal price, @Min(0) int stockQuantity, @NotBlank String material, @NotBlank String color, String imageUrl, @NotNull Long categoryId, Boolean active, @Min(0) Integer expectedStockQuantity) { }
