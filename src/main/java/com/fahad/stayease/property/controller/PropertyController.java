package com.fahad.stayease.property.controller;

import com.fahad.stayease.property.service.PropertyService;
import com.fahad.stayease.property.dto.PropertyRequest;
import com.fahad.stayease.property.dto.PropertyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
@Tag(name = "Properties", description = "Property listing and management APIs")
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    @Operation(summary = "Create property", description = "OWNER creates a new property listing")
    @ApiResponse(responseCode = "200", description = "Property created",
            content = @Content(schema = @Schema(implementation = PropertyResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<PropertyResponse> createProperty(
            Authentication authentication,
            @Valid @RequestBody PropertyRequest request
    ) {
        return ResponseEntity.ok(propertyService.createProperty(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update property", description = "OWNER updates own property")
    @ApiResponse(responseCode = "200", description = "Property updated",
            content = @Content(schema = @Schema(implementation = PropertyResponse.class)))
    @ApiResponse(responseCode = "404", description = "Property not found")
    public ResponseEntity<PropertyResponse> updateProperty(
            @Parameter(description = "Property ID") @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody PropertyRequest request
    ) {
        return ResponseEntity.ok(propertyService.updateProperty(id, authentication.getName(), request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete property", description = "OWNER deletes own property")
    @ApiResponse(responseCode = "204", description = "Property deleted")
    public ResponseEntity<Void> deleteProperty(
            @Parameter(description = "Property ID") @PathVariable Long id,
            Authentication authentication
    ) {
        propertyService.deleteProperty(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Search properties", description = "Public search endpoint with filters")
    @ApiResponse(responseCode = "200", description = "Properties fetched")
    public ResponseEntity<List<PropertyResponse>> getProperties(
            @Parameter(description = "City name") @RequestParam(required = false) String city,
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Desired check-in date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @Parameter(description = "Desired check-out date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @Parameter(description = "Minimum guests capacity") @RequestParam(required = false) Integer guests
    ) {
        return ResponseEntity.ok(propertyService.getProperties(city, minPrice, maxPrice, checkIn, checkOut, guests));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get property by ID", description = "Public property details endpoint")
    @ApiResponse(responseCode = "200", description = "Property fetched",
            content = @Content(schema = @Schema(implementation = PropertyResponse.class)))
    @ApiResponse(responseCode = "404", description = "Property not found")
    public ResponseEntity<PropertyResponse> getPropertyById(
            @Parameter(description = "Property ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(propertyService.getPropertyById(id));
    }

    @GetMapping("/my-listings")
    @Operation(summary = "My listings", description = "OWNER gets own property listings")
    @ApiResponse(responseCode = "200", description = "Listings fetched")
    public ResponseEntity<List<PropertyResponse>> getMyListings(Authentication authentication) {
        return ResponseEntity.ok(propertyService.getMyListings(authentication.getName()));
    }
}
