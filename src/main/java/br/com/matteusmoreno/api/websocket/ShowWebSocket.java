package br.com.matteusmoreno.api.websocket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@ServerEndpoint("/shows/live/{artistId}") // Define o endereço do WebSocket
@ApplicationScoped
@Slf4j
public class ShowWebSocket {

    // Um mapa para guardar a sessão de cada artista conectado
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("artistId") String artistId) {
        sessions.put(artistId, session);
        log.info("Artista {} conectou ao WebSocket.", artistId);
    }

    @OnClose
    public void onClose(Session session, @PathParam("artistId") String artistId) {
        sessions.remove(artistId);
        log.info("Artista {} desconectou do WebSocket.", artistId);
    }

    @OnError
    public void onError(Session session, @PathParam("artistId") String artistId, Throwable throwable) {
        sessions.remove(artistId);
        log.error("Erro no WebSocket para o artista {}: {}", artistId, throwable.getMessage());
    }

    /**
     * Este método será chamado por outros serviços para enviar uma mensagem
     * para um artista específico.
     */
    public void sendToArtist(String artistId, String message) {
        Session session = sessions.get(artistId);
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(message);
        }
    }
}