package com.printforge.dto;

import com.printforge.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.printforge.dto.FieldLimits.*;

/**
 * @param trackingNumber required when moving to SHIPPED; ignored otherwise
 * @param note           optional comment kept in the order's history
 */
public record OrderStatusUpdateRequest(
  @NotNull OrderStatus status,
  @Size(max = TRACKING_NUMBER) String trackingNumber,
  @Size(max = LONG_TEXT) String note) { }
