package br.com.matteusmoreno.api;

import br.com.matteusmoreno.domain.show.ShowEvent;
import br.com.matteusmoreno.domain.show.request.MakeSongRequest;
import br.com.matteusmoreno.domain.show.request.UpdateRequestStatus;
import br.com.matteusmoreno.domain.show.response.ShowDetailsResponse;
import br.com.matteusmoreno.domain.show.service.ShowService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

@Path("/shows")
public class ShowResource {

    private final ShowService showService;

    public ShowResource(ShowService showService) {
        this.showService = showService;
    }

    @POST
    @Path("/start/{artistId}")
    public Response startShow(@PathParam("artistId") String artistId) {
        ShowEvent showEvent = showService.startShow(new ObjectId(artistId));
        return Response.status(Response.Status.CREATED).entity(new ShowDetailsResponse(showEvent)).build();
    }

    @PATCH
    @Path("/end/{showId}")
    public Response endShow(@PathParam("showId") String showId) {
        ShowEvent showEvent = showService.endShow(new ObjectId(showId));
        return Response.ok(new ShowDetailsResponse(showEvent)).build();
    }

    @POST
    @Path("/request")
    public Response makeRequest(@Valid MakeSongRequest request) throws Exception {
        showService.makeSongRequest(request);
        return Response.status(Response.Status.ACCEPTED).entity("Request received and is being processed.").build();
    }

    @PATCH
    @Path("/{showId}/request/{requestId}")
    public Response updateRequestStatus(
            @PathParam("showId") String showId,
            @PathParam("requestId") String requestId,
            @Valid UpdateRequestStatus request) {

        showService.updateRequestStatus(new ObjectId(showId), new ObjectId(requestId), request.status());
        return Response.noContent().build();
    }
}