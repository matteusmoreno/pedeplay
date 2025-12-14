package br.com.matteusmoreno.api.webhook;

import br.com.matteusmoreno.domain.show.ShowEvent;
import br.com.matteusmoreno.domain.show.SongRequest;
import br.com.matteusmoreno.domain.show.constant.RequestStatus;
import br.com.matteusmoreno.domain.show.service.ShowService;
import br.com.matteusmoreno.infrastructure.payment.MercadoPagoService;
import com.mercadopago.resources.payment.Payment;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/webhooks/mercadopago")
@Slf4j
public class MercadoPagoWebhook {

  private static final String STATUS_KEY = "status";
  private static final String PAYMENT_STATUS_KEY = "paymentStatus";
  private static final String ERROR_KEY = "error";
  private static final String HMAC_SHA256 = "HmacSHA256";

  private final MercadoPagoService mercadoPagoService;
  private final ShowService showService;

  @ConfigProperty(name = "app.webhook.secret")
  String webhookSecret;

  public MercadoPagoWebhook(MercadoPagoService mercadoPagoService, ShowService showService) {
    this.mercadoPagoService = mercadoPagoService;
    this.showService = showService;
  }

  /**
   * Webhook do Mercado Pago
   * Recebe notificações quando o status de um pagamento muda
   */
  @POST
  @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
  @Produces(MediaType.APPLICATION_JSON)
  public Response receiveNotification(
      @HeaderParam("x-signature") String signature,
      @HeaderParam("x-request-id") String requestId,
      @QueryParam("topic") String topic,
      @QueryParam("id") Long queryId,
      @QueryParam("data.id") Long dataId,
      @QueryParam("resource") Long resource,
      Map<String, Object> body) {

    // Extrai o payment ID de qualquer fonte (query params ou body JSON)
    Long paymentId = extractPaymentId(queryId, dataId, resource, body);

    if (paymentId == null) {
      log.warn("⚠️ Webhook recebido sem payment ID");
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(Map.of(ERROR_KEY, "Missing payment ID"))
          .build();
    }

    log.info("📥 Webhook recebido - Payment ID: {}, Topic: {}", paymentId, topic);

    // Valida assinatura do Mercado Pago
    if (!isValidSignature(signature, requestId, paymentId)) {
      log.warn("⚠️ Assinatura inválida - Payment ID: {}", paymentId);
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(Map.of(ERROR_KEY, "Invalid signature"))
          .build();
    }

    return processPaymentNotification(paymentId, topic);
  }

  /**
   * Extrai o payment ID de diferentes fontes (query params ou JSON body)
   */
  private Long extractPaymentId(Long queryId, Long dataId, Long resource, Map<String, Object> body) {
    // Tenta query parameters primeiro
    if (queryId != null) return queryId;
    if (dataId != null) return dataId;
    if (resource != null) return resource;

    // Tenta extrair do body JSON
    if (body != null && body.containsKey("data")) {
      Object data = body.get("data");
      if (data instanceof Map) {
        Object id = ((Map<?, ?>) data).get("id");
        if (id != null) {
          return Long.parseLong(id.toString());
        }
      }
    }

    return null;
  }

  /**
   * Processa a notificação de pagamento
   */
  private Response processPaymentNotification(Long paymentId, String topic) {
    // Ignora notificações que não são de pagamento
    if (topic != null && !"payment".equals(topic)) {
      log.debug("ℹ️ Ignorando notificação não relacionada a pagamento: {}", topic);
      return Response.ok(Map.of("message", "Ignored")).build();
    }

    try {
      // Consulta o status atual no Mercado Pago
      Payment payment = mercadoPagoService.getPayment(paymentId);
      String status = payment.getStatus();

      log.info("💳 Payment {} - Status: {}", paymentId, status);

      // Processa de acordo com o status
      if ("approved".equals(status)) {
        processApprovedPayment(String.valueOf(payment.getId()), payment.getTransactionAmount());
        return Response.ok(Map.of(STATUS_KEY, "processed", PAYMENT_STATUS_KEY, "approved")).build();
      }

      if ("rejected".equals(status) || "cancelled".equals(status)) {
        processRejectedPayment(String.valueOf(payment.getId()));
        return Response.ok(Map.of(STATUS_KEY, "processed", PAYMENT_STATUS_KEY, status)).build();
      }

      // Status intermediário (pending, in_process, etc)
      return Response.ok(Map.of(STATUS_KEY, "pending", PAYMENT_STATUS_KEY, status)).build();

    } catch (Exception e) {
      log.error("❌ Erro ao processar webhook - Payment ID: {}", paymentId, e);
      return Response.ok(Map.of(STATUS_KEY, "error", "message", "Processing error")).build();
    }
  }

