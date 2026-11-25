package com.printforge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/** Shop-wide pricing settings, shared by order pricing and the public settings endpoint. */
@Component
public class ShopSettings {
  private final BigDecimal deliveryFee;
  /** Subtotal at or above which delivery is free; null when free delivery is switched off. */
  private final BigDecimal freeDeliveryThreshold;

  public ShopSettings(@Value("${app.delivery-fee:350}") BigDecimal deliveryFee,
                      @Value("${app.free-delivery-threshold:5000}") String freeDeliveryThreshold) {
    if (deliveryFee.signum() < 0) throw new IllegalStateException("app.delivery-fee must not be negative");
    this.deliveryFee = deliveryFee;
    BigDecimal threshold = freeDeliveryThreshold == null || freeDeliveryThreshold.isBlank() ? null : new BigDecimal(freeDeliveryThreshold.trim());
    this.freeDeliveryThreshold = threshold == null || threshold.signum() <= 0 ? null : threshold;
  }

  public BigDecimal deliveryFee() { return deliveryFee; }
  public BigDecimal freeDeliveryThreshold() { return freeDeliveryThreshold; }

  /** The delivery fee actually charged for an order with this subtotal. */
  public BigDecimal deliveryFeeFor(BigDecimal subtotal) {
    if (freeDeliveryThreshold != null && subtotal.compareTo(freeDeliveryThreshold) >= 0) return BigDecimal.ZERO;
    return deliveryFee;
  }
}
