package br.com.matteusmoreno.api.websocket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@ServerEndpoint("/shows/live/{artistId}") // Define o endereço do WebSocket
@ApplicationScoped
@Slf4j
public class ShowWebSocket {

  // ✅ CORRIGIDO: Múltiplas sessões por artistId
  private final Map<String, Set<Session>> sessions = new ConcurrentHashMap<>();

  @OnOpen
  public void onOpen(Session session, @PathParam("artistId") String artistId) {
    sessions.computeIfAbsent(artistId, k -> new CopyOnWriteArraySet<>()).add(session);
    int totalConnections = sessions.get(artistId).size();
    log.info("✅ Cliente conectou ao WebSocket do artista {}. Total de conexões: {}",
        artistId, totalConnections);
  }

  @OnClose
  public void onClose(Session session, @PathParam("artistId") String artistId) {
    Set<Session> artistSessions = sessions.get(artistId);
    if (artistSessions != null) {
      artistSessions.remove(session);
      if (artistSessions.isEmpty()) {
        sessions.remove(artistId);
      }
    }
    log.info("❌ Cliente desconectou do WebSocket do artista {}", artistId);
  }

  @OnError
  public void onError(Session session, @PathParam("artistId") String artistId, Throwable throwable) {
    Set<Session> artistSessions = sessions.get(artistId);
    if (artistSessions != null) {
      artistSessions.remove(session);
      if (artistSessions.isEmpty()) {
        sessions.remove(artistId);
      }
    }
    log.error("❌ Erro no WebSocket para o artista {}: {}", artistId, throwable.getMessage());
  }

  /**
   * Envia mensagem para TODAS as sessões de um artista específico
   */
  public void sendToArtist(String artistId, String message) {
    Set<Session> artistSessions = sessions.get(artistId);
    if (artistSessions != null) {
      artistSessions.forEach(session -> {
        if (session.isOpen()) {
          session.getAsyncRemote().sendText(message);
        }
      });
      log.debug("📤 Mensagem enviada para {} sessões do artista {}",
          artistSessions.size(), artistId);
    }
  }

  /**
   * Envia mensagem para TODOS os artistas conectados
   */
  public void broadcastToAll(String message) {
    sessions.forEach((artistId, artistSessions) -> {
      artistSessions.forEach(session -> {
        if (session.isOpen()) {
          session.getAsyncRemote().sendText(message);
        }
      });
    });
    log.debug("📢 Broadcast enviado para todos os clientes conectados");
  }

  /**
   * Notifica sobre início de show
   */
  public void notifyShowStarted(String artistId, String showId) {
    String message = String.format(
        "{\"type\":\"show-started\",\"artistId\":\"%s\",\"showId\":\"%s\",\"timestamp\":\"%s\"}",
        artistId, showId, java.time.LocalDateTime.now()
    );
    sendToArtist(artistId, message);
    log.info("📣 Notificação: Show {} iniciado - enviado para todos os clientes do artista {}",
        showId, artistId);
  }

  /**
   * Notifica sobre término de show
   */
  public void notifyShowEnded(String artistId, String showId) {
    String message = String.format(
        "{\"type\":\"show-ended\",\"artistId\":\"%s\",\"showId\":\"%s\",\"timestamp\":\"%s\"}",
        artistId, showId, java.time.LocalDateTime.now()
    );
    sendToArtist(artistId, message);
    log.info("📣 Notificação: Show {} encerrado - enviado para todos os clientes do artista {}",
        showId, artistId);
  }

  /**
   * Notifica sobre novo pedido de música
   */
  public void notifyNewSongRequest(String artistId, String showId, String requestId) {
    String message = String.format(
        "{\"type\":\"new-song-request\",\"artistId\":\"%s\",\"showId\":\"%s\",\"requestId\":\"%s\",\"timestamp\":\"%s\"}",
        artistId, showId, requestId, java.time.LocalDateTime.now()
    );
    sendToArtist(artistId, message);
    log.info("📣 Notificação: Novo pedido {} para show {} - enviado para todos os clientes",
        requestId, showId);
  }

  /**
   * Notifica sobre atualização de status de pedido
   */
  public void notifyRequestStatusUpdated(String artistId, String showId, String requestId, String newStatus) {
    String message = String.format(
        "{\"type\":\"request-status-updated\",\"artistId\":\"%s\",\"showId\":\"%s\",\"requestId\":\"%s\",\"status\":\"%s\",\"timestamp\":\"%s\"}",
        artistId, showId, requestId, newStatus, java.time.LocalDateTime.now()
    );
    sendToArtist(artistId, message);
    log.info("📣 Notificação: Status do pedido {} atualizado para {} - enviado para todos os clientes",
        requestId, newStatus);
  }

  /**
   * Notifica sobre início de livestream
   */
  public void notifyLiveStreamStarted(String artistId, String showId, String liveStreamId) {
    String message = String.format(
        "{\"type\":\"livestream-started\",\"artistId\":\"%s\",\"showId\":\"%s\",\"liveStreamId\":\"%s\",\"timestamp\":\"%s\"}",
        artistId, showId, liveStreamId, java.time.LocalDateTime.now()
    );
    sendToArtist(artistId, message);
    log.info("📣 Notificação: Livestream {} iniciada para show {} - enviado para todos os clientes",
        liveStreamId, showId);
  }

  /**
   * Notifica sobre término de livestream
   */
  public void notifyLiveStreamEnded(String artistId, String showId) {
    String message = String.format(
        "{\"type\":\"livestream-ended\",\"artistId\":\"%s\",\"showId\":\"%s\",\"timestamp\":\"%s\"}",
        artistId, showId, java.time.LocalDateTime.now()
    );
    sendToArtist(artistId, message);
    log.info("📣 Notificação: Livestream encerrada para show {} - enviado para todos os clientes",
        showId);
  }

  public void notifyViewerCountUpdated(String artistId, String showId, int count) {
    String message = String.format(
        "{\"type\":\"viewer-count-updated\",\"artistId\":\"%s\",\"showId\":\"%s\",\"count\":%d,\"timestamp\":\"%s\"}",
        artistId, showId, count, java.time.LocalDateTime.now()
    );
    sendToArtist(artistId, message);
    log.debug("📣 Notificação: Contagem de viewers atualizada para {} no show {}", count, showId);
  }

  /**
   * Verifica se há clientes conectados para um artista
   */
  public boolean hasConnectedClients(String artistId) {
    Set<Session> artistSessions = sessions.get(artistId);
    return artistSessions != null && !artistSessions.isEmpty();
  }

  /**
   * Retorna quantidade de clientes conectados para um artista
   */
  public int getConnectedCount(String artistId) {
    Set<Session> artistSessions = sessions.get(artistId);
    return artistSessions != null ? artistSessions.size() : 0;
  }

  /**
   * Retorna quantidade total de conexões ativas
   */
  public int getTotalConnections() {
    return sessions.values().stream().mapToInt(Set::size).sum();
  }
}