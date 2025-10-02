package br.com.matteusmoreno.exception;

public class SongConflictException extends RuntimeException {
    public SongConflictException(String message) {
        super(message);
    }
}
