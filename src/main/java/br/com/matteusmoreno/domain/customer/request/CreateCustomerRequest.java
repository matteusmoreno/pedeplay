package br.com.matteusmoreno.domain.customer.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(
    @NotBlank(message = "Name is required")
    String name,
    @NotBlank(message = "Phone number is required")
    String phoneNumber,
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    @NotBlank(message = "Document number is required")
    String documentNumber
) {

}
