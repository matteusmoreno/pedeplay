package br.com.matteusmoreno.domain.song.response;

import br.com.matteusmoreno.domain.song.Song;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

public record SongDetailsResponse(
        String title,
        String artistName) {

    public SongDetailsResponse(Song song) {
        this(
                song.title,
                song.artistName
        );
    }
}
