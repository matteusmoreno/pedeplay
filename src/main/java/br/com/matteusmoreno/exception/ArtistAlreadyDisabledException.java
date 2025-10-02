package br.com.matteusmoreno.exception;

public class ArtistAlreadyDisabledException extends RuntimeException {
    public ArtistAlreadyDisabledException(String message) {
        super(message);
    }
}
