-- Two independent, append-only fact streams. Neither table is ever
-- UPDATEd or DELETEd from by the application (see V2 for the DB-level
-- backstop). A correction is a brand-new row with a later recorded_at.

CREATE TABLE sent_fact (
    id               BIGSERIAL PRIMARY KEY,
    reference        VARCHAR(128) NOT NULL,
    amount_minor     BIGINT       NOT NULL,
    currency         CHAR(3)      NOT NULL,
    occurred_at      TIMESTAMPTZ  NOT NULL,   -- when the money movement happened, per this source
    recorded_at      TIMESTAMPTZ  NOT NULL DEFAULT now(), -- when THIS SERVICE learned the fact
    idempotency_key  VARCHAR(128) NOT NULL,

    CONSTRAINT sent_fact_idempotency_key_uk UNIQUE (idempotency_key),
    CONSTRAINT sent_fact_currency_format_ck CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT sent_fact_amount_nonzero_ck  CHECK (amount_minor <> 0)
);

CREATE INDEX sent_fact_reference_recorded_idx ON sent_fact (reference, recorded_at);

CREATE TABLE reported_fact (
    id               BIGSERIAL PRIMARY KEY,
    reference        VARCHAR(128) NOT NULL,
    amount_minor     BIGINT       NOT NULL,
    currency         CHAR(3)      NOT NULL,
    occurred_at      TIMESTAMPTZ  NOT NULL,
    recorded_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    idempotency_key  VARCHAR(128) NOT NULL,

    CONSTRAINT reported_fact_idempotency_key_uk UNIQUE (idempotency_key),
    CONSTRAINT reported_fact_currency_format_ck CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT reported_fact_amount_nonzero_ck  CHECK (amount_minor <> 0)
);

CREATE INDEX reported_fact_reference_recorded_idx ON reported_fact (reference, recorded_at);

-- Notes for NOTES.md section 4 ("which invariants does the DB enforce"):
--   * amount_minor is an integer column -> a float/decimal can never enter this path.
--   * the UNIQUE constraint on idempotency_key is what makes retries safe,
--     even under concurrent requests (see V2 comment + IngestService).
--   * currency shape and non-zero amount are enforced here, not just in Java,
--     so a bug in validation code cannot let bad data through.
