package br.com.matteusmoreno.exception;
public class AvailabilityTimeOverlapException extends RuntimeException {
    public AvailabilityTimeOverlapException() {
        super("The selected availabilities have overlapping time ranges");
    }
}
