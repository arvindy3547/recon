package com.laitusneo.recon.web;

import com.laitusneo.recon.service.IngestService;
import com.laitusneo.recon.web.dto.FactResponse;
import com.laitusneo.recon.web.dto.RecordFactRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sent-facts")
public class SentFactController {

    private final IngestService ingestService;

    public SentFactController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    /**
     * Records that we sent this money. Idempotency-Key is required: it is
     * the caller's promise that two requests with the same key describe
     * the same attempt, not two different transfers that happen to match.
     */
    @PostMapping
    public ResponseEntity<FactResponse> record(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RecordFactRequest request) {

        FactResponse response = ingestService.recordSent(request, idempotencyKey);
        HttpStatus status = response.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }
}
