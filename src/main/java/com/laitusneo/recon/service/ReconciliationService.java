package com.laitusneo.recon.service;

import com.laitusneo.recon.domain.ReportedFact;
import com.laitusneo.recon.domain.SentFact;
import com.laitusneo.recon.repo.ReportedFactRepository;
import com.laitusneo.recon.repo.SentFactRepository;
import com.laitusneo.recon.web.dto.MatchStatus;
import com.laitusneo.recon.web.dto.ReconciliationResponse;
import com.laitusneo.recon.web.dto.ReconciliationResponse.FactSummary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Nothing here is stored - match status is a conclusion derived on demand
 * from the two fact streams, as of whatever instant the caller asks about.
 * That's what makes the "what did we believe at time T" question (spec
 * 4.5) answerable without a separate mutable "current status" column that
 * would itself need updating (and would violate 4.4 the moment it did).
 *
 * "Latest as of T" means: among rows for this reference with
 * recorded_at <= T, the one with the greatest recorded_at. Corrections are
 * just later rows: querying with an earlier T naturally reconstructs what
 * we believed before the correction arrived.
 */
@Service
public class ReconciliationService {

    private final SentFactRepository sentFactRepository;
    private final ReportedFactRepository reportedFactRepository;

    public ReconciliationService(SentFactRepository sentFactRepository,
                                  ReportedFactRepository reportedFactRepository) {
        this.sentFactRepository = sentFactRepository;
        this.reportedFactRepository = reportedFactRepository;
    }

    public Optional<ReconciliationResponse> statusFor(String reference, Instant asOf) {
        List<SentFact> sentHistory = sentFactRepository.findAsOf(reference, asOf);
        List<ReportedFact> reportedHistory = reportedFactRepository.findAsOf(reference, asOf);

        if (sentHistory.isEmpty() && reportedHistory.isEmpty()) {
            return Optional.empty();
        }

        SentFact latestSent = sentHistory.isEmpty() ? null : sentHistory.get(0);
        ReportedFact latestReported = reportedHistory.isEmpty() ? null : reportedHistory.get(0);

        MatchStatus status = classify(latestSent, latestReported);

        return Optional.of(new ReconciliationResponse(
                reference,
                status,
                asOf,
                latestSent == null ? null : new FactSummary(
                        latestSent.getAmountMinor(), latestSent.getCurrency(),
                        latestSent.getOccurredAt(), latestSent.getRecordedAt()),
                latestReported == null ? null : new FactSummary(
                        latestReported.getAmountMinor(), latestReported.getCurrency(),
                        latestReported.getOccurredAt(), latestReported.getRecordedAt())
        ));
    }

    public List<ReconciliationResponse> allStatuses(Instant asOf) {
        List<String> references = java.util.stream.Stream.concat(
                        sentFactRepository.findAllReferences().stream(),
                        reportedFactRepository.findAllReferences().stream())
                .distinct()
                .sorted()
                .toList();

        return references.stream()
                .map(ref -> statusFor(ref, asOf))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private MatchStatus classify(SentFact sent, ReportedFact reported) {
        if (sent == null && reported == null) {
            return MatchStatus.UNKNOWN_REFERENCE;
        }
        if (sent == null) {
            return MatchStatus.ORPHAN_REPORT;
        }
        if (reported == null) {
            return MatchStatus.PENDING_REPORT;
        }
        if (!sent.getCurrency().equals(reported.getCurrency())) {
            return MatchStatus.CURRENCY_MISMATCH;
        }
        if (sent.getAmountMinor() != reported.getAmountMinor()) {
            return MatchStatus.AMOUNT_MISMATCH;
        }
        return MatchStatus.MATCHED;
    }
}
