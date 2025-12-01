package br.com.matteusmoreno.exception;
public class AvailabilityNotFoundException extends RuntimeException {
    public AvailabilityNotFoundException(String availabilityId) {
        super("Availability with id " + availabilityId + " not found");
    }
}
