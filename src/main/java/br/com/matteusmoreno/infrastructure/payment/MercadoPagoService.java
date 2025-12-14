package br.com.matteusmoreno.infrastructure.payment;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.resources.payment.Payment;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@Slf4j
public class MercadoPagoService {

  @ConfigProperty(name = "mercadopago.access-token")
  String accessToken;

  // Inicializa o SDK
  void onStart(@Observes StartupEvent ev) {
    MercadoPagoConfig.setAccessToken(accessToken);
    log.info("✅ Mercado Pago SDK inicializado");
  }

  public Map<String, Object> createPixPayment(BigDecimal amount, String description, String email, String notificationUrl) {
    try {
      log.info("🔄 Criando pagamento PIX - Valor: R$ {} - Email: {}", amount, email);

      PaymentClient client = new PaymentClient();

      // Define expiração de 30 minutos para o PIX
      java.time.OffsetDateTime expirationDate = java.time.OffsetDateTime.now().plusMinutes(30);

      PaymentCreateRequest request = PaymentCreateRequest.builder()
          .transactionAmount(amount)
          .description(description)
          .paymentMethodId("pix")
          .dateOfExpiration(expirationDate)
          .payer(PaymentPayerRequest.builder()
              .email(email)
              .build())
          .notificationUrl(notificationUrl) // URL do seu webhook
          .build();

      Payment payment = client.create(request);

      Map<String, Object> response = new HashMap<>();
      response.put("paymentId", payment.getId());
      response.put("status", payment.getStatus());
      response.put("expirationDate", payment.getDateOfExpiration());

      // Extrai dados do QR Code
      if (payment.getPointOfInteraction() != null &&
          payment.getPointOfInteraction().getTransactionData() != null) {

        response.put("qrCodeBase64", payment.getPointOfInteraction().getTransactionData().getQrCodeBase64());
        response.put("qrCodeCopyPaste", payment.getPointOfInteraction().getTransactionData().getQrCode());
        response.put("ticketUrl", payment.getPointOfInteraction().getTransactionData().getTicketUrl());

        log.info("✅ Pagamento PIX criado com sucesso - ID: {} - Expira em: {}",
                 payment.getId(), payment.getDateOfExpiration());
      } else {
        log.warn("⚠️ QR Code não disponível no pagamento {}", payment.getId());
      }

      return response;

    } catch (Exception e) {
      log.error("❌ Erro ao criar pagamento PIX no Mercado Pago", e);
      throw new RuntimeException("Erro ao criar pagamento PIX no Mercado Pago: " + e.getMessage(), e);
    }
  }

  // Método para consultar status depois (usado no webhook)
  public Payment getPayment(Long id) {
    try {
      log.debug("🔍 Consultando pagamento {}", id);
      PaymentClient client = new PaymentClient();
      Payment payment = client.get(id);
      log.debug("📊 Status do pagamento {}: {}", id, payment.getStatus());
      return payment;
    } catch (Exception e) {
      log.error("❌ Erro ao buscar pagamento {}", id, e);
      throw new RuntimeException("Erro ao buscar pagamento: " + e.getMessage(), e);
    }
  }
}