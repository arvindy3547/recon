# recon

A service that reconciles two independently-arriving descriptions of the
same money: what we sent, and what a partner later reported back.

## Stack

Java 21, Spring Boot 3.3, PostgreSQL, Flyway (versioned migrations in
`src/main/resources/db/migration`).

## Running locally

Requires a local Postgres. Simplest path with Docker:

```
docker run --name recon-pg -e POSTGRES_USER=recon -e POSTGRES_PASSWORD=recon \
  -e POSTGRES_DB=recon -p 5432:5432 -d postgres:16
```

Then:

```
./mvnw spring-boot:run
```

Flyway runs all three migrations automatically on startup, including the
seed data. The app listens on `:8080`.

## Environment variables

| Variable | Default | Notes |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/recon` | JDBC URL |
| `DATABASE_USERNAME` | `recon` | |
| `DATABASE_PASSWORD` | `recon` | |
| `PORT` | `8080` | Most PaaS free tiers inject this |

## Endpoints

### `POST /api/v1/sent-facts`
### `POST /api/v1/reported-facts`

Both require an `Idempotency-Key` header and this body:

```json
{
  "reference": "REF-2001",
  "amountMinor": 150000,
  "currency": "INR",
  "occurredAt": "2026-08-19T10:00:00Z"
}
```

Returns `201` with `"replayed": false` on first insert, `200` with
`"replayed": true` if the same `Idempotency-Key` is sent again (see
"observing idempotency" below).

### `GET /api/v1/reconciliation/{reference}?asOf=<ISO-8601 instant>`

Current (or as-of) match status for one reference. `asOf` is optional and
defaults to now.

### `GET /api/v1/reconciliation?asOf=<ISO-8601 instant>`

Match status for every reference either source has ever mentioned.

## How to observe each case

The seed data (`V3__seed_demo_data.sql`) exists specifically to make these
checkable without needing to construct scenarios by hand:

- **Clean match** — `GET /api/v1/reconciliation/REF-1001` → `MATCHED`
- **Amount mismatch** — `GET /api/v1/reconciliation/REF-1002` → `AMOUNT_MISMATCH`
  (sent ₹5,000.00, partner reported ₹4,995.00)
- **Pending** — `GET /api/v1/reconciliation/REF-1003` → `PENDING_REPORT`
  (we sent it, no report yet)
- **Orphan** — `GET /api/v1/reconciliation/REF-1004` → `ORPHAN_REPORT`
  (partner reported something we never sent)
- **Correction over time** — `REF-1005`:
  - `GET /api/v1/reconciliation/REF-1005?asOf=2026-08-13T09:30:00Z` → `CURRENCY_MISMATCH`
    (the partner's first report had the wrong currency)
  - `GET /api/v1/reconciliation/REF-1005?asOf=2026-08-13T15:00:00Z` (or no `asOf`) → `MATCHED`
    (their correction arrived later; the original wrong report still exists
    in the table, it is simply no longer the *latest* fact)

**Idempotency** — `POST` the same body with the same `Idempotency-Key`
twice: first response is `201`/`replayed:false`, second is
`200`/`replayed:true`, same `id` both times, no second row in the table.

**Immutability** — connect to the database directly and try
`UPDATE sent_fact SET amount_minor = 1 WHERE id = 1;` — Postgres raises an
exception from the trigger in `V2__lock_down_immutability.sql` regardless
of which role issues the statement.

## Deployment

Deployed on [Render free tier / Railway — fill in once deployed] with a
managed Postgres instance. Live address and any credentials needed to
exercise it: **[fill in]**.

## What's deliberately not built

See NOTES.md section 6.
