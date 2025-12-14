package br.com.matteusmoreno.infrastructure.payment;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.resources.payment.Payment;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class MercadoPagoService {

  @ConfigProperty(name = "mercadopago.access-token")
  String accessToken;

  // Inicializa o SDK
  void onStart(@Observes StartupEvent ev) {
    MercadoPagoConfig.setAccessToken(accessToken);
  }

  public Map<String, Object> createPixPayment(BigDecimal amount, String description, String email, String notificationUrl) {
    try {
      PaymentClient client = new PaymentClient();

      PaymentCreateRequest request = PaymentCreateRequest.builder()
          .transactionAmount(amount)
          .description(description)
          .paymentMethodId("pix")
          .payer(PaymentPayerRequest.builder()
              .email(email)
              .build())
          .notificationUrl(notificationUrl) // URL do seu webhook (ngrok em dev)
          .build();

      Payment payment = client.create(request);

      Map<String, Object> response = new HashMap<>();
      response.put("paymentId", payment.getId());
      response.put("status", payment.getStatus());

      // Extrai dados do QR Code
      if (payment.getPointOfInteraction() != null &&
          payment.getPointOfInteraction().getTransactionData() != null) {

        response.put("qrCodeBase64", payment.getPointOfInteraction().getTransactionData().getQrCodeBase64());
        response.put("qrCodeCopyPaste", payment.getPointOfInteraction().getTransactionData().getQrCode());
        response.put("ticketUrl", payment.getPointOfInteraction().getTransactionData().getTicketUrl());
      }

      return response;

    } catch (Exception e) {
      throw new RuntimeException("Erro ao criar pagamento PIX no Mercado Pago", e);
    }
  }

  // Método para consultar status depois (usado no webhook)
  public Payment getPayment(Long id) {
    try {
      PaymentClient client = new PaymentClient();
      return client.get(id);
    } catch (Exception e) {
      throw new RuntimeException("Erro ao buscar pagamento", e);
    }
  }
}