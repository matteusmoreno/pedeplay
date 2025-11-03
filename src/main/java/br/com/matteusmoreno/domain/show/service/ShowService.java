package br.com.matteusmoreno.domain.show.service;

import br.com.matteusmoreno.api.websocket.ShowWebSocket;
import br.com.matteusmoreno.api.websocket.dto.NewSongRequestNotification;
import br.com.matteusmoreno.api.websocket.dto.RequestStatusUpdateNotification;
import br.com.matteusmoreno.domain.artist.Artist;
import br.com.matteusmoreno.domain.artist.service.ArtistService;
import br.com.matteusmoreno.domain.show.ShowEvent;
import br.com.matteusmoreno.domain.show.SongRequest;
import br.com.matteusmoreno.domain.show.constant.RequestStatus;
import br.com.matteusmoreno.domain.show.constant.ShowStatus;
import br.com.matteusmoreno.domain.show.request.MakeSongRequest;
import br.com.matteusmoreno.domain.show.request.UpdateRequestStatus;
import br.com.matteusmoreno.domain.song.Song;
import br.com.matteusmoreno.domain.song.service.SongService;
import br.com.matteusmoreno.domain.subscription.service.SubscriptionService;
import br.com.matteusmoreno.exception.ShowConflictException;
import br.com.matteusmoreno.exception.ShowEventNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

@ApplicationScoped
@Slf4j
public class ShowService {

    private final ArtistService artistService;
    private final SubscriptionService subscriptionService;
    private final ShowWebSocket showWebSocket;
    private final SongService songService;
    private final ObjectMapper objectMapper;

    public ShowService(ArtistService artistService, SubscriptionService subscriptionService, ShowWebSocket showWebSocket, SongService songService,
        ObjectMapper objectMapper) {
        this.artistService = artistService;
        this.subscriptionService = subscriptionService;
        this.showWebSocket = showWebSocket;
        this.songService = songService;
      this.objectMapper = objectMapper;
    }

    // INICIA MODO SHOW
    public ShowEvent startShow(ObjectId artistId, String loggedInArtistId) {
        if (!artistId.equals(new ObjectId(loggedInArtistId))) throw new ForbiddenException("You can only start a show for yourself.");
        Artist artist = artistService.getArtistById(artistId);

        long activeShows = ShowEvent.count("artistId = ?1 and status = ?2", artist.id, ShowStatus.ACTIVE);
        if (activeShows > 0) throw new ShowConflictException("Artist already has an active show.");

        // Verifica e incrementa o limite do plano
        subscriptionService.verifyAndIncrementEventUsage(artist);
        ShowEvent showEvent = createNewShowEvent(artist.id);

        ShowEvent.persist(showEvent);
        artist.update();

        return showEvent;
    }

    // FINALIZA MODO SHOW
    public ShowEvent endShow(ObjectId showId, String loggedInArtistId) {
        ShowEvent showEvent = ShowEvent.findById(showId);
        if (showEvent == null || showEvent.status == ShowStatus.FINISHED) throw new ShowConflictException("Show not found or already finished.");

        if (!showEvent.artistId.equals(new ObjectId(loggedInArtistId))) throw new ForbiddenException("You can only end your own show.");

        LocalDateTime endTime = LocalDateTime.now();
        long duration = Duration.between(showEvent.startTime, endTime).toSeconds();

        showEvent.status = ShowStatus.FINISHED;
        showEvent.endTime = endTime;
        showEvent.durationInSeconds = (int) duration;
        showEvent.totalRequests = showEvent.requests.size();

        showEvent.update();

        return showEvent;
    }

    // FAZ PEDIDO DE MÚSICA
    public ShowEvent makeSongRequest(MakeSongRequest request) throws Exception {
        Artist artist = artistService.getArtistById(request.artistId());
        ShowEvent activeShow = ShowEvent.<ShowEvent>find("artistId = ?1 and status = ?2", request.artistId(), ShowStatus.ACTIVE)
                .firstResultOptional()
                .orElseThrow(() -> new ShowConflictException("Artist does not have an active show."));

        subscriptionService.verifyAndIncrementRequestUsage(artist);

        // Define a gorjeta como ZERO se ela vier nula (pedido gratuito)
        BigDecimal tipAmount = Objects.requireNonNullElse(request.tipAmount(), BigDecimal.ZERO);

        // --- PONTO DE INTEGRAÇÃO COM PAGAMENTO ---
        // Se a gorjeta for maior que zero, aqui você iniciaria o fluxo de pagamento.
        // O status ficaria "PENDING_PAYMENT" e só mudaria para "PENDING" no webhook.
        // Por enquanto, vamos adicionar todos os pedidos direto na fila.

        SongRequest newSongRequest = createSongRequest(request);

        activeShow.requests.add(newSongRequest);
        activeShow.totalTipsValue = activeShow.totalTipsValue.add(tipAmount);
        activeShow.totalRequests = activeShow.requests.size();

        activeShow.update();
        artist.update();

        try {
            NewSongRequestNotification notification = new NewSongRequestNotification(newSongRequest);
            String jsonMessage = objectMapper.writeValueAsString(notification);
            showWebSocket.sendToArtist(request.artistId().toString(), jsonMessage);
        } catch (Exception e) {
            log.error("Falha ao enviar notificação WebSocket de novo pedido para o artista {}: {}", request.artistId(), e.getMessage());
        }

        return activeShow;
    }

