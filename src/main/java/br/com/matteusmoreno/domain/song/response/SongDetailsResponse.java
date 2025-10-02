package br.com.matteusmoreno.domain.song.response;

import br.com.matteusmoreno.domain.song.Song;

import java.time.LocalDateTime;

public record SongDetailsResponse(
        String title,
        String artistName,
        LocalDateTime createdAt) {

    public SongDetailsResponse(Song song) {
        this(
                song.title,
                song.artistName,
                song.createdAt
        );
    }
}
