package br.com.matteusmoreno.infrastructure.viacep;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "viacep-api")
public interface ViaCepClient {

    @GET
    @Path("/{cep}/json")
    ViaCepResponse getAddressByCep(@PathParam("cep") String cep);
}