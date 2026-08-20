package com.laitusneo.recon.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * A fact about money the partner reported back to us. Mirrors SentFact -
 * same append-only contract, same shape, deliberately kept as a separate
 * type because the two sources are not guaranteed to stay the same shape
 * over time and conflating them would hide that they are independent feeds.
 */
@Entity
@Table(name = "reported_fact")
@Immutable
public class ReportedFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reference;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    protected ReportedFact() {
        // for JPA
    }

    public ReportedFact(String reference, long amountMinor, String currency,
                         Instant occurredAt, String idempotencyKey) {
        this.reference = reference;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.occurredAt = occurredAt;
        this.idempotencyKey = idempotencyKey;
        // See SentFact for why this is set explicitly rather than left to the
        // database column default.
        this.recordedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
