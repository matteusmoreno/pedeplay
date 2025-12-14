package br.com.matteusmoreno.api.webhook;

import br.com.matteusmoreno.domain.show.ShowEvent;
import br.com.matteusmoreno.domain.show.SongRequest;
import br.com.matteusmoreno.domain.show.constant.RequestStatus;
import br.com.matteusmoreno.domain.show.service.ShowService;
import br.com.matteusmoreno.infrastructure.payment.MercadoPagoService;
import com.mercadopago.resources.payment.Payment;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

@Path("/webhooks/mercadopago")
public class MercadoPagoWebhook {

  @Inject
  MercadoPagoService mercadoPagoService;

  @Inject
  ShowService showService;

  @POST
  public Response receiveNotification(@QueryParam("topic") String topic, @QueryParam("id") Long id) {
    if ("payment".equals(topic) || id != null) {
      try {
        // 1. Consulta o status atual no Mercado Pago
        Payment payment = mercadoPagoService.getPayment(id);

        if ("approved".equals(payment.getStatus())) {
          processApprovedPayment(String.valueOf(payment.getId()), payment.getTransactionAmount());
        }
      } catch (Exception e) {
        e.printStackTrace(); // Logar erro corretamente
      }
    }
    return Response.ok().build();
  }

  private void processApprovedPayment(String paymentId, java.math.BigDecimal amount) {
    // 1. Achar o ShowEvent que tem esse pedido
    // Como o SongRequest está dentro do ShowEvent (embedded), precisamos buscar qual ShowEvent contem o request com esse paymentId.
    // OBS: Se mudou paymentId para String no SongRequest, use String. Se não, adapte.

    // Query no MongoDB para achar o ShowEvent que contém um request com este paymentId
    ShowEvent showEvent = ShowEvent.find("requests.paymentId", paymentId).firstResult();

    if (showEvent != null) {
      // 2. Achar o pedido específico dentro da lista e atualizar
      showEvent.requests.stream()
          .filter(r -> paymentId.equals(r.paymentId)) // Assumindo String
          .findFirst()
          .ifPresent(req -> {
            if (req.status == RequestStatus.PENDING_PAYMENT) {
              req.status = RequestStatus.PENDING; // Libera na fila

              // Atualiza totais do show
              showEvent.totalTipsValue = showEvent.totalTipsValue.add(req.tipAmount);
              showEvent.totalRequests = (int) showEvent.requests.stream()
                  .filter(r -> r.status != RequestStatus.PENDING_PAYMENT).count();

              showEvent.update();

              // 3. Notifica o Artista agora que pagou
              showService.notifyArtistAndPublic(showEvent.artistId, showEvent, req);

              System.out.println("Pagamento confirmado para pedido: " + req.songTitle);
            }
          });
    }
  }
}