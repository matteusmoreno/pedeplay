package br.com.matteusmoreno.domain.subscription;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public class MonthlyUsage {
    public Integer eventsUsed;
    public Integer requestsReceived;
    public LocalDateTime lastResetDate;
}
