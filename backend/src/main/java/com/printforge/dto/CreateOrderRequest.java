package com.printforge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * @param expectedTotal optional: the total the customer was shown. If prices or the delivery fee
 *   changed since then, the order is rejected (409) rather than charging a different amount.
 */
public record CreateOrderRequest(@NotEmpty List<@Valid Item> items, @NotBlank String shippingName, @NotBlank String shippingPhone, @NotBlank String shippingAddress, @NotBlank String shippingCity, @NotBlank String shippingPostalCode, @DecimalMin("0") BigDecimal expectedTotal) {
  public record Item(@NotNull Long productId, @Min(1) int quantity) { }

  /** An order without a shown total to check against. */
  public CreateOrderRequest(List<Item> items, String shippingName, String shippingPhone, String shippingAddress, String shippingCity, String shippingPostalCode) {
    this(items, shippingName, shippingPhone, shippingAddress, shippingCity, shippingPostalCode, null);
  }
}
