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
import br.com.matteusmoreno.infrastructure.payment.MercadoPagoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Slf4j
public class ShowService {

  private final ArtistService artistService;
  private final SubscriptionService subscriptionService;
  private final ShowWebSocket showWebSocket;
  private final SongService songService;
  private final ObjectMapper objectMapper;
  private final MercadoPagoService mercadoPagoService;

  public ShowService(ArtistService artistService, SubscriptionService subscriptionService,
      ShowWebSocket showWebSocket, SongService songService,
      ObjectMapper objectMapper, MercadoPagoService mercadoPagoService) {
    this.artistService = artistService;
    this.subscriptionService = subscriptionService;
    this.showWebSocket = showWebSocket;
    this.songService = songService;
    this.objectMapper = objectMapper;
    this.mercadoPagoService = mercadoPagoService;
  }

  @ConfigProperty(name = "app.webhook.url") // Defina isso no properties (ex: https://seudominio.com/api/webhooks/mercadopago)
  String webhookUrl;

  // INICIA MODO SHOW
  public ShowEvent startShow(ObjectId artistId, String loggedInArtistId) {
    if (!artistId.equals(new ObjectId(loggedInArtistId))) {
      throw new ForbiddenException("You can only start a show for yourself.");
    }
    Artist artist = artistService.getArtistById(artistId);

    long activeShows = ShowEvent.count("artistId = ?1 and status = ?2", artist.id,
        ShowStatus.ACTIVE);
    if (activeShows > 0) {
      throw new ShowConflictException("Artist already has an active show.");
    }

    // Verifica e incrementa o limite do plano
    subscriptionService.verifyAndIncrementEventUsage(artist);
    ShowEvent showEvent = createNewShowEvent(artist.id);

    ShowEvent.persist(showEvent);
    artist.update();

    // 📣 REATIVO: Notifica via WebSocket que show iniciou
    showWebSocket.notifyShowStarted(artist.id.toString(), showEvent.id.toString());
    log.info("📣 Show {} iniciado - Notificação enviada via WebSocket", showEvent.id);

    return showEvent;
  }

  // FINALIZA MODO SHOW
  public ShowEvent endShow(ObjectId showId, String loggedInArtistId) {
    ShowEvent showEvent = ShowEvent.findById(showId);
    if (showEvent == null || showEvent.status == ShowStatus.FINISHED) {
      throw new ShowConflictException("Show not found or already finished.");
    }

    if (!showEvent.artistId.equals(new ObjectId(loggedInArtistId))) {
      throw new ForbiddenException("You can only end your own show.");
    }

    LocalDateTime endTime = LocalDateTime.now();
    long duration = Duration.between(showEvent.startTime, endTime).toSeconds();

    showEvent.status = ShowStatus.FINISHED;
    showEvent.endTime = endTime;
    showEvent.durationInSeconds = (int) duration;
    showEvent.totalRequests = showEvent.requests.size();

    showEvent.update();

    // 📣 REATIVO: Notifica via WebSocket que show terminou
    showWebSocket.notifyShowEnded(showEvent.artistId.toString(), showEvent.id.toString());
    log.info("📣 Show {} encerrado - Notificação enviada via WebSocket", showEvent.id);

    return showEvent;
  }

