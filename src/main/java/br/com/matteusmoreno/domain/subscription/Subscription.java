package br.com.matteusmoreno.domain.subscription;

import br.com.matteusmoreno.domain.subscription.constant.PlanType;
import br.com.matteusmoreno.domain.subscription.constant.SubscriptionStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public class Subscription {
    public PlanType planType;
    public SubscriptionStatus status;
    public String gatewaySubscriptionId;
    public LocalDateTime startedAt;
    public LocalDateTime nextBillingDate;
    public MonthlyUsage monthlyUsage;
}
