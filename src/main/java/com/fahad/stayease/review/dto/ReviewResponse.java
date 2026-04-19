package com.fahad.stayease.review.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewResponse {
    private Long id;
    private Integer rating;
    private String comment;
    private Long propertyId;
    private Long renterId;
    private String renterName;
    private LocalDateTime createdAt;
}
