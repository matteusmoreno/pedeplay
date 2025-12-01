package br.com.matteusmoreno.domain.customer.request;

import org.bson.types.ObjectId;

public record UpdateCustomerRequest(
    ObjectId id,
    String name,
    String phoneNumber,
    String email,
    String documentNumber
) {

}
