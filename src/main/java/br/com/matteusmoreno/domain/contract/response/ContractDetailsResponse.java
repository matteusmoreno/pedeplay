package br.com.matteusmoreno.domain.contract.response;
import br.com.matteusmoreno.domain.artist.response.ArtistDetailsResponse;
import br.com.matteusmoreno.domain.contract.Contract;
import br.com.matteusmoreno.domain.contract.constant.ContractStatus;
import br.com.matteusmoreno.domain.customer.response.CustomerDetailsResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
public record ContractDetailsResponse(
    String id,
    ArtistDetailsResponse artist,
    CustomerDetailsResponse customer,
    List<String> availabilityIds,
    BigDecimal totalPrice,
    ContractStatus status,
    LocalDateTime createdAt) {
  public ContractDetailsResponse(Contract contract, ArtistDetailsResponse artist, CustomerDetailsResponse customer) {
    this(
        contract.id.toString(),
        artist,
        customer,
        contract.availabilityIds.stream().map(Object::toString).collect(Collectors.toList()),
        contract.totalPrice,
        contract.contractStatus,
        contract.createdAt);
  }
}
