package br.com.matteusmoreno.api;

import br.com.matteusmoreno.domain.show.ShowEvent;
import br.com.matteusmoreno.domain.show.request.MakeSongRequest;
import br.com.matteusmoreno.domain.show.request.UpdateRequestStatus;
import br.com.matteusmoreno.domain.show.response.ShowDetailsResponse;
import br.com.matteusmoreno.domain.show.service.ShowService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@Path("/shows")
public class ShowResource {

    private final ShowService showService;
    private final JsonWebToken jwt;

    public ShowResource(ShowService showService, JsonWebToken jwt) {
        this.showService = showService;
        this.jwt = jwt;
    }

    @POST
    @Path("/start/{artistId}")
    @RolesAllowed("ARTIST")
    public Response startShow(@PathParam("artistId") String artistId) {
        String loggedInArtistId = jwt.getSubject();
        ShowEvent showEvent = showService.startShow(new ObjectId(artistId), loggedInArtistId);
        return Response.status(Response.Status.CREATED).entity(new ShowDetailsResponse(showEvent)).build();
    }

    @PATCH
    @Path("/end/{showId}")
    @RolesAllowed("ARTIST")
    public Response endShow(@PathParam("showId") String showId) {
        String loggedInArtistId = jwt.getSubject();
        ShowEvent showEvent = showService.endShow(new ObjectId(showId), loggedInArtistId);
        return Response.ok(new ShowDetailsResponse(showEvent)).build();
    }

    @GET
    @Path("/{showId}")
    public Response getShowDetails(@PathParam("showId") String showId) {
        ShowEvent showEvent = showService.getShowEventById(new ObjectId(showId));
        return Response.ok().entity(new ShowDetailsResponse(showEvent)).build();
    }

    @GET
    @Path("/all/{artistId}")
    @RolesAllowed("ARTIST")
    public Response getAllShowsByArtist(@QueryParam("page") @DefaultValue("0") int page,
                                        @QueryParam("size") @DefaultValue("10") int size,
                                        @PathParam("artistId") String artistId) {

        String loggedInArtistId = jwt.getSubject();
        List<ShowDetailsResponse> shows = showService.getAllShowsByArtist(page, size, new ObjectId(artistId), loggedInArtistId)
                .stream()
                .map(ShowDetailsResponse::new)
                .toList();

        return Response.ok(shows).build();
    }

    @GET
    @Path("/active/{artistId}")
    public Response getActiveShowByArtist(@PathParam("artistId") String artistId) {
        ShowEvent showEvent = showService.getActiveShowByArtist(new ObjectId(artistId));

        return Response.ok().entity(new ShowDetailsResponse(showEvent)).build();
    }

    @POST
    @Path("/request")
    public Response makeRequest(@Valid MakeSongRequest request) throws Exception {
        ShowEvent showEvent = showService.makeSongRequest(request);
        return Response.status(Response.Status.ACCEPTED).entity(new ShowDetailsResponse(showEvent)).build();
    }

    @PATCH
    @Path("/{showId}/requests/{requestId}/status")
    @RolesAllowed("ARTIST")
    public Response updateRequestStatus(
            @PathParam("showId") String showId,
            @PathParam("requestId") String requestId,
            @Valid UpdateRequestStatus request) {

        String loggedInArtistId = jwt.getSubject();
        ShowEvent showEvent = showService.updateRequestStatus(new ObjectId(showId), new ObjectId(requestId), request, loggedInArtistId);

        return Response.ok().entity(new ShowDetailsResponse(showEvent)).build();
    }
}