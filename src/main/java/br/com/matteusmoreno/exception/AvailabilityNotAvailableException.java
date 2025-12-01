package br.com.matteusmoreno.exception;
public class AvailabilityNotAvailableException extends RuntimeException {
    public AvailabilityNotAvailableException(String availabilityId) {
        super("Availability with id " + availabilityId + " is not available for booking");
    }
}
