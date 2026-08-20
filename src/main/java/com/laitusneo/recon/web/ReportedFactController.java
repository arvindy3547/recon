package com.laitusneo.recon.web;

import com.laitusneo.recon.service.IngestService;
import com.laitusneo.recon.web.dto.FactResponse;
import com.laitusneo.recon.web.dto.RecordFactRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reported-facts")
public class ReportedFactController {

    private final IngestService ingestService;

    public ReportedFactController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    /** Records what the partner told us happened. Same idempotency contract as sent-facts. */
    @PostMapping
    public ResponseEntity<FactResponse> record(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RecordFactRequest request) {

        FactResponse response = ingestService.recordReported(request, idempotencyKey);
        HttpStatus status = response.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }
}