  // FAZ PEDIDO DE MÚSICA
  public Map<String, Object> makeSongRequest(MakeSongRequest request) throws Exception {
    Artist artist = artistService.getArtistById(request.artistId());
    ShowEvent activeShow = ShowEvent.<ShowEvent>find("artistId = ?1 and status = ?2", request.artistId(), ShowStatus.ACTIVE)
        .firstResultOptional()
        .orElseThrow(() -> new ShowConflictException("Artist does not have an active show."));

    subscriptionService.verifyAndIncrementRequestUsage(artist);

    BigDecimal tipAmount = Objects.requireNonNullElse(request.tipAmount(), BigDecimal.ZERO);
    SongRequest newSongRequest = createSongRequest(request);

    // LÓGICA DE PAGAMENTO
    Map<String, Object> paymentData = Collections.emptyMap();

    if (tipAmount.compareTo(BigDecimal.ZERO) > 0) {
      // 1. Define status como AGUARDANDO PAGAMENTO
      newSongRequest.status = RequestStatus.PENDING_PAYMENT;

      // 2. Cria o PIX no Mercado Pago
      // Obs: O email deveria vir do usuário logado ou do request. Usando placeholder por enquanto.
      String payerEmail = "cliente@email.com";
      paymentData = mercadoPagoService.createPixPayment(
          tipAmount,
          "Gorjeta - Pedido de Música",
          payerEmail,
          webhookUrl
      );

      // 3. Salva o ID do pagamento no pedido
      Long mpPaymentId = (Long) paymentData.get("paymentId");
      newSongRequest.paymentId = String.valueOf(mpPaymentId);
      // O pedido é salvo, mas NÃO notifica o artista ainda via WebSocket
    } else {
      // Fluxo Gratuito Normal
      newSongRequest.status = RequestStatus.PENDING;
    }

    activeShow.requests.add(newSongRequest);
    // Só soma nos totais se já estiver PENDING (gratuito). Se for pago, soma no webhook.
    if (newSongRequest.status == RequestStatus.PENDING) {
      activeShow.totalRequests = activeShow.requests.size(); // Recalcula
    }

    activeShow.update();
    artist.update();

    // NOTIFICAÇÕES
    // Só notifica se for gratuito. Se for pago, a notificação sai no Webhook após confirmação.
    if (newSongRequest.status == RequestStatus.PENDING) {
      notifyArtistAndPublic(request.artistId(), activeShow, newSongRequest);
    }

    // Retorna dados do show + dados do pagamento (QR Code) se houver
    Map<String, Object> result = new HashMap<>();
    result.put("show", activeShow); // Ou ShowDetailsResponse
    result.put("payment", paymentData);
    return result;
  }

  // Extrai o método de notificação para reutilizar no Webhook
  public void notifyArtistAndPublic(ObjectId artistId, ShowEvent activeShow, SongRequest request) {
    try {
      NewSongRequestNotification notification = new NewSongRequestNotification(request);
      String jsonMessage = objectMapper.writeValueAsString(notification);
      showWebSocket.sendToArtist(artistId.toString(), jsonMessage);
    } catch (Exception e) {
      log.error("Erro websocket artista", e);
    }

    showWebSocket.notifyNewSongRequest(
        artistId.toString(),
        activeShow.id.toString(),
        request.requestId.toString()
    );
  }

  // DETALHES DO SHOW POR ID
  public ShowEvent getShowEventById(ObjectId showId) {
    ShowEvent showEvent = ShowEvent.findById(showId);
    if (showEvent == null) {
      throw new ShowEventNotFoundException("Show not found with ID: ".concat(showId.toString()));
    }

    return showEvent;
  }

  // LISTAGEM DE TODOS OS SHOWS DE UM ARTISTA
  public List<ShowEvent> getAllShowsByArtist(int page, int size, ObjectId artistId,
      String loggedInArtistId) {
    if (!artistId.equals(new ObjectId(loggedInArtistId))) {
      throw new ForbiddenException("You can only view your own shows.");
    }

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
  public ShowEvent updateRequestStatus(ObjectId showId, ObjectId requestId,
      UpdateRequestStatus request, String loggedInArtistId) {
    ShowEvent showEvent = getShowEventById(showId);

    if (!showEvent.artistId.equals(new ObjectId(loggedInArtistId))) {
      throw new ForbiddenException("You can only update requests for your own show.");
    }

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

    // 📣 REATIVO: Notifica artista sobre atualização de status
    try {
      RequestStatusUpdateNotification notification = new RequestStatusUpdateNotification(requestId,
          newStatus);
      String jsonMessage = new ObjectMapper().writeValueAsString(notification);
      showWebSocket.sendToArtist(showEvent.artistId.toString(), jsonMessage);
      log.info("📣 Status do pedido {} atualizado - Notificação enviada para artista", requestId);
    } catch (Exception e) {
      log.error(
          "Falha ao enviar notificação WebSocket de atualização de status para o artista {}: {}",
          showEvent.artistId, e.getMessage());
    }

    // 📣 REATIVO: Notifica TODOS (página pública) sobre mudança na fila
    showWebSocket.notifyRequestStatusUpdated(
        showEvent.artistId.toString(),
        showEvent.id.toString(),
        requestId.toString(),
        newStatus.toString()
    );
    log.info("📣 Notificação de status enviada para página pública do show {}", showEvent.id);

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