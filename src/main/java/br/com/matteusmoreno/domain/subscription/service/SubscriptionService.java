package br.com.matteusmoreno.domain.subscription.service;

import br.com.matteusmoreno.domain.subscription.MonthlyUsage;
import br.com.matteusmoreno.domain.subscription.Subscription;
import br.com.matteusmoreno.domain.subscription.constant.PlanType;
import br.com.matteusmoreno.domain.subscription.constant.SubscriptionStatus;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;

@ApplicationScoped
public class SubscriptionService {

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
}
