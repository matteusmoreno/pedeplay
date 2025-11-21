package br.com.matteusmoreno.domain.show.service;

import br.com.matteusmoreno.api.websocket.LiveStreamWebSocket;
import br.com.matteusmoreno.domain.show.LiveStream;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Monitor para atualizar métricas de live streams automaticamente
 */
@ApplicationScoped
@Slf4j
public class LiveStreamMonitor {

    private final LiveStreamWebSocket liveStreamWebSocket;

    public LiveStreamMonitor(LiveStreamWebSocket liveStreamWebSocket) {
        this.liveStreamWebSocket = liveStreamWebSocket;
    }

    /**
     * Atualiza peak viewers a cada 10 segundos para todas as lives ativas
     * Isso garante que o pico real seja capturado mesmo sem chamadas externas
     */
    @Scheduled(every = "10s")
    void updatePeakViewers() {
        List<LiveStream> activeStreams = LiveStream.list("isActive", true);

        if (activeStreams.isEmpty()) {
            return; // Nenhuma live ativa
        }

        log.debug("🔄 Atualizando peak viewers para {} lives ativas", activeStreams.size());

        for (LiveStream stream : activeStreams) {
            try {
                // Busca viewers atuais conectados no WebSocket
                int currentViewers = liveStreamWebSocket.getViewerCount(stream.showId.toString());

                // Atualiza pico se o valor atual for maior
                if (currentViewers > stream.peakViewers) {
                    int oldPeak = stream.peakViewers;
                    stream.peakViewers = currentViewers;
                    stream.update();

                    log.info("🔝 Novo pico de viewers para live {}: {} → {} (showId: {})",
                             stream.id, oldPeak, currentViewers, stream.showId);
                }
            } catch (Exception e) {
                log.error("❌ Erro ao atualizar peak viewers para live {}: {}",
                          stream.id, e.getMessage());
            }
        }
    }

    /**
     * Remove viewers inativos do WebSocket a cada 30 segundos
     * Limpa sessões que ficaram abertas mas não estão mais ativas
     */
    @Scheduled(every = "30s")
    void cleanInactiveSessions() {
        // O LiveStreamWebSocket já gerencia isso automaticamente no onClose
        // Este método é um placeholder para futuras limpezas se necessário
        log.debug("🧹 Verificando sessões inativas do WebSocket");
    }
}

