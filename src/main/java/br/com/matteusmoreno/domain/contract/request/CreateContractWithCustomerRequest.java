package br.com.matteusmoreno.domain.contract.request;

import br.com.matteusmoreno.domain.customer.request.CreateCustomerRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateContractWithCustomerRequest(
    @NotNull(message = "Contract information is required")
    @Valid
    CreateContractRequest contract,
    @NotNull(message = "Customer information is required")
    @Valid
    CreateCustomerRequest customer
) {

}

