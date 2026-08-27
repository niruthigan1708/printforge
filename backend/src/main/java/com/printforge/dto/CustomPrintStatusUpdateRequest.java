package com.printforge.dto;

import jakarta.validation.constraints.NotNull;
import com.printforge.entity.RequestStatus;

public record CustomPrintStatusUpdateRequest(@NotNull RequestStatus status) { }
