package br.com.matteusmoreno.domain.customer.response;

import br.com.matteusmoreno.domain.customer.Customer;
import java.time.LocalDateTime;

public record CustomerDetailsResponse(
    String name,
    String phoneNumber,
    String email,
    String documentNumber,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  public CustomerDetailsResponse(Customer customer) {
    this(
        customer.name,
        customer.phoneNumber,
        customer.email,
        customer.documentNumber,
        customer.createdAt,
        customer.updatedAt
    );
  }

}
