package br.com.matteusmoreno.domain.song.response;

import br.com.matteusmoreno.domain.song.Song;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

public record SongDetailsResponse(
        ObjectId id,
        String title,
        String artistName) {

    public SongDetailsResponse(Song song) {
        this(
                song.id,
                song.title,
                song.artistName
        );
    }
}
