package br.com.matteusmoreno.exception;

public class ArtistAlreadyEnabledException extends RuntimeException {
    public ArtistAlreadyEnabledException(String message) {
        super(message);
    }
}
