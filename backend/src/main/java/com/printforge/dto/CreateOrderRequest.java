package com.printforge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

import static com.printforge.dto.FieldLimits.*;

/**
 * @param expectedTotal optional: the total the customer was shown. If prices or the delivery fee
 *   changed since then, the order is rejected (409) rather than charging a different amount.
 */
public record CreateOrderRequest(
  @NotEmpty @Size(max = MAX_ORDER_LINES) List<@Valid @NotNull Item> items,
  @NotBlank @Size(max = PERSON_NAME) String shippingName,
  @NotBlank @Size(max = PHONE) String shippingPhone,
  @NotBlank @Size(max = ADDRESS) String shippingAddress,
  @NotBlank @Size(max = CITY) String shippingCity,
  @NotBlank @Size(max = POSTAL_CODE) String shippingPostalCode,
  @DecimalMin("0") BigDecimal expectedTotal) {

  public record Item(@NotNull Long productId, @Min(1) @Max(MAX_QUANTITY) int quantity) { }

  /** An order without a shown total to check against. */
  public CreateOrderRequest(List<Item> items, String shippingName, String shippingPhone, String shippingAddress, String shippingCity, String shippingPostalCode) {
    this(items, shippingName, shippingPhone, shippingAddress, shippingCity, shippingPostalCode, null);
  }
}
