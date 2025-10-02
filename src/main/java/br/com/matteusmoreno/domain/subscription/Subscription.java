package br.com.matteusmoreno.domain.subscription;


import java.time.LocalDateTime;

public class Subscription {
    public PlanType planId;
    public SubscriptionStatus status;
    public String gatewaySubscriptionId;
    public LocalDateTime startedAt;
    public LocalDateTime nextBillingDate;
    public MonthlyUsage monthlyUsage;
}
