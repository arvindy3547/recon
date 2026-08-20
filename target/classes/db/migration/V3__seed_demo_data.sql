-- Seed data. Each reference below exists to make one case observable
-- through GET /api/v1/reconciliation. See README "How to observe each case".

-- REF-1001: clean match. Both sides agree on amount and currency.
INSERT INTO sent_fact (reference, amount_minor, currency, occurred_at, recorded_at, idempotency_key)
VALUES ('REF-1001', 500000, 'INR', '2026-08-10T09:00:00Z', '2026-08-10T09:00:01Z', 'seed-sent-1001');
INSERT INTO reported_fact (reference, amount_minor, currency, occurred_at, recorded_at, idempotency_key)
VALUES ('REF-1001', 500000, 'INR', '2026-08-10T09:00:00Z', '2026-08-10T10:15:00Z', 'seed-reported-1001');

-- REF-1002: deliberate amount mismatch. We sent 5,000.00 INR; the partner
-- reported 4,995.00 INR back, five rupees short - the shape of a
-- partner-side fee silently deducted before settlement. This is exactly
-- the kind of thing the service exists to surface, not resolve: we do not
-- guess that it's a fee, we report the mismatch and the two numbers.
INSERT INTO sent_fact (reference, amount_minor, currency, occurred_at, recorded_at, idempotency_key)
VALUES ('REF-1002', 500000, 'INR', '2026-08-11T09:00:00Z', '2026-08-11T09:00:01Z', 'seed-sent-1002');
INSERT INTO reported_fact (reference, amount_minor, currency, occurred_at, recorded_at, idempotency_key)
VALUES ('REF-1002', 499500, 'INR', '2026-08-11T09:00:00Z', '2026-08-11T11:30:00Z', 'seed-reported-1002');

-- REF-1003: pending. We sent it; the partner has not reported back yet.
INSERT INTO sent_fact (reference, amount_minor, currency, occurred_at, recorded_at, idempotency_key)
VALUES ('REF-1003', 250000, 'INR', '2026-08-12T09:00:00Z', '2026-08-12T09:00:01Z', 'seed-sent-1003');

-- REF-1004: orphan. The partner reported something we have no record of
-- sending. We do not assume it is fraud or a timing issue - we say only
-- that it is unmatched on our side.
INSERT INTO reported_fact (reference, amount_minor, currency, occurred_at, recorded_at, idempotency_key)
VALUES ('REF-1004', 120000, 'INR', '2026-08-12T09:30:00Z', '2026-08-12T09:45:00Z', 'seed-reported-1004');

-- REF-1005: correction, to demonstrate the as-of query. The partner's
-- first report has the wrong currency (USD instead of INR); a later
-- report from the same partner corrects it. Both rows survive; nothing
-- is updated or deleted.
--   as-of the first recorded_at  -> CURRENCY_MISMATCH
--   as-of (or after) the second  -> MATCHED
INSERT INTO sent_fact (reference, amount_minor, currency, occurred_at, recorded_at, idempotency_key)
VALUES ('REF-1005', 750000, 'INR', '2026-08-13T09:00:00Z', '2026-08-13T09:00:01Z', 'seed-sent-1005');
INSERT INTO reported_fact (reference, amount_minor, currency, occurred_at, recorded_at, idempotency_key)
VALUES ('REF-1005', 750000, 'USD', '2026-08-13T09:00:00Z', '2026-08-13T09:10:00Z', 'seed-reported-1005-typo');
INSERT INTO reported_fact (reference, amount_minor, currency, occurred_at, recorded_at, idempotency_key)
VALUES ('REF-1005', 750000, 'INR', '2026-08-13T09:00:00Z', '2026-08-13T14:00:00Z', 'seed-reported-1005-correction');
