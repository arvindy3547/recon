package com.laitusneo.recon.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * A fact about money we sent, as reported by our own initiating system.
 *
 * This entity is append-only: rows are never updated or deleted, by the
 * application or the database (see V2 migration). {@code @Immutable} tells
 * Hibernate never to issue an UPDATE for this entity even by accident via
 * dirty checking; the DB trigger is the real backstop.
 */
@Entity
@Table(name = "sent_fact")
@Immutable
public class SentFact {

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

    protected SentFact() {
        // for JPA
    }

    public SentFact(String reference, long amountMinor, String currency,
                     Instant occurredAt, String idempotencyKey) {
        this.reference = reference;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.occurredAt = occurredAt;
        this.idempotencyKey = idempotencyKey;
        // Set explicitly rather than relying on the DB column default: Hibernate
        // includes every mapped field in its INSERT, including nulls, which
        // overrides a DEFAULT now() at the database level.
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
