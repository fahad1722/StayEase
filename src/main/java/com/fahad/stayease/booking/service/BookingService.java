package com.fahad.stayease.booking.service;

import com.fahad.stayease.booking.dto.BookingRequest;
import com.fahad.stayease.booking.dto.BookingResponse;
import com.fahad.stayease.booking.model.Booking;
import com.fahad.stayease.booking.model.BookingStatus;
import com.fahad.stayease.booking.repository.BookingRepository;
import com.fahad.stayease.exception.BookingConflictException;
import com.fahad.stayease.exception.ResourceNotFoundException;
import com.fahad.stayease.exception.UnauthorizedActionException;
import com.fahad.stayease.property.model.Property;
import com.fahad.stayease.property.service.PropertyService;
import com.fahad.stayease.user.model.Role;
import com.fahad.stayease.user.model.User;
import com.fahad.stayease.user.service.UserService;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyService propertyService;
    private final UserService userService;

    @Transactional
    public BookingResponse createBooking(String renterEmail, BookingRequest request) {
        log.info("Create booking request renterEmail={} propertyId={} checkIn={} checkOut={}",
                renterEmail, request.getPropertyId(), request.getCheckIn(), request.getCheckOut());
        User renter = userService.getByEmail(renterEmail);
        if (renter.getRole() != Role.RENTER) {
            throw new UnauthorizedActionException("Only RENTER users can create bookings");
        }

        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }

        Property property = propertyService.getEntityById(request.getPropertyId());

        if (bookingRepository.existsOverlappingBookings(property.getId(), request.getCheckIn(), request.getCheckOut())) {
            log.warn("Booking conflict propertyId={} checkIn={} checkOut={}",
                    property.getId(), request.getCheckIn(), request.getCheckOut());
            throw new BookingConflictException("Property is already booked for the selected dates");
        }

        long numberOfDays = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        BigDecimal totalPrice = property.getPricePerNight().multiply(BigDecimal.valueOf(numberOfDays));

        Booking booking = new Booking();
        booking.setProperty(property);
        booking.setRenter(renter);
        booking.setCheckIn(request.getCheckIn());
        booking.setCheckOut(request.getCheckOut());
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(totalPrice);

        Booking saved = bookingRepository.save(booking);
        log.info("Booking created bookingId={} propertyId={} renterId={} totalPrice={}",
            saved.getId(), property.getId(), renter.getId(), saved.getTotalPrice());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId, String requesterEmail) {
        log.info("Get booking request bookingId={} requesterEmail={}", bookingId, requesterEmail);
        Booking booking = getEntityById(bookingId);
        User requester = userService.getByEmail(requesterEmail);

        boolean isRenter = booking.getRenter().getId().equals(requester.getId());
        boolean isOwner = booking.getProperty().getOwner().getId().equals(requester.getId());

        if (!isRenter && !isOwner) {
            throw new UnauthorizedActionException("You are not allowed to view this booking");
        }

        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String renterEmail) {
        log.info("Get my bookings renterEmail={}", renterEmail);
        User renter = userService.getByEmail(renterEmail);
        if (renter.getRole() != Role.RENTER) {
            throw new UnauthorizedActionException("Only RENTER users can view renter bookings");
        }

        return bookingRepository.findByRenterIdOrderByCreatedAtDesc(renter.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForProperty(Long propertyId, String ownerEmail) {
        log.info("Get bookings for property propertyId={} ownerEmail={}", propertyId, ownerEmail);
        User owner = userService.getByEmail(ownerEmail);
        Property property = propertyService.getEntityById(propertyId);

        if (!property.getOwner().getId().equals(owner.getId())) {
            throw new UnauthorizedActionException("You can only view bookings for your own properties");
        }

        return bookingRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, String requesterEmail) {
        log.info("Cancel booking request bookingId={} requesterEmail={}", bookingId, requesterEmail);
        Booking booking = getEntityById(bookingId);
        User requester = userService.getByEmail(requesterEmail);

        boolean isRenter = booking.getRenter().getId().equals(requester.getId());
        boolean isOwner = booking.getProperty().getOwner().getId().equals(requester.getId());

        if (!isRenter && !isOwner) {
            throw new UnauthorizedActionException("Only booking renter or property owner can cancel booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking cancelled bookingId={} byUserId={}", saved.getId(), requester.getId());
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse confirmBooking(Long bookingId, String ownerEmail) {
        log.info("Confirm booking request bookingId={} ownerEmail={}", bookingId, ownerEmail);
        Booking booking = getEntityById(bookingId);
        User owner = userService.getByEmail(ownerEmail);

        if (!booking.getProperty().getOwner().getId().equals(owner.getId())) {
            throw new UnauthorizedActionException("Only property owner can confirm booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled booking cannot be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking confirmed bookingId={} ownerId={}", saved.getId(), owner.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Booking getEntityById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .propertyId(booking.getProperty().getId())
                .propertyTitle(booking.getProperty().getTitle())
                .renterId(booking.getRenter().getId())
                .renterName(booking.getRenter().getName())
                .checkIn(booking.getCheckIn())
                .checkOut(booking.getCheckOut())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
