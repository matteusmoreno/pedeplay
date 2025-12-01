package br.com.matteusmoreno.domain.customer.service;

import static br.com.matteusmoreno.domain.customer.Customer.findByPhoneNumberOrEmailOrDocumentNumber;

import br.com.matteusmoreno.domain.customer.Customer;
import br.com.matteusmoreno.domain.customer.request.CreateCustomerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class CustomerService {

  public Customer createOrUpdateCustomer(CreateCustomerRequest request) {
    log.info("Creating or updating customer: {}", request);
    Customer customer = findByPhoneNumberOrEmailOrDocumentNumber(request.email(), request.phoneNumber(), request.documentNumber());
    if (customer != null) {
      log.info("Customer already exists: {}", customer);
      this.updateCustomer(customer, request);
      return customer;
    }

    log.info("Creating customer: {}", request);
    Customer newCustomer = Customer.builder()
        .name(request.name())
        .phoneNumber(request.phoneNumber())
        .email(request.email())
        .documentNumber(request.documentNumber())
        .createdAt(java.time.LocalDateTime.now())
        .build();
    newCustomer.persist();
    return newCustomer;
  }

  public Customer updateCustomer(Customer customer, CreateCustomerRequest request) {
    log.info("Updating customer: {}", request);
    if (request.name() != null) customer.name = request.name();
    if (request.phoneNumber() != null) customer.phoneNumber = request.phoneNumber();
    if (request.email() != null) customer.email = request.email();
    if (request.documentNumber() != null) customer.documentNumber = request.documentNumber();
    customer.updatedAt = LocalDateTime.now();
    customer.update();
    return customer;
  }

}
