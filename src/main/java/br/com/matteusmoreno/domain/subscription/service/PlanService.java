package br.com.matteusmoreno.domain.subscription.service;

import br.com.matteusmoreno.domain.subscription.constant.PlanType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PlanService {

    public Integer getSongLimitForPlan(PlanType planType) {
        return switch (planType) {
            case FREE -> 300;
            case PREMIUM -> 500;
            case ENTERPRISE -> Integer.MAX_VALUE; // Representa ilimitado
        };
    }

    public Integer getEventLimitForPlan(PlanType planType) {
        return switch (planType) {
            case FREE -> 20;
            case PREMIUM -> 50;
            case ENTERPRISE -> Integer.MAX_VALUE;
        };
    }

    public Integer getRequestLimitForPlan(PlanType planType) {
        return switch (planType) {
            case FREE -> 100;
            case PREMIUM, ENTERPRISE -> Integer.MAX_VALUE;
        };
    }
}