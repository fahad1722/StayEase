package com.fahad.stayease.review.service;

import com.fahad.stayease.booking.repository.BookingRepository;
import com.fahad.stayease.exception.ResourceNotFoundException;
import com.fahad.stayease.exception.UnauthorizedActionException;
import com.fahad.stayease.property.model.Property;
import com.fahad.stayease.property.service.PropertyService;
import com.fahad.stayease.review.dto.ReviewRequest;
import com.fahad.stayease.review.dto.ReviewResponse;
import com.fahad.stayease.review.model.Review;
import com.fahad.stayease.review.repository.ReviewRepository;
import com.fahad.stayease.user.model.Role;
import com.fahad.stayease.user.model.User;
import com.fahad.stayease.user.service.UserService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PropertyService propertyService;
    private final UserService userService;
    private final BookingRepository bookingRepository;

    @Transactional
    public ReviewResponse createReview(String renterEmail, ReviewRequest request) {
        log.info("Create review request renterEmail={} propertyId={} rating={}",
            renterEmail, request.getPropertyId(), request.getRating());
        User renter = userService.getByEmail(renterEmail);
        if (renter.getRole() != Role.RENTER) {
            throw new UnauthorizedActionException("Only RENTER users can post reviews");
        }

        Property property = propertyService.getEntityById(request.getPropertyId());

        boolean hasCompletedBooking = bookingRepository.existsCompletedBookingForReview(
                property.getId(),
                renter.getId(),
                LocalDate.now()
        );

        if (!hasCompletedBooking) {
            log.warn("Review rejected: no completed booking renterId={} propertyId={}", renter.getId(), property.getId());
            throw new UnauthorizedActionException("You can only review a property after a completed stay");
        }

        Review review = new Review();
        review.setProperty(property);
        review.setRenter(renter);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review saved = reviewRepository.save(review);
        log.info("Review created reviewId={} propertyId={} renterId={}",
            saved.getId(), property.getId(), renter.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProperty(Long propertyId) {
        log.info("Get reviews by property propertyId={}", propertyId);
        propertyService.getEntityById(propertyId);
        List<ReviewResponse> responses = reviewRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId)
                .stream()
                .map(this::toResponse)
                .toList();
        log.info("Get reviews resultCount={} propertyId={}", responses.size(), propertyId);
        return responses;
    }

    @Transactional
    public void deleteReview(Long reviewId, String renterEmail) {
        log.info("Delete review request reviewId={} renterEmail={}", reviewId, renterEmail);
        User renter = userService.getByEmail(renterEmail);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getRenter().getId().equals(renter.getId())) {
            throw new UnauthorizedActionException("You can only delete your own review");
        }

        reviewRepository.delete(review);
        log.info("Review deleted reviewId={} renterId={}", reviewId, renter.getId());
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .propertyId(review.getProperty().getId())
                .renterId(review.getRenter().getId())
                .renterName(review.getRenter().getName())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
