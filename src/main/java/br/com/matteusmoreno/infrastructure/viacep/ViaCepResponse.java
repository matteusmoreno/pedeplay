package br.com.matteusmoreno.infrastructure.viacep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // Ignora campos que não mapeamos
public record ViaCepResponse(
        String cep,
        String logradouro,
        String complemento,
        String bairro,
        String localidade,
        String uf,
        Boolean erro // ViaCEP retorna 'erro: true' se o CEP não for encontrado
) {}
