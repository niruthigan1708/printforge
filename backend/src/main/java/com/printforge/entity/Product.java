package com.printforge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Getter @Setter @NoArgsConstructor
public class Product {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @NotBlank private String name;
  @NotBlank @Column(length = 2000) private String description;
  @DecimalMin("0.01") @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
  @Min(0) private int stockQuantity;
  @NotBlank private String material;
  @NotBlank private String color;
  @Column(columnDefinition = "text") private String imageUrl;
  private boolean active = true;
  private Instant createdAt = Instant.now();
  private Instant updatedAt = Instant.now();
  @ManyToOne(optional = false) private Category category;
}
