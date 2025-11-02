package br.com.matteusmoreno.domain.artist.response;

import br.com.matteusmoreno.domain.song.response.SongDetailsResponse;

import java.util.List;

public record ArtistRepertoireDetailsResponse(
        List<SongDetailsResponse> repertoire) {
}
