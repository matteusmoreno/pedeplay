package br.com.matteusmoreno.domain.availability.request;

import br.com.matteusmoreno.domain.availability.constant.AvailabilityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateAvailabilityRequest(
    @NotBlank(message = "Artist ID is required")
    String artistId,
    @NotNull(message = "Start time is required")
    LocalDateTime startTime,
    @NotNull(message = "End time is required")
    LocalDateTime endTime,
    @NotNull(message = "Availability status is required")
    AvailabilityStatus availabilityStatus,
    @PositiveOrZero(message = "Price must be positive or zero")
    BigDecimal price) {}
