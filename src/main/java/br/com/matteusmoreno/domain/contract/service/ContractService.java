package br.com.matteusmoreno.domain.contract.service;

import br.com.matteusmoreno.domain.artist.Artist;
import br.com.matteusmoreno.domain.artist.response.ArtistDetailsResponse;
import br.com.matteusmoreno.domain.artist.service.ArtistService;
import br.com.matteusmoreno.domain.availability.Availability;
import br.com.matteusmoreno.domain.availability.constant.AvailabilityStatus;
import br.com.matteusmoreno.domain.availability.service.AvailabilityService;
import br.com.matteusmoreno.domain.contract.Contract;
import br.com.matteusmoreno.domain.contract.constant.ContractStatus;
import br.com.matteusmoreno.domain.contract.request.CreateContractRequest;
import br.com.matteusmoreno.domain.contract.response.ContractDetailsResponse;
import br.com.matteusmoreno.domain.customer.Customer;
import br.com.matteusmoreno.domain.customer.request.CreateCustomerRequest;
import br.com.matteusmoreno.domain.customer.response.CustomerDetailsResponse;
import br.com.matteusmoreno.domain.customer.service.CustomerService;
import br.com.matteusmoreno.exception.AvailabilityNotAvailableException;
import br.com.matteusmoreno.exception.AvailabilityTimeOverlapException;
import br.com.matteusmoreno.exception.ContractNotFoundException;
import br.com.matteusmoreno.exception.InvalidContractException;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
@ApplicationScoped
@Slf4j
public class ContractService {

  private final AvailabilityService availabilityService;
  private final CustomerService customerService;
  private final ArtistService artistService;

  public ContractService(AvailabilityService availabilityService, CustomerService customerService, ArtistService artistService) {
    this.availabilityService = availabilityService;
    this.customerService = customerService;
    this.artistService = artistService;
  }

  public ContractDetailsResponse createContract(CreateContractRequest contractRequest, CreateCustomerRequest customerRequest) {
    Customer customer = this.customerService.createOrUpdateCustomer(customerRequest);
    List<Availability> availabilities = contractRequest.availabilityIds().stream()
        .map(availabilityService::findAvailabilityById)
        .toList();

    validateAvailabilities(availabilities);
    ObjectId artistId = availabilities.getFirst().artistId;
    BigDecimal totalPrice = this.calculateTotalPrice(availabilities);

    List<ObjectId> availabilityObjectIds = contractRequest.availabilityIds().stream()
        .map(ObjectId::new)
        .toList();

    Contract contract = Contract.builder()
        .artistId(artistId)
        .customerId((ObjectId) customer.id)
        .availabilityIds(availabilityObjectIds)
        .totalPrice(totalPrice)
        .contractStatus(ContractStatus.PENDING_CONFIRMATION)
        .createdAt(LocalDateTime.now())
        .build();

    availabilities.forEach(availability ->
        availabilityService.changeAvailabilityStatus(availability.id.toString(), AvailabilityStatus.UNAVAILABLE));
    contract.persist();

    Artist artist = this.artistService.getArtistById(artistId);
    return new ContractDetailsResponse(contract, new ArtistDetailsResponse(artist), new CustomerDetailsResponse(customer));
  }

  public ContractDetailsResponse findContractById(String contractId) {
    log.info("Finding contract by id: {}", contractId);
    Contract contract = Contract.findById(new ObjectId(contractId));
    if (contract == null) {
      throw new ContractNotFoundException(contractId);
    }
    Artist artist = artistService.getArtistById(contract.artistId);
    ArtistDetailsResponse artistResponse = new ArtistDetailsResponse(artist);
    Customer customer = Customer.findById(contract.customerId);
    CustomerDetailsResponse customerResponse = new CustomerDetailsResponse(customer);
    return new ContractDetailsResponse(contract, artistResponse, customerResponse);
  }

  public List<ContractDetailsResponse> findAllContractsByAuthenticatedArtist(String artistId) {
    log.info("Finding all contracts for artistId: {}", artistId);
    ObjectId artistObjectId = new ObjectId(artistId);
    List<Contract> contracts = Contract.findAllByArtistId(artistObjectId);
    Artist artist = artistService.getArtistById(artistObjectId);
    ArtistDetailsResponse artistResponse = new ArtistDetailsResponse(artist);
    return contracts.stream()
        .map(contract -> {
          Customer customer = Customer.findById(contract.customerId);
          CustomerDetailsResponse customerResponse = new CustomerDetailsResponse(customer);
          return new ContractDetailsResponse(contract, artistResponse, customerResponse);
        })
        .toList();
  }

