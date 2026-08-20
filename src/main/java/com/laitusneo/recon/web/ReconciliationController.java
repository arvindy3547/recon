package com.laitusneo.recon.web;

import com.laitusneo.recon.service.ReconciliationService;
import com.laitusneo.recon.web.dto.ReconciliationResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    /**
     * Status for a single reference. asOf defaults to now; pass an ISO-8601
     * instant to ask what this service believed at a stated past instant
     * (spec 4.5), reconstructed purely from facts recorded by that time.
     */
    @GetMapping("/{reference}")
    public ReconciliationResponse one(
            @PathVariable String reference,
            @RequestParam(required = false) Instant asOf) {

        Instant effectiveAsOf = asOf != null ? asOf : Instant.now();
        return reconciliationService.statusFor(reference, effectiveAsOf)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "no sent or reported fact exists for reference '" + reference + "' as of " + effectiveAsOf));
    }

    /** Status for every reference either source has ever mentioned, as of asOf (default now). */
    @GetMapping
    public List<ReconciliationResponse> all(@RequestParam(required = false) Instant asOf) {
        return reconciliationService.allStatuses(asOf != null ? asOf : Instant.now());
    }
}
