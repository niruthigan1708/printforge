package com.printforge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/** Shop-wide pricing settings, shared by order pricing and the public settings endpoint. */
@Component
public class ShopSettings {
  private final BigDecimal deliveryFee;

  public ShopSettings(@Value("${app.delivery-fee:350}") BigDecimal deliveryFee) {
    if (deliveryFee.signum() < 0) throw new IllegalStateException("app.delivery-fee must not be negative");
    this.deliveryFee = deliveryFee;
  }

  public BigDecimal deliveryFee() { return deliveryFee; }
}
