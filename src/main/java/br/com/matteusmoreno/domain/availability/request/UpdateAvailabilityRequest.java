package br.com.matteusmoreno.domain.availability.request;

import br.com.matteusmoreno.domain.availability.constant.AvailabilityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateAvailabilityRequest(
    @NotBlank(message = "Availability ID is required")
    String id,
    @NotNull(message = "Start time is required")
    LocalDateTime startTime,
    @NotNull(message = "End time is required")
    LocalDateTime endTime,
    AvailabilityStatus availabilityStatus,
    @PositiveOrZero(message = "Price must be positive or zero")
    BigDecimal price
) {

}
