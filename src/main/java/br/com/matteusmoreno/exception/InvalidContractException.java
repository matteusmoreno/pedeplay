package br.com.matteusmoreno.exception;
public class InvalidContractException extends RuntimeException {
    public InvalidContractException(String message) {
        super(message);
    }
}
