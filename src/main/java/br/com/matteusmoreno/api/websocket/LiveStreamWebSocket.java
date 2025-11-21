package br.com.matteusmoreno.api.websocket;

import br.com.matteusmoreno.domain.show.service.LiveStreamService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket para sinalização WebRTC (signaling server)
 * NÃO retransmite dados de vídeo - apenas facilita conexão P2P
 */
@ServerEndpoint("/livestream/{showId}/{userId}/{role}")
@ApplicationScoped
@Slf4j
public class LiveStreamWebSocket {

  private static final String ROLE_BROADCASTER = "broadcaster";
  private static final String ROLE_VIEWER = "viewer";
  private static final long COOLDOWN_MS = 5000;

  private static final Map<String, Session> broadcasters = new ConcurrentHashMap<>();
  private static final Map<String, Set<Session>> viewers = new ConcurrentHashMap<>();
  private static final Map<String, Long> lastConnectionAttempt = new ConcurrentHashMap<>();

  private final LiveStreamService liveStreamService;

  public LiveStreamWebSocket(LiveStreamService liveStreamService) {
    this.liveStreamService = liveStreamService;
  }

  @OnOpen
  public void onOpen(Session session,
      @PathParam("showId") String showId,
      @PathParam("userId") String userId,
      @PathParam("role") String role) {

    if (ROLE_BROADCASTER.equalsIgnoreCase(role)) {
      String broadcasterKey = showId + ":" + userId;
      long now = System.currentTimeMillis();

      cleanOldAttempts();

      Long lastAttempt = lastConnectionAttempt.get(broadcasterKey);

      if (lastAttempt != null && (now - lastAttempt) < COOLDOWN_MS) {
        long waitTime = COOLDOWN_MS - (now - lastAttempt);
        log.warn("⚠️ COOLDOWN ATIVO - Artista {} tentando reconectar muito rápido ao show {}. Aguarde {} ms.",
            userId, showId, waitTime);
        try {
          session.close(new CloseReason(
              CloseReason.CloseCodes.TRY_AGAIN_LATER,
              "Please wait " + waitTime + "ms before reconnecting"
          ));
        } catch (Exception e) {
          log.debug("Erro ao fechar sessão: {}", e.getMessage());
        }
        return;
      }

      Session existingBroadcaster = broadcasters.get(showId);
      if (existingBroadcaster != null && existingBroadcaster.isOpen()) {
        log.warn("🚫 CONEXÃO REJEITADA - Broadcaster já ativo para o show {}. Frontend deve parar de tentar reconectar.", showId);
        try {
          session.close(new CloseReason(
              CloseReason.CloseCodes.CANNOT_ACCEPT,
              "Broadcaster already connected. Stop reconnecting."
          ));
        } catch (Exception e) {
          log.debug("Erro ao fechar nova sessão: {}", e.getMessage());
        }
        return;
      }

      lastConnectionAttempt.put(broadcasterKey, now);
      broadcasters.put(showId, session);
      log.info("Broadcaster (Artista {}) conectado ao show {}", userId, showId);

      notifyViewers(showId, "{\"type\":\"stream-started\",\"showId\":\"" + showId + "\"}");

    } else if (ROLE_VIEWER.equalsIgnoreCase(role)) {
      // Espectador se conectando
      viewers.computeIfAbsent(showId, k -> new CopyOnWriteArraySet<>()).add(session);
      log.info("Viewer (Usuário {}) conectado ao show {}", userId, showId);

      Session broadcaster = broadcasters.get(showId);
      if (broadcaster != null && broadcaster.isOpen()) {
        // Notifica o viewer se já existe um broadcaster ativo
        session.getAsyncRemote().sendText("{\"type\":\"broadcaster-ready\",\"showId\":\"" + showId + "\"}");

        // 📣 REATIVO: Notifica broadcaster sobre novo viewer
        notifyBroadcasterNewViewer(showId, userId);

        // 📣 REATIVO: Atualiza contagem de viewers
        notifyViewerCountChanged(showId);
      }
    }
  }

