package com.printforge.controller;

import com.printforge.config.ShopSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;

/** Public, read-only shop settings the storefront needs to show correct totals. */
@RestController @RequestMapping("/api/shop/settings") @RequiredArgsConstructor
public class ShopSettingsController {
  private final ShopSettings settings;

  public record Response(BigDecimal deliveryFee) { }

  @GetMapping public Response get() { return new Response(settings.deliveryFee()); }
}
