package br.com.matteusmoreno.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionHandler {

    @Provider
    public static class ArtistNotFoundMapper implements ExceptionMapper<ArtistNotFoundException> {
        @Override
        public Response toResponse(ArtistNotFoundException exception) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(exception.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class SubscriptionLimitMapper implements ExceptionMapper<SubscriptionLimitExceededException> {
        @Override
        public Response toResponse(SubscriptionLimitExceededException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(exception.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class SongNotFoundMapper implements ExceptionMapper<SongNotFoundException> {
        @Override
        public Response toResponse(SongNotFoundException exception) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(exception.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class SongConflictMapper implements ExceptionMapper<SongConflictException> {
        @Override
        public Response toResponse(SongConflictException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(exception.getMessage()))
                    .build();
        }
    }

    public record ErrorResponse(String message) {}
}