package br.com.matteusmoreno.domain.show.request;

import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;

import java.math.BigDecimal;

public record MakeSongRequest(
        @NotNull ObjectId songId,
        @NotNull ObjectId artistId,
        @NotNull BigDecimal tipAmount,
        String clientMessage) {}