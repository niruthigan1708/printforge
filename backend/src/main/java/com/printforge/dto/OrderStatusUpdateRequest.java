package com.printforge.dto;

import jakarta.validation.constraints.NotNull;
import com.printforge.entity.OrderStatus;

public record OrderStatusUpdateRequest(@NotNull OrderStatus status) { }
