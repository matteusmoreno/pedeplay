package br.com.matteusmoreno.api;

import br.com.matteusmoreno.domain.show.request.StartLiveStreamRequest;
import br.com.matteusmoreno.domain.show.response.LiveStreamResponse;
import br.com.matteusmoreno.domain.show.service.LiveStreamService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Resource para gerenciar transmissões ao vivo
 */
@Path("/livestreams")
public class LiveStreamResource {

    private final LiveStreamService liveStreamService;
    private final JsonWebToken jwt;

    public LiveStreamResource(LiveStreamService liveStreamService, JsonWebToken jwt) {
        this.liveStreamService = liveStreamService;
        this.jwt = jwt;
    }

    @POST
    @Path("/start")
    @RolesAllowed("ARTIST")
    public Response startLiveStream(@Valid StartLiveStreamRequest request) {
        String artistId = jwt.getSubject();
        LiveStreamResponse response = liveStreamService.startLiveStream(request, artistId);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/stop/{showId}")
    @RolesAllowed("ARTIST")
    public Response stopLiveStream(@PathParam("showId") String showId) {
        String artistId = jwt.getSubject();
        LiveStreamResponse response = liveStreamService.stopLiveStream(showId, artistId);
        return Response.ok(response).build();
    }

    @GET
    @Path("/{showId}")
    public Response getLiveStream(@PathParam("showId") String showId) {
        LiveStreamResponse response = liveStreamService.getLiveStream(showId);

        if (response == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"message\": \"No active live stream found\"}")
                    .build();
        }

        return Response.ok(response).build();
    }

    @POST
    @Path("/{showId}/register")
    public Response registerViewer(@PathParam("showId") String showId,
                                   @QueryParam("userId") String userId) {
        liveStreamService.registerViewer(showId, userId);
        return Response.ok().build();
    }


    @GET
    @Path("/artist/{artistId}/active")
    public Response hasActiveLiveStream(@PathParam("artistId") String artistId) {
        boolean hasActive = liveStreamService.hasActiveLiveStream(artistId);
        return Response.ok()
                .entity("{\"hasActiveLiveStream\": " + hasActive + "}")
                .build();
    }
}

