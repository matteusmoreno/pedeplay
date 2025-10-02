package br.com.matteusmoreno.domain.subscription.service;

import br.com.matteusmoreno.domain.artist.Artist;
import br.com.matteusmoreno.domain.subscription.constant.SubscriptionStatus;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
@Slf4j
public class SubscriptionScheduler {

    @Scheduled(cron = "0 0 0 * * ?", timeZone = "America/Sao_Paulo")
    @Transactional
    void processDailySubscriptionResets() {
        LocalDateTime now = LocalDateTime.now();
        log.info("JOB: Iniciando processo de renovação diária de assinaturas às {}", now);

        // 1. Busca todos os artistas cuja próxima data de "cobrança" (renovação) já passou.
        List<Artist> artistsToRenew = Artist.list(
                "subscription.status = ?1 and subscription.nextBillingDate <= ?2",
                SubscriptionStatus.ACTIVE,
                now
        );

        log.info("JOB: Encontrados {} artistas para renovar.", artistsToRenew.size());

        for (Artist artist : artistsToRenew) {
            // 2. Zera os contadores de uso mensal.
            artist.subscription.monthlyUsage.eventsUsed = 0;
            artist.subscription.monthlyUsage.requestsReceived = 0;
            artist.subscription.monthlyUsage.lastResetDate = now;

            // 3. IMPORTANTE: Agenda a próxima renovação para daqui a um mês.
            artist.subscription.nextBillingDate = artist.subscription.nextBillingDate.plusMonths(1);

            // 4. Salva as alterações no banco de dados.
            artist.update();

            log.info("JOB: Assinatura renovada para o artista ID: {}", artist.id);
        }

        log.info("JOB: Processo de renovação diária finalizado às {}", LocalDateTime.now());
    }
}