package com.printforge.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

import static com.printforge.dto.FieldLimits.*;

/**
 * @param expectedStockQuantity on update, the stock value the editor was loaded with. When given,
 *   the server protects sales made while the form was open: an unchanged stock field keeps the
 *   current stock, and an edited one is rejected (409) if stock moved in the meantime. When
 *   omitted, {@code stockQuantity} overwrites the stock unconditionally. Ignored on create.
 */
public record ProductRequest(
  @NotBlank @Size(max = PRODUCT_NAME) String name,
  @NotBlank @Size(max = LONG_TEXT) String description,
  // price is NUMERIC(12,2)
  @NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") BigDecimal price,
  @Min(0) @Max(1_000_000) int stockQuantity,
  @NotBlank @Size(max = MATERIAL) String material,
  @NotBlank @Size(max = COLOR) String color,
  @Size(max = URL) String imageUrl,
  @NotNull Long categoryId,
  Boolean active,
  @Min(0) Integer expectedStockQuantity) { }
