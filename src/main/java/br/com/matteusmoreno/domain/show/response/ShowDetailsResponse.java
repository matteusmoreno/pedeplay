package br.com.matteusmoreno.domain.show.response;

import br.com.matteusmoreno.domain.show.ShowEvent;
import br.com.matteusmoreno.domain.show.SongRequest;
import br.com.matteusmoreno.domain.show.constant.ShowStatus;
import org.bson.types.ObjectId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ShowDetailsResponse(
        ObjectId id,
        ObjectId artistId,
        ShowStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer durationInSeconds,
        BigDecimal totalTipsValue,
        Integer totalRequests,
        List<SongRequest> requests) {

    public ShowDetailsResponse(ShowEvent showEvent) {
        this(
                showEvent.id,
                showEvent.artistId,
                showEvent.status,
                showEvent.startTime,
                showEvent.endTime,
                showEvent.durationInSeconds,
                showEvent.totalTipsValue,
                showEvent.totalRequests,
                showEvent.requests
        );
    }
}