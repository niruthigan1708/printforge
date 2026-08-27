package com.printforge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreateOrderRequest(@NotEmpty List<@Valid Item> items, @NotBlank String shippingName, @NotBlank String shippingPhone, @NotBlank String shippingAddress, @NotBlank String shippingCity, @NotBlank String shippingPostalCode) {
  public record Item(@NotNull Long productId, @Min(1) int quantity) { }
}