  @OnMessage
  public void onMessage(Session session, String message,
      @PathParam("showId") String showId,
      @PathParam("userId") String userId,
      @PathParam("role") String role) {

    if (ROLE_BROADCASTER.equalsIgnoreCase(role)) {
      // Broadcaster enviando sinalização (WebRTC signaling)
      try {
        JSONObject json = new JSONObject(message);
        String messageType = json.getString("type");

        // Se a mensagem tem viewerId, envia apenas para aquele viewer
        if (json.has("viewerId")) {
          String targetViewerId = json.getString("viewerId");
          sendToSpecificViewer(showId, targetViewerId, message);
          log.debug("📤 Mensagem '{}' do broadcaster enviada para viewer: {}", messageType, targetViewerId);
        } else {
          // Retransmite para todos os viewers
          notifyViewers(showId, message);
          log.debug("📤 Mensagem '{}' do broadcaster retransmitida para todos viewers", messageType);
        }

      } catch (Exception e) {
        log.error("Erro ao processar mensagem do broadcaster", e);
      }

    } else if (ROLE_VIEWER.equalsIgnoreCase(role)) {
      // Viewer enviando sinalização de volta ao broadcaster
      Session broadcaster = broadcasters.get(showId);
      if (broadcaster != null && broadcaster.isOpen()) {
        try {
          // ✅✅✅ ADICIONA viewerId na mensagem ✅✅✅
          JSONObject json = new JSONObject(message);
          json.put("viewerId", userId);
          broadcaster.getAsyncRemote().sendText(json.toString());
          log.debug("📥 Mensagem de sinalização do viewer {} retransmitida para broadcaster", userId);
        } catch (Exception e) {
          log.error("Erro ao processar mensagem do viewer", e);
        }
      }
    }
  }

  @OnClose
  public void onClose(Session session,
      @PathParam("showId") String showId,
      @PathParam("userId") String userId,
      @PathParam("role") String role) {

    if (ROLE_BROADCASTER.equalsIgnoreCase(role)) {
      String broadcasterKey = showId + ":" + userId;

      Session currentBroadcaster = broadcasters.get(showId);
      if (currentBroadcaster != null && currentBroadcaster.equals(session)) {
        broadcasters.remove(showId);
        lastConnectionAttempt.remove(broadcasterKey);
        log.info("🔌 Broadcaster (Artista {}) desconectado do show {}", userId, showId);

        notifyViewers(showId, "{\"type\":\"stream-ended\",\"showId\":\"" + showId + "\"}");
      } else {
        log.debug("Sessão de broadcaster fechada mas já foi substituída para o show {}", showId);
      }

    } else if (ROLE_VIEWER.equalsIgnoreCase(role)) {
      Set<Session> showViewers = viewers.get(showId);
      if (showViewers != null) {
        showViewers.remove(session);
        if (showViewers.isEmpty()) {
          viewers.remove(showId);
        }
      }
      log.info("🔌 Viewer (Usuário {}) desconectado do show {}", userId, showId);

      // 📣 REATIVO: Desregistra viewer do LiveStreamService
      if (liveStreamService != null) {
        liveStreamService.unregisterViewer(showId, userId);
      }

      // 📣 REATIVO: Notifica broadcaster que viewer saiu
      notifyBroadcasterViewerLeft(showId, userId);

      // 📣 REATIVO: Atualiza contagem de viewers
      notifyViewerCountChanged(showId);
    }
  }

  @OnError
  public void onError(Session session, Throwable throwable,
      @PathParam("showId") String showId,
      @PathParam("userId") String userId,
      @PathParam("role") String role) {

    log.error("❌ Erro no WebSocket - usuário: {}, show: {}, role: {}",
        userId, showId, role, throwable);

    if (ROLE_BROADCASTER.equalsIgnoreCase(role)) {
      Session currentBroadcaster = broadcasters.get(showId);
      if (currentBroadcaster != null && currentBroadcaster.equals(session)) {
        broadcasters.remove(showId);
        notifyViewers(showId, "{\"type\":\"stream-error\",\"showId\":\"" + showId + "\"}");
      }
    } else if (ROLE_VIEWER.equalsIgnoreCase(role)) {
      Set<Session> showViewers = viewers.get(showId);
      if (showViewers != null) {
        showViewers.remove(session);
      }
    }
  }

  /**
   * Envia mensagem para um viewer específico
   */
  private void sendToSpecificViewer(String showId, String viewerId, String message) {
    Set<Session> showViewers = viewers.get(showId);
    if (showViewers != null) {
      for (Session viewer : showViewers) {
        try {
          // Extrai userId dos parâmetros da sessão
          String pathUserId = viewer.getPathParameters().get("userId");

          if (viewerId.equals(pathUserId) && viewer.isOpen()) {
            viewer.getAsyncRemote().sendText(message);
            log.debug("✉️ Mensagem enviada para viewer específico: {}", viewerId);
            return;
          }
        } catch (Exception e) {
          log.warn("Erro ao enviar mensagem para viewer: {}", viewerId, e);
        }
      }
      log.warn("⚠️ Viewer não encontrado: {}", viewerId);
    }
  }

  /**
   * Notifica todos os viewers de um show
   */
  private void notifyViewers(String showId, String message) {
    Set<Session> showViewers = viewers.get(showId);
    if (showViewers != null) {
      for (Session viewer : showViewers) {
        if (viewer.isOpen()) {
          viewer.getAsyncRemote().sendText(message);
        }
      }
      log.debug("📢 Broadcast enviado para {} viewers", showViewers.size());
    }
  }

