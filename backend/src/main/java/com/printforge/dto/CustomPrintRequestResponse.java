package com.printforge.dto;

import com.printforge.entity.CustomPrintRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CustomPrintRequestResponse(Long id, String requestNumber, String fileName, String fileType, String material, String color, int quantity, String notes, String status, BigDecimal adminQuote, String adminNotes, Instant createdAt, Instant updatedAt, String customerName, String customerEmail, List<String> nextStatuses, boolean canQuote, boolean canCancel, boolean fileAvailable) {
  public static CustomPrintRequestResponse from(CustomPrintRequest request) {
    return new CustomPrintRequestResponse(request.getId(), request.getRequestNumber(), request.getFileName(), request.getFileType(), request.getMaterial(), request.getColor(), request.getQuantity(), request.getNotes(), request.getStatus().name(), request.getAdminQuote(), request.getAdminNotes(), request.getCreatedAt(), request.getUpdatedAt(), request.getCustomer().getName(), request.getCustomer().getEmail(), request.getStatus().adminNextStatuses().stream().map(Enum::name).toList(), request.getStatus().canBeQuoted(), request.getStatus().customerCanCancel(), request.getFilePurgedAt() == null);
  }
}
