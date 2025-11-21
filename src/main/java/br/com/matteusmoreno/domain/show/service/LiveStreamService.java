package br.com.matteusmoreno.domain.show.service;

import br.com.matteusmoreno.api.websocket.LiveStreamWebSocket;
import br.com.matteusmoreno.domain.show.LiveStream;
import br.com.matteusmoreno.domain.show.ShowEvent;
import br.com.matteusmoreno.domain.show.constant.ShowStatus;
import br.com.matteusmoreno.domain.show.request.StartLiveStreamRequest;
import br.com.matteusmoreno.domain.show.response.LiveStreamResponse;
import br.com.matteusmoreno.exception.ShowEventNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

@ApplicationScoped
@Slf4j
public class LiveStreamService {

    private final LiveStreamWebSocket liveStreamWebSocket;
    private final Set<String> uniqueViewers = new HashSet<>();

    public LiveStreamService(LiveStreamWebSocket liveStreamWebSocket) {
        this.liveStreamWebSocket = liveStreamWebSocket;
    }

    public LiveStreamResponse startLiveStream(StartLiveStreamRequest request, String artistId) {
        ObjectId showId = new ObjectId(request.getShowId());

        ShowEvent showEvent = ShowEvent.<ShowEvent>findById(showId);
        if (showEvent == null) {
            throw new ShowEventNotFoundException("Show not found");
        }

        if (showEvent.status != ShowStatus.ACTIVE) {
            throw new IllegalStateException("Show is not active");
        }

        if (!showEvent.artistId.toString().equals(artistId)) {
            throw new SecurityException("You are not authorized to start stream for this show");
        }

        LiveStream existingStream = LiveStream.findActiveByShowId(showId);
        if (existingStream != null) {
            log.info("Live stream já existe para o show {}", showId);
            int currentViewers = liveStreamWebSocket.getViewerCount(showId.toString());
            return new LiveStreamResponse(existingStream, currentViewers);
        }

        LiveStream liveStream = LiveStream.builder()
                .showId(showId)
                .artistId(new ObjectId(artistId))
                .isActive(true)
                .startTime(LocalDateTime.now())
                .totalViewers(0)
                .peakViewers(0)
                .streamQuality(request.getStreamQuality() != null ? request.getStreamQuality() : "HD")
                .build();

        liveStream.persist();

        log.info("Live stream iniciada para o show {} pelo artista {}", showId, artistId);

        return new LiveStreamResponse(liveStream, 0);
    }

    public LiveStreamResponse stopLiveStream(String showId, String artistId) {
        ObjectId showObjectId = new ObjectId(showId);

        LiveStream liveStream = LiveStream.findActiveByShowId(showObjectId);
        if (liveStream == null) {
            throw new IllegalStateException("No active live stream found for this show");
        }

        if (!liveStream.artistId.toString().equals(artistId)) {
            throw new SecurityException("You are not authorized to stop this stream");
        }

        liveStream.isActive = false;
        liveStream.endTime = LocalDateTime.now();
        liveStream.update();

        log.info("Live stream encerrada para o show {}", showId);

        return new LiveStreamResponse(liveStream, 0);
    }

  public LiveStreamResponse getLiveStream(String showId) {
    log.info("🔍 Buscando live stream para showId: {}", showId);

    ObjectId showObjectId = new ObjectId(showId);
    log.info("📝 ObjectId convertido: {}", showObjectId);

    LiveStream liveStream = LiveStream.findActiveByShowId(showObjectId);
    log.info("📊 Resultado da busca: {}", liveStream != null ? "ENCONTRADO" : "NULL");

    if (liveStream != null) {
      log.info("✅ LiveStream encontrada - ID: {}, isActive: {}, showId: {}",
          liveStream.id, liveStream.isActive, liveStream.showId);
    }

    if (liveStream == null) {
      log.warn("❌ Nenhuma live stream ativa encontrada para showId: {}", showId);
      return null;
    }

    int currentViewers = liveStreamWebSocket.getViewerCount(showId);
    log.info("👥 Viewers atuais: {}", currentViewers);

    // Atualiza pico de viewers se necessário
    if (currentViewers > liveStream.peakViewers) {
      liveStream.peakViewers = currentViewers;
      liveStream.update();
    }

    return new LiveStreamResponse(liveStream, currentViewers);
  }

    public void registerViewer(String showId, String userId) {
        String viewerKey = showId + ":" + userId;

        if (!uniqueViewers.contains(viewerKey)) {
            uniqueViewers.add(viewerKey);

            ObjectId showObjectId = new ObjectId(showId);
            LiveStream liveStream = LiveStream.findActiveByShowId(showObjectId);

            if (liveStream != null) {
                liveStream.totalViewers++;

                int currentViewers = liveStreamWebSocket.getViewerCount(showId);
                if (currentViewers > liveStream.peakViewers) {
                    liveStream.peakViewers = currentViewers;
                }

                liveStream.update();
                log.info("Novo viewer registrado. Total de viewers únicos: {}", liveStream.totalViewers);
            }
        }
    }

    public boolean hasActiveLiveStream(String artistId) {
        LiveStream liveStream = LiveStream.findActiveByArtistId(new ObjectId(artistId));
        return liveStream != null;
    }
}