  public List<ContractDetailsResponse> findContractsByCustomer(String email, String phoneNumber) {
    log.info("Finding contracts for customer with email: {} or phone: {}", email, phoneNumber);
    Customer customer = Customer.findByPhoneNumberOrEmailOrDocumentNumber(phoneNumber, email, null);
    if (customer == null) {
      return List.of();
    }
    List<Contract> contracts = Contract.list("customerId", customer.id);
    return contracts.stream()
        .map(contract -> {
          Artist artist = artistService.getArtistById(contract.artistId);
          ArtistDetailsResponse artistResponse = new ArtistDetailsResponse(artist);
          CustomerDetailsResponse customerResponse = new CustomerDetailsResponse(customer);
          return new ContractDetailsResponse(contract, artistResponse, customerResponse);
        })
        .toList();
  }

  public void confirmContract(String contractId) {
    log.info("Confirming contract with id: {}", contractId);
    Contract contract = Contract.findById(new ObjectId(contractId));
    if (contract == null) {
      throw new ContractNotFoundException(contractId);
    }
    contract.availabilityIds.forEach(availId ->
        availabilityService.changeAvailabilityStatus(availId.toString(), AvailabilityStatus.BOOKED));
    contract.contractStatus = ContractStatus.CONFIRMED;
    contract.confirmedAt = LocalDateTime.now();
    contract.update();
  }

  public void rejectContract(String contractId) {
    log.info("Rejecting contract with id: {}", contractId);
    Contract contract = Contract.findById(new ObjectId(contractId));
    if (contract == null) {
      throw new ContractNotFoundException(contractId);
    }
    contract.availabilityIds.forEach(availId ->
        availabilityService.changeAvailabilityStatus(availId.toString(), AvailabilityStatus.AVAILABLE));
    contract.contractStatus = ContractStatus.REJECTED;
    contract.rejectedAt = LocalDateTime.now();
    contract.update();
  }

  public void cancelContract(String contractId) {
    log.info("Canceling contract with id: {}", contractId);
    Contract contract = Contract.findById(new ObjectId(contractId));
    if (contract == null) {
      throw new ContractNotFoundException(contractId);
    }
    contract.availabilityIds.forEach(availId ->
        availabilityService.changeAvailabilityStatus(availId.toString(), AvailabilityStatus.AVAILABLE));
    contract.contractStatus = ContractStatus.CANCELED;
    contract.canceledAt = LocalDateTime.now();
    contract.update();
  }

  public void completeContract(String contractId) {
    log.info("Completing contract with id: {}", contractId);
    Contract contract = Contract.findById(new ObjectId(contractId));
    if (contract == null) {
      throw new ContractNotFoundException(contractId);
    }
    contract.contractStatus = ContractStatus.COMPLETED;
    contract.completedAt = LocalDateTime.now();
    contract.update();
  }

  private void validateAvailabilities(List<Availability> availabilities) {
    if (availabilities.isEmpty()) {
      throw new InvalidContractException("At least one availability is required");
    }
    for (Availability availability : availabilities) {
      if (availability.availabilityStatus != AvailabilityStatus.AVAILABLE) {
        throw new AvailabilityNotAvailableException(availability.id.toString());
      }
    }
    ObjectId firstArtistId = availabilities.getFirst().artistId;
    boolean allSameArtist = availabilities.stream()
        .allMatch(av -> av.artistId.equals(firstArtistId));
    if (!allSameArtist) {
      throw new InvalidContractException("All availabilities must belong to the same artist");
    }
    validateTimeOverlap(availabilities);
  }
  private void validateTimeOverlap(List<Availability> availabilities) {
    for (int i = 0; i < availabilities.size(); i++) {
      for (int j = i + 1; j < availabilities.size(); j++) {
        Availability av1 = availabilities.get(i);
        Availability av2 = availabilities.get(j);
        boolean overlaps = av1.startTime.isBefore(av2.endTime) && av2.startTime.isBefore(av1.endTime);
        if (overlaps) {
          throw new AvailabilityTimeOverlapException();
        }
      }
    }
  }

  protected BigDecimal calculateTotalPrice(List<Availability> availabilities) {
    BigDecimal totalPrice = BigDecimal.ZERO;
    for (Availability availability : availabilities) {
      totalPrice = totalPrice.add(availability.price);
    }
    return totalPrice;
  }
}
