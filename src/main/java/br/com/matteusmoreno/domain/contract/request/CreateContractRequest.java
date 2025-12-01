package br.com.matteusmoreno.domain.contract.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateContractRequest(
    @NotEmpty(message = "At least one Availability ID is required")
    List<String> availabilityIds
) {

}
