package br.com.matteusmoreno.domain.show.response;

import br.com.matteusmoreno.domain.show.LiveStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LiveStreamResponse {

    private String id;
    private String showId;
    private String artistId;
    private Boolean isActive;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalViewers;
    private Integer peakViewers;
    private String streamQuality;
    private Integer currentViewers;

    public LiveStreamResponse(LiveStream liveStream) {
        this.id = liveStream.id.toString();
        this.showId = liveStream.showId.toString();
        this.artistId = liveStream.artistId.toString();
        this.isActive = liveStream.isActive;
        this.startTime = liveStream.startTime;
        this.endTime = liveStream.endTime;
        this.totalViewers = liveStream.totalViewers;
        this.peakViewers = liveStream.peakViewers;
        this.streamQuality = liveStream.streamQuality;
        this.currentViewers = 0; // Será preenchido pelo serviço
    }

    public LiveStreamResponse(LiveStream liveStream, Integer currentViewers) {
        this(liveStream);
        this.currentViewers = currentViewers;
    }
}

