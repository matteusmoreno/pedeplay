package br.com.matteusmoreno.domain.song.request;

import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;

public record UpdateSongRequest(
        @NotNull(message = "Song ID is required")
        ObjectId songId,
        String title,
        String artistName

) {}
