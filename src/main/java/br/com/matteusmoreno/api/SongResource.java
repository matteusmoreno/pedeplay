package br.com.matteusmoreno.api;

import br.com.matteusmoreno.domain.song.CreateSongRequest;
import br.com.matteusmoreno.domain.song.Song;
import br.com.matteusmoreno.domain.song.SongDetailsResponse;
import br.com.matteusmoreno.domain.song.SongService;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;

@Path("/songs")
public class SongResource {

    private final SongService songService;

    public SongResource(SongService songService) {
        this.songService = songService;
    }

    @POST
    public Response create(@Valid CreateSongRequest request, @Context UriInfo uriInfo) {
        Song song = songService.createSong(request);
        URI uri = uriInfo.getAbsolutePathBuilder().path(String.valueOf(song.id)).build();

        return Response.created(uri).entity(new SongDetailsResponse(song)).build();
    }
}
