package br.com.matteusmoreno.domain.show.response;

import br.com.matteusmoreno.domain.show.ShowEvent;
import br.com.matteusmoreno.domain.show.constant.ShowStatus;
import org.bson.types.ObjectId;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowDetailsResponse(
        ObjectId id,
        ObjectId artistId,
        ShowStatus status,
        LocalDateTime startTime,
        BigDecimal totalTipsValue) {

    public ShowDetailsResponse(ShowEvent showEvent) {
        this(
                showEvent.id,
                showEvent.artistId,
                showEvent.status,
                showEvent.startTime,
                showEvent.totalTipsValue
        );
    }
}