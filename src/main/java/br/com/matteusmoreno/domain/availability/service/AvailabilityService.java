package br.com.matteusmoreno.domain.availability.service;
import br.com.matteusmoreno.domain.availability.Availability;
import br.com.matteusmoreno.domain.availability.constant.AvailabilityStatus;
import br.com.matteusmoreno.domain.availability.request.CreateAvailabilityRequest;
import br.com.matteusmoreno.domain.availability.request.UpdateAvailabilityRequest;
import br.com.matteusmoreno.domain.availability.response.AvailabilityDetailsResponse;
import br.com.matteusmoreno.exception.AvailabilityNotFoundException;
import br.com.matteusmoreno.exception.InvalidTimeRangeException;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
@ApplicationScoped
@Slf4j
public class AvailabilityService {
  public Availability createAvailability(CreateAvailabilityRequest request){
    log.info("Creating availability: {}", request);
    this.validateTime(request.startTime(), request.endTime());
    Availability availability = Availability.builder()
        .artistId(new ObjectId(request.artistId()))
        .startTime(request.startTime())
        .endTime(request.endTime())
        .availabilityStatus(request.availabilityStatus())
        .price(request.price())
        .build();
    availability.persist();
    return availability;
  }
  public Availability findAvailabilityById(String id) {
      log.info("Finding availability by id: {}", id);
      Availability availability = Availability.findAvailabilityById(id);
      if (availability == null) {
          throw new AvailabilityNotFoundException(id);
      }
      return availability;
  }
  public List<AvailabilityDetailsResponse> findAllAvailabilitiesByArtist(String artistId) {
    log.info("Finding all availabilities for artistId: {}", artistId);
    List<Availability> availabilities = Availability.findAllAvailabilitiesByArtist(new ObjectId(artistId));
    return availabilities.stream()
        .map(AvailabilityDetailsResponse::new)
        .toList();
  }
  public List<AvailabilityDetailsResponse> findAllAvailableAvailabilities() {
    log.info("Finding all available availabilities");
    List<Availability> availabilities = Availability.list("availabilityStatus", AvailabilityStatus.AVAILABLE);
    return availabilities.stream()
        .map(AvailabilityDetailsResponse::new)
        .toList();
  }
  public List<AvailabilityDetailsResponse> findAvailableByArtist(String artistId) {
    log.info("Finding available availabilities for artistId: {}", artistId);
    List<Availability> availabilities = Availability.list("artistId = ?1 and availabilityStatus = ?2", 
        new ObjectId(artistId), AvailabilityStatus.AVAILABLE);
    return availabilities.stream()
        .map(AvailabilityDetailsResponse::new)
        .toList();
  }
  public Availability updateAvailability(UpdateAvailabilityRequest request) {
    log.info("Updating availability: {}", request);
    Availability availability = Availability.findAvailabilityById(request.id());
    if (request.startTime() != null && request.endTime() != null) {
      validateTime(request.startTime(), request.endTime());
    } else if (request.startTime() != null) {
      validateTime(request.startTime(), availability.endTime);
    } else if (request.endTime() != null) {
      validateTime(availability.startTime, request.endTime());
    }
    if (request.startTime() != null) availability.startTime = request.startTime();
    if (request.endTime() != null) availability.endTime = request.endTime();
    if (request.availabilityStatus() != null) availability.availabilityStatus = request.availabilityStatus();
    if (request.price() != null) availability.price = request.price();
    availability.update();
    return availability;
  }
  public void deleteAvailabilityById(String id) {
    log.info("Deleting availability by id: {}", id);
    Availability availability = this.findAvailabilityById(id);
    if (availability.availabilityStatus.equals(AvailabilityStatus.BOOKED)) {
      throw new IllegalStateException("Cannot delete a booked availability");
    }
    availability.delete();
  }
  public void changeAvailabilityStatus(String id, AvailabilityStatus status) {
    log.info("Changing availability status for id: {} to status: {}", id, status);
    Availability availability = this.findAvailabilityById(id);
    availability.availabilityStatus = status;
    availability.update();
  }
  protected void validateTime(LocalDateTime startTime, LocalDateTime endTime) {
    if (endTime.isBefore(startTime)) throw new InvalidTimeRangeException("End time cannot be before start time");
    if (startTime.isBefore(LocalDateTime.now())) throw new InvalidTimeRangeException("Availability start time must be in the future");
  }
}
