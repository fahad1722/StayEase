package com.fahad.stayease.review.controller;

import com.fahad.stayease.review.service.ReviewService;
import com.fahad.stayease.review.dto.ReviewRequest;
import com.fahad.stayease.review.dto.ReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Property review APIs")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Create review", description = "RENTER posts review after completed stay")
    @ApiResponse(responseCode = "200", description = "Review created",
            content = @Content(schema = @Schema(implementation = ReviewResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<ReviewResponse> createReview(
            Authentication authentication,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.createReview(authentication.getName(), request));
    }

    @GetMapping("/property/{id}")
    @Operation(summary = "Get reviews by property", description = "Public endpoint for property reviews")
    @ApiResponse(responseCode = "200", description = "Reviews fetched")
    public ResponseEntity<List<ReviewResponse>> getReviewsByProperty(
            @Parameter(description = "Property ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByProperty(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete review", description = "RENTER deletes own review")
    @ApiResponse(responseCode = "204", description = "Review deleted")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "Review ID") @PathVariable Long id,
            Authentication authentication
    ) {
        reviewService.deleteReview(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
