package br.com.matteusmoreno.api;

import br.com.matteusmoreno.domain.song.request.CreateSongRequest;
import br.com.matteusmoreno.domain.song.Song;
import br.com.matteusmoreno.domain.song.request.UpdateSongRequest;
import br.com.matteusmoreno.domain.song.response.SongDetailsResponse;
import br.com.matteusmoreno.domain.song.service.SongService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.bson.types.ObjectId;

import java.net.URI;
import java.time.LocalDateTime;

@Path("/songs")
public class SongResource {

    private final SongService songService;

    public SongResource(SongService songService) {
        this.songService = songService;
    }

    @POST
    @RolesAllowed("ARTIST")
    public Response create(@Valid CreateSongRequest request, @Context UriInfo uriInfo) {
        Song song = songService.createOrFindSong(request);
        URI uri = uriInfo.getAbsolutePathBuilder().path(String.valueOf(song.id)).build();

        if (song.createdAt.isBefore(LocalDateTime.now().minusSeconds(1))) {
            return Response.ok(new SongDetailsResponse(song)).build();
        }
        return Response.created(uri).entity(new SongDetailsResponse(song)).build();
    }

    @GET
    @Path("/{songId}")
    public Response getSongById(@PathParam("songId") String songId) {
        Song song = songService.getSongById(new ObjectId(songId));

        return Response.ok(new SongDetailsResponse(song)).build();
    }

    @GET
    @Path("/all")
    public Response getAllSongs(@QueryParam("page") @DefaultValue("0") Integer page,
                                @QueryParam("size") @DefaultValue("10") Integer size) {

        return Response.ok(songService.getAllSongs(page, size).stream().map(SongDetailsResponse::new).toList()).build();
    }

    @PUT
    @Path("/update")
    @RolesAllowed("ARTIST")
    public Response updateSong(@Valid UpdateSongRequest request) {
        Song song = songService.updateSong(request);

        return Response.ok(new SongDetailsResponse(song)).build();
    }

    @DELETE
    @Path("/delete/{songId}")
    @RolesAllowed("ARTIST")
    public Response deleteSong(@PathParam("songId") String songId) {
        songService.deleteSong(new ObjectId(songId));

        return Response.noContent().build();
    }

}
