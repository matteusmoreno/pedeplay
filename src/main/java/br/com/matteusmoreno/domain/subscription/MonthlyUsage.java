package br.com.matteusmoreno.domain.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MonthlyUsage {
    public Integer eventsUsed;
    public Integer requestsReceived;
    public LocalDateTime lastResetDate;
}
