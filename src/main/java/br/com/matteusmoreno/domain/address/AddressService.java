package br.com.matteusmoreno.domain.address;

import br.com.matteusmoreno.infrastructure.viacep.ViaCepClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class AddressService {

    private final ViaCepClient viaCepClient;

    public AddressService(@RestClient ViaCepClient viaCepClient) {
        this.viaCepClient = viaCepClient;
    }

    public Address getAddressByCep(String cep) {
        var viaCepResponse = viaCepClient.getAddressByCep(cep);
        return Address.builder()
                .cep(viaCepResponse.cep())
                .street(viaCepResponse.logradouro())
                .complement(viaCepResponse.complemento())
                .neighborhood(viaCepResponse.bairro())
                .city(viaCepResponse.localidade())
                .state(viaCepResponse.uf())
                .build();
    }
}