    // DETALHES DO SHOW POR ID
    public ShowEvent getShowEventById(ObjectId showId) {
        ShowEvent showEvent = ShowEvent.findById(showId);
        if (showEvent == null) throw new ShowEventNotFoundException("Show not found with ID: ".concat(showId.toString()));

        return showEvent;
    }

    // LISTAGEM DE TODOS OS SHOWS DE UM ARTISTA
    public List<ShowEvent> getAllShowsByArtist(int page, int size, ObjectId artistId, String loggedInArtistId) {
        if (!artistId.equals(new ObjectId(loggedInArtistId))) throw new ForbiddenException("You can only view your own shows.");

        return ShowEvent.find("artistId", artistId)
                .page(page, size)
                .list();
    }

    // PEGA O ÚLTIMO SHOW ATIVO DE UM ARTISTA
    public ShowEvent getActiveShowByArtist(ObjectId artistId) {
        return ShowEvent.<ShowEvent>find("artistId = ?1 and status = ?2", artistId, ShowStatus.ACTIVE)
                .firstResultOptional()
                .orElse(null);
    }

    //UPDATE REQUEST STATUS (ex: PLAYED, CANCELED)
    public ShowEvent updateRequestStatus(ObjectId showId, ObjectId requestId, UpdateRequestStatus request, String loggedInArtistId) {
        ShowEvent showEvent = getShowEventById(showId);

        if (!showEvent.artistId.equals(new ObjectId(loggedInArtistId))) throw new ForbiddenException("You can only update requests for your own show.");


        // Esta linha agora funciona, pois o campo 'requestId' existe
        SongRequest songRequestToUpdate = showEvent.requests.stream()
                .filter(req -> req.requestId.equals(requestId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Song Request not found with ID: " + requestId));

        RequestStatus newStatus = request.status();
        if (newStatus != RequestStatus.PLAYED && newStatus != RequestStatus.CANCELED) {
            throw new IllegalArgumentException("Status can only be updated to PLAYED or CANCELED.");
        }

        songRequestToUpdate.status = newStatus;
        showEvent.update();

        try {
            RequestStatusUpdateNotification notification = new RequestStatusUpdateNotification(requestId, newStatus);
            String jsonMessage = new ObjectMapper().writeValueAsString(notification);
            showWebSocket.sendToArtist(showEvent.artistId.toString(), jsonMessage);
        } catch (Exception e) {
            log.error("Falha ao enviar notificação WebSocket de atualização de status para o artista {}: {}", showEvent.artistId, e.getMessage());
        }

        return showEvent;
    }

    //PRIVATE METHODS
    private ShowEvent createNewShowEvent(ObjectId artistId) {
        return ShowEvent.builder()
                .artistId(artistId)
                .status(ShowStatus.ACTIVE)
                .startTime(LocalDateTime.now())
                .totalTipsValue(BigDecimal.ZERO)
                .requests(new ArrayList<>())
                .totalRequests(0)
                .durationInSeconds(0)
                .build();
    }

    private SongRequest createSongRequest(MakeSongRequest request) {
        String songTitle = "Dedicatória";
        String songArtist = " ";

        if (request.songId() != null) {
            Song song = songService.getSongById(request.songId());
            songTitle = song.title;
            songArtist = song.artistName;
        }

        // A gorjeta agora é opcional, então tratamos o valor nulo
        BigDecimal tipAmount = Objects.requireNonNullElse(request.tipAmount(), BigDecimal.ZERO);

        return SongRequest.builder()
                .requestId(new ObjectId())
                .songTitle(songTitle)
                .songArtist(songArtist)
                .tipAmount(tipAmount)
                .clientMessage(request.clientMessage())
                .status(RequestStatus.PENDING)
                .receivedAt(LocalDateTime.now())
                .build();
    }
}