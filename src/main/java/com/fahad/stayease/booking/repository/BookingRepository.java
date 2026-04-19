package com.fahad.stayease.booking.repository;

import com.fahad.stayease.booking.model.Booking;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.property.id = :propertyId
            AND b.status <> 'CANCELLED'
            AND b.checkIn < :checkOut
            AND b.checkOut > :checkIn
            """)
    boolean existsOverlappingBookings(
            @Param("propertyId") Long propertyId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    List<Booking> findByRenterIdOrderByCreatedAtDesc(Long renterId);

    List<Booking> findByPropertyIdOrderByCreatedAtDesc(Long propertyId);

    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.property.id = :propertyId
            AND b.renter.id = :renterId
            AND b.status = 'CONFIRMED'
            AND b.checkOut <= :today
            """)
    boolean existsCompletedBookingForReview(
            @Param("propertyId") Long propertyId,
            @Param("renterId") Long renterId,
            @Param("today") LocalDate today
    );
}
