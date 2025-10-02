package br.com.matteusmoreno.api;

import br.com.matteusmoreno.domain.artist.Artist;
import br.com.matteusmoreno.domain.artist.request.AddSongRequest;
import br.com.matteusmoreno.domain.artist.request.CreateArtistRequest;
import br.com.matteusmoreno.domain.artist.response.ArtistDetailsResponse;
import br.com.matteusmoreno.domain.artist.service.ArtistService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.bson.types.ObjectId;

import java.net.URI;
import java.util.List;

@Path("/artists")
public class ArtistResource {

    private final ArtistService artistService;

    public ArtistResource(ArtistService artistService) {
        this.artistService = artistService;
    }

    @POST
    public Response create(@Valid CreateArtistRequest request, @Context UriInfo uriInfo) {
        Artist artist = artistService.createArtist(request);
        URI uri = uriInfo.getAbsolutePathBuilder().build();

        return Response.created(uri).entity(new ArtistDetailsResponse(artist)).build();
    }

    @GET
    @Path("/{artistId}")
    public Response getArtistById(@PathParam("artistId") String artistId) {
        Artist artist = artistService.getArtistById(new ObjectId(artistId));

        return Response.ok(new ArtistDetailsResponse(artist)).build();
    }

    @GET
    @Path("/all")
    public Response getAllArtists(@QueryParam("page") @DefaultValue("0") int page,
                                  @QueryParam("size") @DefaultValue("10") int size) {

        List<Artist> artists = artistService.getAllArtists(page, size);
        List<ArtistDetailsResponse> response = artists.stream().map(ArtistDetailsResponse::new).toList();

        return Response.ok(response).build();
    }


    @DELETE
    @Path("/disable/{artistId}")
    public Response disableArtist(@PathParam("artistId") String artistId) {
        artistService.disableArtist(new ObjectId(artistId));

        return Response.noContent().build();
    }

    @PATCH
    @Path("/enable/{artistId}")
    public Response enableArtist(@PathParam("artistId") String artistId) {
        artistService.enableArtist(new ObjectId(artistId));

        return Response.noContent().build();
    }

    @PATCH
    @Path("/repertoire")
    public Response addSongsToRepertoire(@Valid AddSongRequest request) {
        Artist artist = artistService.addSongsToRepertoire(request);

        return Response.ok(new ArtistDetailsResponse(artist)).build();
    }
}
