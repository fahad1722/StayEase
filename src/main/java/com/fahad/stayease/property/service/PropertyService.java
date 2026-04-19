package com.fahad.stayease.property.service;

import com.fahad.stayease.booking.repository.BookingRepository;
import com.fahad.stayease.exception.ResourceNotFoundException;
import com.fahad.stayease.exception.UnauthorizedActionException;
import com.fahad.stayease.property.dto.PropertyRequest;
import com.fahad.stayease.property.dto.PropertyResponse;
import com.fahad.stayease.property.model.Property;
import com.fahad.stayease.property.repository.PropertyRepository;
import com.fahad.stayease.user.model.Role;
import com.fahad.stayease.user.model.User;
import com.fahad.stayease.user.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final UserService userService;

    @Transactional
    public PropertyResponse createProperty(String ownerEmail, PropertyRequest request) {
        log.info("Create property request by ownerEmail={} city={}", ownerEmail, request.getCity());
        User owner = userService.getByEmail(ownerEmail);
        assertOwnerRole(owner);

        Property property = new Property();
        mapRequest(property, request);
        property.setOwner(owner);

        Property saved = propertyRepository.save(property);
        log.info("Property created propertyId={} ownerId={}", saved.getId(), owner.getId());
        return toResponse(saved);
    }

    @Transactional
    public PropertyResponse updateProperty(Long propertyId, String ownerEmail, PropertyRequest request) {
        log.info("Update property request propertyId={} ownerEmail={}", propertyId, ownerEmail);
        User owner = userService.getByEmail(ownerEmail);
        assertOwnerRole(owner);

        Property property = getEntityById(propertyId);
        if (!property.getOwner().getId().equals(owner.getId())) {
            throw new UnauthorizedActionException("You can only edit your own properties");
        }

        mapRequest(property, request);
        Property saved = propertyRepository.save(property);
        log.info("Property updated propertyId={} ownerId={}", saved.getId(), owner.getId());
        return toResponse(saved);
    }

    @Transactional
    public void deleteProperty(Long propertyId, String ownerEmail) {
        log.info("Delete property request propertyId={} ownerEmail={}", propertyId, ownerEmail);
        User owner = userService.getByEmail(ownerEmail);
        assertOwnerRole(owner);

        Property property = getEntityById(propertyId);
        if (!property.getOwner().getId().equals(owner.getId())) {
            throw new UnauthorizedActionException("You can only delete your own properties");
        }

        propertyRepository.delete(property);
        log.info("Property deleted propertyId={} ownerId={}", propertyId, owner.getId());
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> getProperties(
            String city,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            LocalDate checkIn,
            LocalDate checkOut,
            Integer guests
    ) {
        log.info("Search properties city={} minPrice={} maxPrice={} checkIn={} checkOut={} guests={}",
                city, minPrice, maxPrice, checkIn, checkOut, guests);
        if ((checkIn != null && checkOut == null) || (checkIn == null && checkOut != null)) {
            throw new IllegalArgumentException("Both checkIn and checkOut are required together");
        }
        if (checkIn != null && !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }

        Specification<Property> spec = Specification.where(null);

        if (city != null && !city.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("city")), city.toLowerCase()));
        }
        if (minPrice != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("pricePerNight"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("pricePerNight"), maxPrice));
        }
        if (guests != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("maxGuests"), guests));
        }

        List<Property> properties = propertyRepository.findAll(spec);

        if (checkIn != null) {
            properties = properties.stream()
                    .filter(p -> !bookingRepository.existsOverlappingBookings(p.getId(), checkIn, checkOut))
                    .toList();
        }

        List<PropertyResponse> responses = properties.stream().map(this::toResponse).toList();
        log.info("Search properties resultCount={}", responses.size());
        return responses;
    }

    @Transactional(readOnly = true)
    public PropertyResponse getPropertyById(Long id) {
        log.info("Get property by id={}", id);
        return toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> getMyListings(String ownerEmail) {
        log.info("Get my listings for ownerEmail={}", ownerEmail);
        User owner = userService.getByEmail(ownerEmail);
        assertOwnerRole(owner);

        return propertyRepository.findByOwnerId(owner.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Property getEntityById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
    }

    private void assertOwnerRole(User user) {
        if (user.getRole() != Role.OWNER) {
            throw new UnauthorizedActionException("Only OWNER users can manage properties");
        }
    }

    private void mapRequest(Property property, PropertyRequest request) {
        property.setTitle(request.getTitle());
        property.setDescription(request.getDescription());
        property.setCity(request.getCity());
        property.setPricePerNight(request.getPricePerNight());
        property.setMaxGuests(request.getMaxGuests());
    }

    private PropertyResponse toResponse(Property property) {
        return PropertyResponse.builder()
                .id(property.getId())
                .title(property.getTitle())
                .description(property.getDescription())
                .city(property.getCity())
                .pricePerNight(property.getPricePerNight())
                .maxGuests(property.getMaxGuests())
                .ownerId(property.getOwner().getId())
                .ownerName(property.getOwner().getName())
                .createdAt(property.getCreatedAt())
                .build();
    }
}
