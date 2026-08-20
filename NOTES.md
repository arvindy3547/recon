# NOTES.md

## 1–2. The cases, and how each was handled

**Reference known to only one side.** A `reference` can exist in
`sent_fact` with nothing in `reported_fact` (we sent, partner hasn't told
us anything back yet — `PENDING_REPORT`), or the reverse (partner told us
about money we have no record of sending — `ORPHAN_REPORT`). Both are
first-class statuses, not errors, because both are normal states a
reference passes through, not exceptional ones.

**Both sides present, values disagree.** Split into `CURRENCY_MISMATCH`
and `AMOUNT_MISMATCH` rather than one generic `MISMATCH`, because they are
different failure shapes with different likely causes (a currency mismatch
usually means a routing or config error; an amount mismatch usually means
a fee or a genuine discrepancy) and a reviewer should not have to open the
record to know which kind they're looking at.

**Corrections.** Constraint 4.4 forbids updating or deleting a recorded
fact, but real partners do send corrections. A correction is modelled as a
new row for the same `reference` with a later `recorded_at`. The original,
wrong row is never touched. "Current" status for a reference is always
computed from the row with the greatest `recorded_at <= asOf` per source —
so a correction simply becomes the new latest fact, and the wrong fact it
superseded is still on disk for audit, forever.

**"What did we believe at time T."** Because status is derived on demand
rather than stored, answering this is the same query with a different
`asOf` bound — no separate history table, no separate reconciliation-log
that itself would need updating as beliefs change.

**Retried ingestion.** A caller (us, retrying our own send-notification
call, or the partner, retrying their webhook) can submit the same logical
fact more than once. Handled via a required `Idempotency-Key` header plus
a database `UNIQUE` constraint on it — see section 3.

**Concurrent retries of the same key.** Two requests with the same
`Idempotency-Key` arriving close enough together that both miss the
"does this key already exist" check. Handled by catching the unique
constraint violation and re-reading the row the other request just
committed, rather than trusting the pre-check alone. See section 3.

**Reference reused across sides with different currency, or the same
partner sending two different reports for the same reference before ever
sending a correction (not a typo-fix, just two live, disagreeing reports)**
— identified but not specially handled: the service always takes the
*latest* `reported_fact` row for a reference, so an out-of-order arrival
where an older, correct report arrives after a newer, wrong one would be
read as the wrong one being current. I did not have time to model
per-source "which of these several same-time reports is authoritative" —
right now recency of `recorded_at` is the only tiebreaker, and I'm not
confident that's always right.

**Malformed money (fractional currency units, zero amounts).** Handled at
two layers: `amountMinor` is a `Long` end-to-end (never `double` /
`BigDecimal`), and the database additionally rejects zero amounts and
malformed currency codes via `CHECK` constraints, so a bug in the Java
validation layer cannot let bad data through — see section 4.

**Not handled, named honestly:** partial matches (partner reports two
smaller payments against one larger sent amount, or vice versa — this
service matches strictly by shared `reference`, one-to-one, and has no
concept of splitting/aggregating); a reference that never gets a report at
all (currently indistinguishable from "not urgent yet" — there's no
staleness threshold that would flag a `PENDING_REPORT` as overdue); and
authentication on the ingest endpoints (see section 6).

## 3. Repeat safety

Rejected approach 1: **check-then-insert with no DB constraint.** ("Does a
row with this `Idempotency-Key` exist? If not, insert.") Fails under
concurrency specifically: two requests can both run the SELECT, both see
nothing, and both proceed to INSERT — the check has no atomicity with the
write that follows it.

Rejected approach 2: **an application-level lock (e.g. a
`ConcurrentHashMap` or `synchronized` block keyed by idempotency key).**
Closes the race within a single JVM, but silently stops working the moment
this service runs as more than one instance — which is exactly the
condition under which duplicate concurrent requests are most likely to
land on different instances in the first place.

What's actually used: a `UNIQUE` constraint on `idempotency_key` in both
fact tables, enforced by Postgres. The service still does a SELECT first
(cheap, avoids an exception on the common non-duplicate path), but
correctness never depends on that SELECT — if two concurrent requests both
miss it and both attempt the INSERT, Postgres allows exactly one to
commit; the loser catches `DataIntegrityViolationException` and re-reads
the winner's row. Both callers receive the same fact back either way,
distinguished only by a `replayed` flag in the response.

## 4. Where correctness lives

**Database-enforced:** the `idempotency_key` uniqueness (the mechanism
that makes retries safe, not just a nice-to-have); non-null and
non-zero-amount checks on every fact; the currency-code shape check
(`^[A-Z]{3}$`); and immutability itself — a `BEFORE UPDATE OR DELETE`
trigger on both fact tables raises unconditionally, for any role, so no
application bug can silently violate constraint 4.4.

**Application-enforced only:** the matching rule (same `reference`, same
`amountMinor`, same `currency` ⇒ `MATCHED`) and the "latest by
`recorded_at`" resolution when a reference has multiple facts on one side.
For the database to enforce these instead, "current state per reference"
would need to be a materialized, queryable structure (e.g. a table or view
keyed by `reference` holding only the winning row per side) with a
trigger or constraint defining "winning" — I judged that as added
complexity the 48-hour scope didn't justify, given the derived-on-read
approach already answers the as-of requirement correctly.

## 5. What the service says about a record it can't match

It states which side is missing (`PENDING_REPORT` / `ORPHAN_REPORT`) or,
if both sides are present, exactly how they disagree
(`AMOUNT_MISMATCH` / `CURRENCY_MISMATCH`) — and returns the actual values
from both sides alongside the status, not just the label. It never
returns `MATCHED` unless every field the matching rule checks is
literally equal, and it never infers a match from partial agreement (e.g.
same amount, different currency is not treated as "probably fine"). This
is deliberate: the two sentences in section 3 of the brief forbid
recording anything as true that can't be shown to be true, and inferring
a match from partial evidence is exactly that — a claim the data doesn't
support.

## 6. Where this is not production-ready

- **No authentication or authorization** on the ingest endpoints. Anyone
  who can reach the service can record a "sent" or "reported" fact.
- **No rate limiting or payload-size limits** on the POST endpoints.
- **`Idempotency-Key` is trusted, unscoped, and never expires** — nothing
  stops a caller from reusing a key across genuinely different requests,
  which would incorrectly return the first request's data for the second.
  A production version would need to detect and reject a reused key whose
  body doesn't match the original.
- **No partial/split matching**, as noted in section 2 — real reconciliation
  systems often need many-to-one and one-to-many matching, and this one
  doesn't attempt it.
- **No pagination** on `GET /api/v1/reconciliation` — fine for the seeded
  demo data, would not scale.
- **No monitoring/alerting** on references that stay `PENDING_REPORT` or
  `ORPHAN_REPORT` past a reasonable threshold — today someone has to think
  to query for them.
- **Single environment, no migration rollback story** beyond Flyway's
  forward-only model, and no tested backup/restore process.
