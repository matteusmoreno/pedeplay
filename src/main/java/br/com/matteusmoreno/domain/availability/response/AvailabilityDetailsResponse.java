package br.com.matteusmoreno.domain.availability.response;
import br.com.matteusmoreno.domain.availability.Availability;
import br.com.matteusmoreno.domain.availability.constant.AvailabilityStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record AvailabilityDetailsResponse(
    String id,
    String artistId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    AvailabilityStatus availabilityStatus,
    BigDecimal price
) {
  public AvailabilityDetailsResponse(Availability availability) {
    this(
        availability.id.toString(),
        availability.artistId.toString(),
        availability.startTime,
        availability.endTime,
        availability.availabilityStatus,
        availability.price
    );
  }
}
