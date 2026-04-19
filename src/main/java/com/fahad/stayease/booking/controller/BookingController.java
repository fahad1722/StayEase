package com.fahad.stayease.booking.controller;

import com.fahad.stayease.booking.service.BookingService;
import com.fahad.stayease.booking.dto.BookingRequest;
import com.fahad.stayease.booking.dto.BookingResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking creation and lifecycle APIs")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create booking", description = "RENTER creates booking for a property")
    @ApiResponse(responseCode = "200", description = "Booking created",
            content = @Content(schema = @Schema(implementation = BookingResponse.class)))
    @ApiResponse(responseCode = "409", description = "Date overlap conflict")
    public ResponseEntity<BookingResponse> createBooking(
            Authentication authentication,
            @Valid @RequestBody BookingRequest request
    ) {
        return ResponseEntity.ok(bookingService.createBooking(authentication.getName(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID", description = "Visible to booking renter and property owner")
    @ApiResponse(responseCode = "200", description = "Booking fetched",
            content = @Content(schema = @Schema(implementation = BookingResponse.class)))
    @ApiResponse(responseCode = "404", description = "Booking not found")
    public ResponseEntity<BookingResponse> getBookingById(
            @Parameter(description = "Booking ID") @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(bookingService.getBookingById(id, authentication.getName()));
    }

    @GetMapping("/my-bookings")
    @Operation(summary = "My bookings", description = "RENTER gets own bookings")
    @ApiResponse(responseCode = "200", description = "Bookings fetched")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getMyBookings(authentication.getName()));
    }

    @GetMapping("/property/{id}")
    @Operation(summary = "Bookings by property", description = "OWNER gets bookings for own property")
    @ApiResponse(responseCode = "200", description = "Bookings fetched")
    public ResponseEntity<List<BookingResponse>> getBookingsForProperty(
            @Parameter(description = "Property ID") @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(bookingService.getBookingsForProperty(id, authentication.getName()));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking", description = "Both OWNER and RENTER can cancel")
    @ApiResponse(responseCode = "200", description = "Booking cancelled",
            content = @Content(schema = @Schema(implementation = BookingResponse.class)))
    public ResponseEntity<BookingResponse> cancelBooking(
            @Parameter(description = "Booking ID") @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, authentication.getName()));
    }

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirm booking", description = "OWNER confirms booking")
    @ApiResponse(responseCode = "200", description = "Booking confirmed",
            content = @Content(schema = @Schema(implementation = BookingResponse.class)))
    public ResponseEntity<BookingResponse> confirmBooking(
            @Parameter(description = "Booking ID") @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(bookingService.confirmBooking(id, authentication.getName()));
    }
}
