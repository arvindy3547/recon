package com.laitusneo.recon.web.dto;

import java.time.Instant;

public record ReconciliationResponse(
        String reference,
        MatchStatus status,
        Instant asOf,
        FactSummary sent,       // null if we have no sent fact as of asOf
        FactSummary reported    // null if we have no reported fact as of asOf
) {
    public record FactSummary(long amountMinor, String currency, Instant occurredAt, Instant recordedAt) {}
}
