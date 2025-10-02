package br.com.matteusmoreno.domain.subscription.service;

import br.com.matteusmoreno.domain.artist.Artist;
import br.com.matteusmoreno.domain.subscription.MonthlyUsage;
import br.com.matteusmoreno.domain.subscription.Subscription;
import br.com.matteusmoreno.domain.subscription.constant.PlanType;
import br.com.matteusmoreno.domain.subscription.constant.SubscriptionStatus;
import br.com.matteusmoreno.exception.SubscriptionLimitExceededException;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;

@ApplicationScoped
public class SubscriptionService {

    private final PlanService planService;

    public SubscriptionService(PlanService planService) {
        this.planService = planService;
    }

    public Subscription createFreeSubscription() {
        return Subscription.builder()
                .planType(PlanType.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .nextBillingDate(LocalDateTime.now().plusMonths(1))
                .monthlyUsage(createMonthlyUsage())
                .build();
    }

    public Subscription createPremiumSubscription() {
        return Subscription.builder()
                .planType(PlanType.PREMIUM)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .nextBillingDate(LocalDateTime.now().plusMonths(1))
                .monthlyUsage(createMonthlyUsage())
                .build();
    }

    public Subscription createEnterpriseSubscription() {
        return Subscription.builder()
                .planType(PlanType.ENTERPRISE)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .nextBillingDate(LocalDateTime.now().plusMonths(1))
                .monthlyUsage(createMonthlyUsage())
                .build();
    }

    private MonthlyUsage createMonthlyUsage() {
        return MonthlyUsage.builder()
                .eventsUsed(0)
                .requestsReceived(0)
                .lastResetDate(LocalDateTime.now())
                .build();
    }

    public void verifyAndIncrementEventUsage(Artist artist) {
        Subscription subscription = artist.subscription;
        if (subscription == null || subscription.monthlyUsage == null) {
            throw new IllegalStateException("Artista sem assinatura configurada.");
        }

        int limit = planService.getEventLimitForPlan(subscription.planType);
        int currentUsage = subscription.monthlyUsage.eventsUsed;

        if (currentUsage >= limit) {
            throw new SubscriptionLimitExceededException("Limite mensal de eventos (" + limit + ") atingido para o plano " + subscription.planType);
        }

        // Se a verificação passar, apenas incrementa o contador.
        // A persistência será feita no ArtistService depois.
        subscription.monthlyUsage.eventsUsed++;
    }

    public void verifyAndIncrementRequestUsage(Artist artist) {
        Subscription subscription = artist.subscription;
        if (subscription == null || subscription.monthlyUsage == null) {
            throw new IllegalStateException("Artista sem assinatura configurada.");
        }

        int limit = planService.getRequestLimitForPlan(subscription.planType);
        int currentUsage = subscription.monthlyUsage.requestsReceived;

        if (currentUsage >= limit) {
            throw new SubscriptionLimitExceededException("Limite mensal de solicitações (" + limit + ") atingido para o plano " + subscription.planType);
        }

        // Se a verificação passar, apenas incrementa o contador.
        // A persistência será feita no ArtistService depois.
        subscription.monthlyUsage.requestsReceived++;
    }
}
