package br.com.matteusmoreno.domain.address;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Address {

    public String cep;
    public String street;
    public String complement;
    public String neighborhood;
    public String city;
    public String state;
    public String number;
}
