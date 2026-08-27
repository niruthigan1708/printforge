package com.printforge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Getter @Setter @NoArgsConstructor
public class CustomPrintRequest {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(unique = true, nullable = false) private String requestNumber;
  @ManyToOne(optional = false) private User customer;
  @NotBlank private String fileName;
  @NotBlank private String filePath;
  @NotBlank private String fileType;
  @NotBlank private String material;
  @NotBlank private String color;
  private int quantity;
  @Column(length = 2000) private String notes;
  @Enumerated(EnumType.STRING) private RequestStatus status = RequestStatus.SUBMITTED;
  private BigDecimal adminQuote;
  @Column(length = 2000) private String adminNotes;
  private Instant createdAt = Instant.now();
  private Instant updatedAt = Instant.now();
}
