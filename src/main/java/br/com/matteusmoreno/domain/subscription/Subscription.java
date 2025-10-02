package br.com.matteusmoreno.domain.subscription;

import br.com.matteusmoreno.domain.subscription.constant.PlanType;
import br.com.matteusmoreno.domain.subscription.constant.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Subscription {
    public PlanType planType;
    public SubscriptionStatus status;
    public String gatewaySubscriptionId;
    public LocalDateTime startedAt;
    public LocalDateTime nextBillingDate;
    public MonthlyUsage monthlyUsage;
}
