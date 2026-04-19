package com.fahad.stayease.property.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PropertyResponse {
    private Long id;
    private String title;
    private String description;
    private String city;
    private BigDecimal pricePerNight;
    private Integer maxGuests;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime createdAt;
}
