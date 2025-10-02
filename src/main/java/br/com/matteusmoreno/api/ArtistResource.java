package br.com.matteusmoreno.api;

import br.com.matteusmoreno.domain.artist.Artist;
import br.com.matteusmoreno.domain.artist.request.AddSongRequest;
import br.com.matteusmoreno.domain.artist.request.CreateArtistRequest;
import br.com.matteusmoreno.domain.artist.response.ArtistDetailsResponse;
import br.com.matteusmoreno.domain.artist.service.ArtistService;
import jakarta.validation.Valid;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;

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

    @PATCH
    @Path("/repertoire")
    public Response addSongsToRepertoire(@Valid AddSongRequest request) {
        Artist artist = artistService.addSongsToRepertoire(request);

        return Response.ok(new ArtistDetailsResponse(artist)).build();
    }
}