  /**
   * Retorna o número de viewers conectados
   */
  public int getViewerCount(String showId) {
    Set<Session> showViewers = viewers.get(showId);
    return showViewers != null ? showViewers.size() : 0;
  }

  /**
   * Verifica se existe broadcaster ativo
   */
  public boolean hasBroadcaster(String showId) {
    Session broadcaster = broadcasters.get(showId);
    return broadcaster != null && broadcaster.isOpen();
  }

  /**
   * Limpa timestamps antigos
   */
  private void cleanOldAttempts() {
    long now = System.currentTimeMillis();
    lastConnectionAttempt.entrySet().removeIf(entry ->
        (now - entry.getValue()) > 30000
    );
  }

  // ========== MÉTODOS REATIVOS ==========

  /**
   * 📣 REATIVO: Notifica viewers sobre início da transmissão
   */
  public void notifyLiveStreamStarted(String showId, String quality) {
    String message = String.format(
        "{\"type\":\"livestream-started\",\"showId\":\"%s\",\"quality\":\"%s\",\"timestamp\":\"%s\"}",
        showId, quality, java.time.LocalDateTime.now()
    );
    notifyViewers(showId, message);
    log.info("📣 Notificação: Transmissão iniciada para show {}", showId);
  }

  /**
   * 📣 REATIVO: Notifica viewers sobre término da transmissão
   */
  public void notifyLiveStreamEnded(String showId, Integer totalViewers, Integer peakViewers) {
    String message = String.format(
        "{\"type\":\"livestream-ended\",\"showId\":\"%s\",\"totalViewers\":%d,\"peakViewers\":%d,\"timestamp\":\"%s\"}",
        showId, totalViewers, peakViewers, java.time.LocalDateTime.now()
    );
    notifyViewers(showId, message);
    log.info("📣 Notificação: Transmissão encerrada para show {}", showId);
  }

  /**
   * 📣 REATIVO: Atualiza contagem de viewers em tempo real
   */
  public void notifyViewerCountChanged(String showId) {
    int currentViewers = getViewerCount(showId);
    String message = String.format(
        "{\"type\":\"viewer-count-updated\",\"showId\":\"%s\",\"count\":%d,\"timestamp\":\"%s\"}",
        showId, currentViewers, java.time.LocalDateTime.now()
    );

    // Notifica broadcaster
    Session broadcaster = broadcasters.get(showId);
    if (broadcaster != null && broadcaster.isOpen()) {
      broadcaster.getAsyncRemote().sendText(message);
    }

    // Notifica todos os viewers
    notifyViewers(showId, message);

    log.debug("📊 Contagem de viewers atualizada para show {}: {}", showId, currentViewers);
  }

  /**
   * 📣 REATIVO: Notifica mudança de qualidade da transmissão
   */
  public void notifyQualityChanged(String showId, String newQuality) {
    String message = String.format(
        "{\"type\":\"stream-quality-changed\",\"showId\":\"%s\",\"quality\":\"%s\",\"timestamp\":\"%s\"}",
        showId, newQuality, java.time.LocalDateTime.now()
    );
    notifyViewers(showId, message);
    log.info("📣 Notificação: Qualidade alterada para {} no show {}", newQuality, showId);
  }

  /**
   * 📣 REATIVO: Notifica broadcaster sobre novo viewer
   */
  public void notifyBroadcasterNewViewer(String showId, String viewerId) {
    Session broadcaster = broadcasters.get(showId);
    if (broadcaster != null && broadcaster.isOpen()) {
      String message = String.format(
          "{\"type\":\"viewer-joined\",\"showId\":\"%s\",\"viewerId\":\"%s\",\"totalViewers\":%d,\"timestamp\":\"%s\"}",
          showId, viewerId, getViewerCount(showId), java.time.LocalDateTime.now()
      );
      broadcaster.getAsyncRemote().sendText(message);
      log.debug("📣 Notificado broadcaster sobre viewer {}", viewerId);
    }
  }


  /**
   * 📣 REATIVO: Notifica broadcaster sobre viewer que saiu
   */
  public void notifyBroadcasterViewerLeft(String showId, String viewerId) {
    Session broadcaster = broadcasters.get(showId);
    if (broadcaster != null && broadcaster.isOpen()) {
      String message = String.format(
          "{\"type\":\"viewer-left\",\"showId\":\"%s\",\"viewerId\":\"%s\",\"totalViewers\":%d,\"timestamp\":\"%s\"}",
          showId, viewerId, getViewerCount(showId), java.time.LocalDateTime.now()
      );
      broadcaster.getAsyncRemote().sendText(message);
      log.debug("📣 Notificado broadcaster sobre saída do viewer {}", viewerId);
    }
  }
}