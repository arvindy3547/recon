package com.laitusneo.recon.web.dto;

import java.time.Instant;

/**
 * replayed=true means: this Idempotency-Key was already seen, and what
 * you're getting back is the original fact, not a new insert. This is
 * what makes idempotency observable from the outside rather than just
 * asserted in NOTES.md - call the same POST twice and watch the flag flip.
 */
public record FactResponse(
        Long id,
        String reference,
        long amountMinor,
        String currency,
        Instant occurredAt,
        Instant recordedAt,
        String idempotencyKey,
        boolean replayed
) {}
