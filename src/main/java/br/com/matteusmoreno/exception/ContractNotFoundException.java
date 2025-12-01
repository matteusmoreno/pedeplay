package br.com.matteusmoreno.exception;
public class ContractNotFoundException extends RuntimeException {
    public ContractNotFoundException(String contractId) {
        super("Contract with id " + contractId + " not found");
    }
}
