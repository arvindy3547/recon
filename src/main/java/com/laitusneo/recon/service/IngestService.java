package com.laitusneo.recon.service;

import com.laitusneo.recon.domain.ReportedFact;
import com.laitusneo.recon.domain.SentFact;
import com.laitusneo.recon.repo.ReportedFactRepository;
import com.laitusneo.recon.repo.SentFactRepository;
import com.laitusneo.recon.web.dto.FactResponse;
import com.laitusneo.recon.web.dto.RecordFactRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Idempotency, end to end.
 *
 * The obvious approach - "SELECT by idempotency key, and if nothing comes
 * back, INSERT" - has a race: two requests with the same key can both pass
 * the SELECT before either commits its INSERT, and both then try to insert.
 * Without a database constraint, that produces two rows for one logical
 * request; with a purely application-level lock (e.g. a ConcurrentHashMap
 * keyed by idempotency key), it only protects a single JVM instance and
 * silently stops working the moment this service is scaled horizontally.
 *
 * What actually closes the race: the UNIQUE constraint on idempotency_key
 * (see V1 migration) is enforced by Postgres itself, atomically, across
 * every connection and every instance. We still do the SELECT-first as an
 * optimisation to avoid a DB round-trip exception on the common path, but
 * correctness does not depend on it - if two concurrent requests both miss
 * the SELECT and both attempt the INSERT, exactly one commits and the
 * other catches DataIntegrityViolationException and re-reads the row the
 * winner just wrote. Both callers get back the same fact either way.
 */
@Service
public class IngestService {

    private final SentFactRepository sentFactRepository;
    private final ReportedFactRepository reportedFactRepository;

    public IngestService(SentFactRepository sentFactRepository,
                          ReportedFactRepository reportedFactRepository) {
        this.sentFactRepository = sentFactRepository;
        this.reportedFactRepository = reportedFactRepository;
    }

    public FactResponse recordSent(RecordFactRequest req, String idempotencyKey) {
        var existing = sentFactRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get(), true);
        }
        try {
            SentFact saved = sentFactRepository.save(new SentFact(
                    req.reference(), req.amountMinor(), req.currency(), req.occurredAt(), idempotencyKey));
            return toResponse(saved, false);
        } catch (DataIntegrityViolationException e) {
            SentFact winner = sentFactRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> e);
            return toResponse(winner, true);
        }
    }

    public FactResponse recordReported(RecordFactRequest req, String idempotencyKey) {
        var existing = reportedFactRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get(), true);
        }
        try {
            ReportedFact saved = reportedFactRepository.save(new ReportedFact(
                    req.reference(), req.amountMinor(), req.currency(), req.occurredAt(), idempotencyKey));
            return toResponse(saved, false);
        } catch (DataIntegrityViolationException e) {
            ReportedFact winner = reportedFactRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> e);
            return toResponse(winner, true);
        }
    }

    private FactResponse toResponse(SentFact f, boolean replayed) {
        return new FactResponse(f.getId(), f.getReference(), f.getAmountMinor(), f.getCurrency(),
                f.getOccurredAt(), f.getRecordedAt(), f.getIdempotencyKey(), replayed);
    }

    private FactResponse toResponse(ReportedFact f, boolean replayed) {
        return new FactResponse(f.getId(), f.getReference(), f.getAmountMinor(), f.getCurrency(),
                f.getOccurredAt(), f.getRecordedAt(), f.getIdempotencyKey(), replayed);
    }
}
