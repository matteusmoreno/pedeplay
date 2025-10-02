package br.com.matteusmoreno.domain.address;

public record AddressDetailsResponse(

        String cep,
        String street,
        String complement,
        String neighborhood,
        String city,
        String state,
        String number)
{
    public AddressDetailsResponse(Address address) {
        this(
                address.cep,
                address.street,
                address.complement,
                address.neighborhood,
                address.city,
                address.state,
                address.number
        );
    }
}
