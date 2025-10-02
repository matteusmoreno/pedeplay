package br.com.matteusmoreno.domain.show.request;

import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import java.math.BigDecimal;

public record MakeSongRequest(
        ObjectId songId,
        @NotNull ObjectId artistId,
        BigDecimal tipAmount,
        String clientMessage) {}