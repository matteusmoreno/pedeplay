package br.com.matteusmoreno.domain.subscription;

import java.time.LocalDateTime;

public class MonthlyUsage {
    public Integer eventsUsed;
    public Integer requestsReceived;
    public LocalDateTime lastResetDate;
}
