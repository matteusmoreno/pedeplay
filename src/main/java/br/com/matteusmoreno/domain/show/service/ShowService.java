package br.com.matteusmoreno.domain.show.service;

import br.com.matteusmoreno.api.ShowWebSocket;
import br.com.matteusmoreno.domain.artist.Artist;
import br.com.matteusmoreno.domain.artist.service.ArtistService;
import br.com.matteusmoreno.domain.show.ShowEvent;
import br.com.matteusmoreno.domain.show.SongRequest;
import br.com.matteusmoreno.domain.show.constant.RequestStatus;
import br.com.matteusmoreno.domain.show.constant.ShowStatus;
import br.com.matteusmoreno.domain.show.request.MakeSongRequest;
import br.com.matteusmoreno.domain.song.Song;
import br.com.matteusmoreno.domain.song.service.SongService;
import br.com.matteusmoreno.domain.subscription.service.SubscriptionService;
import br.com.matteusmoreno.exception.ShowConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

@ApplicationScoped
public class ShowService {

    private final ArtistService artistService;
    private final SubscriptionService subscriptionService;
    private final ShowWebSocket showWebSocket;
    private final SongService songService;

    public ShowService(ArtistService artistService, SubscriptionService subscriptionService, ShowWebSocket showWebSocket, SongService songService) {
        this.artistService = artistService;
        this.subscriptionService = subscriptionService;
        this.showWebSocket = showWebSocket;
        this.songService = songService;
    }

    public ShowEvent startShow(ObjectId artistId) {
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

    public ShowEvent endShow(ObjectId showId) {
        ShowEvent showEvent = ShowEvent.findById(showId);
        if (showEvent == null || showEvent.status == ShowStatus.FINISHED) throw new ShowConflictException("Show not found or already finished.");

        LocalDateTime endTime = LocalDateTime.now();
        long duration = Duration.between(showEvent.startTime, endTime).toSeconds();

        showEvent.status = ShowStatus.FINISHED;
        showEvent.endTime = endTime;
        showEvent.durationInSeconds = (int) duration;
        showEvent.totalRequests = showEvent.requests.size();

        showEvent.update();

        return showEvent;
    }

    public ShowEvent makeSongRequest(MakeSongRequest request) throws Exception {
        // 1. Encontra o show ativo do artista
        ShowEvent activeShow = ShowEvent.<ShowEvent>find("artistId = ?1 and status = ?2", request.artistId(), ShowStatus.ACTIVE)
                .firstResultOptional()
                .orElseThrow(() -> new ShowConflictException("Artist does not have an active show."));

        // 2. Busca os detalhes da música
        Song song = songService.getSongById(request.songId());

        // --- PONTO DE INTEGRAÇÃO COM PAGAMENTO ---
        // Aqui você chamaria um PaymentService para processar a gorjeta (tipAmount).
        // A lógica abaixo só executaria após a confirmação do pagamento.
        // Por enquanto, vamos simular que o pagamento foi um sucesso.
        // ObjectId paymentId = paymentService.processTip(request);

        // 3. Cria o objeto SongRequest
        SongRequest newSongRequest = new SongRequest();
        newSongRequest.requestId = new ObjectId(); // ID único para o pedido
        newSongRequest.songTitle = song.title;
        newSongRequest.songArtist = song.artistName;
        newSongRequest.tipAmount = request.tipAmount();
        newSongRequest.clientMessage = request.clientMessage();
        newSongRequest.status = RequestStatus.PAID; // Assume que foi pago
        newSongRequest.receivedAt = LocalDateTime.now();
        // newSongRequest.paymentId = paymentId;

        // 4. Adiciona o pedido ao show e atualiza os totais
        activeShow.requests.add(newSongRequest);
        activeShow.totalTipsValue = activeShow.totalTipsValue.add(request.tipAmount());
        activeShow.totalRequests = activeShow.requests.size();

        activeShow.update();

        // 5. Notifica o artista em tempo real via WebSocket
        String jsonMessage = new ObjectMapper().writeValueAsString(newSongRequest);
        showWebSocket.sendToArtist(request.artistId().toString(), jsonMessage);

        return activeShow;
    }

    public ShowEvent updateRequestStatus(ObjectId showId, ObjectId requestId, RequestStatus newStatus) {
        ShowEvent showEvent = ShowEvent.findById(showId);
        if (showEvent == null) {
            throw new ShowConflictException("Show not found.");
        }

        // Encontra o pedido específico dentro da lista de pedidos do show
        SongRequest songRequest = showEvent.requests.stream()
                .filter(req -> req.requestId.equals(requestId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Song request not found in this show.")); // Exceção customizada aqui

        songRequest.status = newStatus;
        showEvent.update();

        return showEvent;
    }

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
}