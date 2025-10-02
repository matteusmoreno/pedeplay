package br.com.matteusmoreno.domain.artist.request;

import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;

import java.util.List;

public record AddSongRequest(
        @NotNull(message = "Artist ID cannot be null")
        ObjectId artistId,
        @NotNull(message = "Song IDs cannot be null")
        List<ObjectId> songIds) {}