  /**
   * Processa pagamento aprovado pelo Mercado Pago
   */
  private void processApprovedPayment(String paymentId, java.math.BigDecimal amount) {
    log.info("✅ Pagamento aprovado: {} - R$ {}", paymentId, amount);

    // Busca o show que contém este pagamento
    ShowEvent showEvent = ShowEvent.find("requests.paymentId", paymentId).firstResult();
    if (showEvent == null) {
      log.warn("⚠️ Show não encontrado para pagamento: {}", paymentId);
      return;
    }

    // Busca o pedido específico dentro do show
    SongRequest songRequest = showEvent.requests.stream()
        .filter(r -> paymentId.equals(r.paymentId))
        .findFirst()
        .orElse(null);

    if (songRequest == null) {
      log.warn("⚠️ Pedido não encontrado para pagamento: {}", paymentId);
      return;
    }

    // Verifica se já foi processado (idempotência)
    if (songRequest.status != RequestStatus.PENDING_PAYMENT) {
      log.info("ℹ️ Pagamento {} já processado. Status: {}", paymentId, songRequest.status);
      return;
    }

    // Atualiza status e libera o pedido na fila
    songRequest.status = RequestStatus.PENDING;

    // Atualiza totais do show
    showEvent.totalTipsValue = showEvent.totalTipsValue.add(songRequest.tipAmount);
    showEvent.totalRequests = (int) showEvent.requests.stream()
        .filter(r -> r.status != RequestStatus.PENDING_PAYMENT && r.status != RequestStatus.REJECTED)
        .count();

    showEvent.update();

    // Notifica o artista via WebSocket
    showService.notifyArtistAndPublic(showEvent.artistId, showEvent, songRequest);

    log.info("🎵 Pedido liberado: {} - Pagamento: {}", songRequest.songTitle, paymentId);
  }

  /**
   * Processa pagamento rejeitado ou cancelado
   */
  private void processRejectedPayment(String paymentId) {
    log.info("❌ Pagamento rejeitado: {}", paymentId);

    ShowEvent showEvent = ShowEvent.find("requests.paymentId", paymentId).firstResult();
    if (showEvent == null) {
      log.warn("⚠️ Show não encontrado para pagamento rejeitado: {}", paymentId);
      return;
    }

    // Marca o pedido como rejeitado
    showEvent.requests.stream()
        .filter(r -> paymentId.equals(r.paymentId))
        .findFirst()
        .ifPresent(req -> {
          if (req.status == RequestStatus.PENDING_PAYMENT) {
            req.status = RequestStatus.REJECTED;
            showEvent.update();
            log.info("🚫 Pedido rejeitado: {} - Pagamento: {}", req.songTitle, paymentId);
          }
        });
  }

  /**
   * Valida a assinatura do webhook do Mercado Pago
   */
  private boolean isValidSignature(String signature, String requestId, Long paymentId) {
    // Em desenvolvimento sem secret, permite webhook
    if (webhookSecret == null || webhookSecret.isBlank()) {
      log.warn("⚠️ Webhook secret não configurado - Validação desabilitada");
      return true;
    }

    // Se não houver assinatura, rejeita
    if (signature == null || signature.isBlank()) {
      log.warn("⚠️ Webhook sem assinatura");
      return false;
    }

    try {
      // Extrai timestamp e hash da assinatura: ts=1234567890,v1=abc123...
      String[] parts = signature.split(",");
      String ts = null;
      String hash = null;

      for (String part : parts) {
        String[] keyValue = part.trim().split("=", 2);
        if (keyValue.length == 2) {
          if ("ts".equals(keyValue[0])) ts = keyValue[1];
          else if ("v1".equals(keyValue[0])) hash = keyValue[1];
        }
      }

      if (ts == null || hash == null) {
        log.warn("⚠️ Formato de assinatura inválido");
        return false;
      }

      // Testa diferentes formatos de manifest que o MP pode usar
      String dataId = String.valueOf(paymentId);

      // Formato principal: id:X;request-id:Y;ts:Z;
      String manifest = String.format("id:%s;request-id:%s;ts:%s;", dataId, requestId, ts);
      String calculatedHash = calculateHMAC(manifest);

      if (hash.equals(calculatedHash)) {
        log.info("✅ Assinatura válida - Payment {}", paymentId);
        return true;
      }

      // Tenta formatos alternativos (sem request-id, formato novo, etc)
      String[] alternativeFormats = {
          String.format("id=%s&request_id=%s&ts=%s", dataId, requestId, ts),
          String.format("id:%s;ts:%s;", dataId, ts),
          String.format("id=%s&ts=%s", dataId, ts)
      };

      for (String altManifest : alternativeFormats) {
        if (hash.equals(calculateHMAC(altManifest))) {
          log.info("✅ Assinatura válida (formato alternativo) - Payment {}", paymentId);
          return true;
        }
      }

      log.warn("❌ Assinatura inválida - Payment {}", paymentId);
      return false;

    } catch (Exception e) {
      log.error("❌ Erro ao validar assinatura", e);
      return false;
    }
  }

  /**
   * Calcula HMAC SHA256 de um manifest
   */
  private String calculateHMAC(String manifest) {
    try {
      javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA256");
      javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
          webhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
          "HmacSHA256"
      );
      hmac.init(secretKey);
      byte[] hashBytes = hmac.doFinal(manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8));

      // Converte para hex
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception e) {
      log.error("❌ Erro ao calcular HMAC", e);
      return "";
    }
  }
}