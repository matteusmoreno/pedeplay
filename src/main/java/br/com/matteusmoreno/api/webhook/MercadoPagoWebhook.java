package br.com.matteusmoreno.api.webhook;

import br.com.matteusmoreno.domain.show.ShowEvent;
import br.com.matteusmoreno.domain.show.SongRequest;
import br.com.matteusmoreno.domain.show.constant.RequestStatus;
import br.com.matteusmoreno.domain.show.service.ShowService;
import br.com.matteusmoreno.infrastructure.payment.MercadoPagoService;
import com.mercadopago.resources.payment.Payment;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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
   * Webhook do Mercado Pago - Suporta formatos antigo e novo
   * Formato antigo: POST com query params ?topic=payment&id=123
   * Formato novo: POST com JSON body {"action": "payment.updated", "data": {"id": "123"}}
   */
  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response receiveNotificationUrlEncoded(
      @HeaderParam("x-signature") String signature,
      @HeaderParam("x-request-id") String requestId,
      @QueryParam("topic") String topic,
      @QueryParam("id") Long id,
      @QueryParam("data.id") Long dataId) {

    // Valida assinatura se configurada
    if (!validateSignature(signature, requestId, String.valueOf(id != null ? id : dataId))) {
      log.warn("⚠️ Assinatura inválida no webhook - Possível tentativa de fraude!");
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(Map.of(ERROR_KEY, "Invalid signature"))
          .build();
    }

    return processNotification(topic, id, dataId, null);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response receiveNotificationJson(
      @HeaderParam("x-signature") String signature,
      @HeaderParam("x-request-id") String requestId,
      @QueryParam("topic") String topic,
      @QueryParam("id") Long queryId,
      Map<String, Object> body) {

    log.info("📥 Webhook JSON recebido - Body: {}", body);

    Long bodyId = null;
    if (body != null && body.containsKey("data")) {
      Object data = body.get("data");
      if (data instanceof Map) {
        Object id = ((Map<?, ?>) data).get("id");
        if (id != null) {
          bodyId = Long.parseLong(id.toString());
        }
      }
    }

    // Valida assinatura se configurada
    Long paymentId = queryId != null ? queryId : bodyId;
    if (!validateSignature(signature, requestId, String.valueOf(paymentId))) {
      log.warn("⚠️ Assinatura inválida no webhook JSON - Possível tentativa de fraude!");
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(Map.of(ERROR_KEY, "Invalid signature"))
          .build();
    }

    return processNotification(topic, queryId, null, bodyId);
  }

  private Response processNotification(String topic, Long id, Long dataId, Long bodyId) {
    log.info("📥 Webhook recebido - Topic: {}, ID: {}, Data.ID: {}, Body.ID: {}",
             topic, id, dataId, bodyId);

    // Tenta obter o ID de qualquer fonte
    Long paymentId;
    if (id != null) {
      paymentId = id;
    } else if (dataId != null) {
      paymentId = dataId;
    } else {
      paymentId = bodyId;
    }

    if (paymentId == null) {
      log.warn("⚠️ Webhook sem ID de pagamento");
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(Map.of(ERROR_KEY, "Missing payment ID"))
          .build();
    }

    // Ignora notificações que não são de pagamento
    if (topic != null && !"payment".equals(topic)) {
      log.debug("ℹ️ Ignorando notificação não relacionada a pagamento: {}", topic);
      return Response.ok(Map.of("message", "Ignored non-payment notification")).build();
    }

    try {
      // 1. Consulta o status atual no Mercado Pago
      Payment payment = mercadoPagoService.getPayment(paymentId);

      log.info("💳 Status do pagamento {}: {}", paymentId, payment.getStatus());

      if ("approved".equals(payment.getStatus())) {
        processApprovedPayment(String.valueOf(payment.getId()), payment.getTransactionAmount());
        return Response.ok(Map.of(STATUS_KEY, "processed", PAYMENT_STATUS_KEY, "approved")).build();
      } else if ("rejected".equals(payment.getStatus()) || "cancelled".equals(payment.getStatus())) {
        processRejectedPayment(String.valueOf(payment.getId()));
        return Response.ok(Map.of(STATUS_KEY, "processed", PAYMENT_STATUS_KEY, payment.getStatus())).build();
      } else {
        log.info("ℹ️ Status intermediário: {} - Aguardando confirmação", payment.getStatus());
        return Response.ok(Map.of(STATUS_KEY, "pending", PAYMENT_STATUS_KEY, payment.getStatus())).build();
      }

    } catch (Exception e) {
      log.error("❌ Erro ao processar webhook do Mercado Pago - Payment ID: {}", paymentId, e);
      // Retorna 200 mesmo com erro para evitar reenvios infinitos do MP
      return Response.ok(Map.of(STATUS_KEY, "error", "message", "Internal processing error")).build();
    }
  }

  private void processApprovedPayment(String paymentId, java.math.BigDecimal amount) {
    log.info("✅ Processando pagamento aprovado: {} - Valor: R$ {}", paymentId, amount);

    // Query no MongoDB para achar o ShowEvent que contém um request com este paymentId
    ShowEvent showEvent = ShowEvent.find("requests.paymentId", paymentId).firstResult();

    if (showEvent == null) {
      log.warn("⚠️ Nenhum ShowEvent encontrado para o paymentId: {}", paymentId);
      return;
    }

    // Achar o pedido específico dentro da lista
    SongRequest songRequest = showEvent.requests.stream()
        .filter(r -> paymentId.equals(r.paymentId))
        .findFirst()
        .orElse(null);

    if (songRequest == null) {
      log.warn("⚠️ Nenhum SongRequest encontrado com paymentId: {}", paymentId);
      return;
    }

    // ✅ IDEMPOTÊNCIA: Verifica se já foi processado
    if (songRequest.status != RequestStatus.PENDING_PAYMENT) {
      log.info("ℹ️ Pagamento {} já foi processado anteriormente. Status atual: {}",
               paymentId, songRequest.status);
      return;
    }

    // Atualiza o status do pedido
    songRequest.status = RequestStatus.PENDING; // Libera na fila

    // Atualiza totais do show
    showEvent.totalTipsValue = showEvent.totalTipsValue.add(songRequest.tipAmount);
    showEvent.totalRequests = (int) showEvent.requests.stream()
        .filter(r -> r.status != RequestStatus.PENDING_PAYMENT &&
                     r.status != RequestStatus.REJECTED)
        .count();

    showEvent.update();

    // Notifica o Artista agora que o pagamento foi confirmado
    showService.notifyArtistAndPublic(showEvent.artistId, showEvent, songRequest);

    log.info("🎵 Pagamento confirmado! Pedido liberado: {} - {}",
             songRequest.songTitle, paymentId);
  }

  private void processRejectedPayment(String paymentId) {
    log.info("❌ Processando pagamento rejeitado/cancelado: {}", paymentId);

    ShowEvent showEvent = ShowEvent.find("requests.paymentId", paymentId).firstResult();

    if (showEvent == null) {
      log.warn("⚠️ Nenhum ShowEvent encontrado para o paymentId rejeitado: {}", paymentId);
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
            log.info("🚫 Pedido marcado como rejeitado: {} - {}", req.songTitle, paymentId);
          }
        });
  }

  /**
   * Valida a assinatura do webhook do Mercado Pago
   * Documentação: https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks#editor_3
   */
  private boolean validateSignature(String signature, String requestId, String dataId) {
    // Se não houver secret configurado, permite (modo desenvolvimento)
    if (webhookSecret == null || webhookSecret.isBlank()) {
      log.warn("⚠️ Webhook secret não configurado - Validação de assinatura desabilitada (INSEGURO!)");
      return true;
    }

    // Se não houver assinatura, rejeita
    if (signature == null || signature.isBlank()) {
      log.warn("⚠️ Webhook sem assinatura - Rejeitado");
      return false;
    }

    try {
      // Extrai as partes da assinatura: ts=timestamp,v1=hash
      String[] parts = signature.split(",");
      String ts = null;
      String hash = null;

      for (String part : parts) {
        String[] keyValue = part.trim().split("=", 2);
        if (keyValue.length == 2) {
          if ("ts".equals(keyValue[0])) {
            ts = keyValue[1];
          } else if ("v1".equals(keyValue[0])) {
            hash = keyValue[1];
          }
        }
      }

      if (ts == null || hash == null) {
        log.warn("⚠️ Formato de assinatura inválido: {}", signature);
        return false;
      }

      // Monta o manifest: id + requestId + timestamp
      String manifest = String.format("id:%s;request-id:%s;ts:%s;", dataId, requestId, ts);

      // Calcula HMAC SHA256
      Mac hmac = Mac.getInstance(HMAC_SHA256);
      SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
      hmac.init(secretKey);
      byte[] hashBytes = hmac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));

      // Converte para hex
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      String calculatedHash = hexString.toString();

      // Compara hashes
      boolean isValid = calculatedHash.equals(hash);

      if (isValid) {
        log.debug("✅ Assinatura válida - Webhook autenticado");
      } else {
        log.warn("⚠️ Hash não corresponde - Esperado: {}, Recebido: {}", calculatedHash, hash);
      }

      return isValid;

    } catch (Exception e) {
      log.error("❌ Erro ao validar assinatura do webhook", e);
      return false;
    }
  }
}