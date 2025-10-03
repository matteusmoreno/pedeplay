package br.com.matteusmoreno.api;

import br.com.matteusmoreno.domain.artist.Artist;
import br.com.matteusmoreno.domain.artist.request.AddSongOrRemoveRequest;
import br.com.matteusmoreno.domain.artist.request.CreateArtistRequest;
import br.com.matteusmoreno.domain.artist.request.UpdateArtistRequest;
import br.com.matteusmoreno.domain.artist.response.ArtistDetailsResponse;
import br.com.matteusmoreno.domain.artist.service.ArtistService;
import br.com.matteusmoreno.infrastructure.image.FileUploadRequest;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Path("/artists")
public class ArtistResource {

    private final ArtistService artistService;
    private final JsonWebToken jwt;

    public ArtistResource(ArtistService artistService, JsonWebToken jwt) {
        this.artistService = artistService;
        this.jwt = jwt;
    }

    @POST
    public Response create(@Valid CreateArtistRequest request, @Context UriInfo uriInfo) {
        Artist artist = artistService.createArtist(request);
        URI uri = uriInfo.getAbsolutePathBuilder().path(String.valueOf(artist.id)).build();

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

    @PATCH
    @Path("/profile-image/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed("ARTIST")
    public Response uploadProfileImage(@PathParam("id") String id, @BeanParam FileUploadRequest formData) {
        try {
            if (formData.file == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Image file is required.").build();
            }
            String loggedInArtistId = jwt.getSubject();
            Artist artist = artistService.uploadOrUpdateProfileImage(new ObjectId(id), formData.file, loggedInArtistId);
            return Response.ok(new ArtistDetailsResponse(artist)).build();

        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error processing file upload: " + e.getMessage())
                    .build();
        }
    }

    @PUT
    @RolesAllowed("ARTIST")
    public Response updateArtist(@Valid UpdateArtistRequest request) {
        String loggedInArtistId = jwt.getSubject();
        Artist artist = artistService.updateArtist(request, loggedInArtistId);

        return Response.ok(new ArtistDetailsResponse(artist)).build();
    }

    @DELETE
    @Path("/disable/{artistId}")
    @RolesAllowed("ARTIST")
    public Response disableArtist(@PathParam("artistId") String artistId) {
        String loggedInArtistId = jwt.getSubject();
        artistService.disableArtist(new ObjectId(artistId), loggedInArtistId);

        return Response.noContent().build();
    }

    @PATCH
    @Path("/enable/{artistId}")
    @RolesAllowed("ARTIST")
    public Response enableArtist(@PathParam("artistId") String artistId) {
        String loggedInArtistId = jwt.getSubject();
        artistService.enableArtist(new ObjectId(artistId), loggedInArtistId);

        return Response.noContent().build();
    }

    @PATCH
    @Path("/repertoire/add")
    @RolesAllowed("ARTIST")
    public Response addSongsToRepertoire(@Valid AddSongOrRemoveRequest request) {
        String loggedInArtistId = jwt.getSubject();
        Artist artist = artistService.addSongsToRepertoire(request, loggedInArtistId);

        return Response.ok(new ArtistDetailsResponse(artist)).build();
    }

    @PATCH
    @Path("/repertoire/remove")
    @RolesAllowed("ARTIST")
    public Response removeSongsFromRepertoire(@Valid AddSongOrRemoveRequest request) {
        String loggedInArtistId = jwt.getSubject();
        Artist artist = artistService.removeSongsFromRepertoire(request, loggedInArtistId);

        return Response.ok(new ArtistDetailsResponse(artist)).build();
    }
}