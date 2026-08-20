package com.laitusneo.recon.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

/**
 * amountMinor is a long, deliberately - there is no code path in this
 * service where money is ever represented as a float or BigDecimal.
 * Validation here is a fast-fail convenience for callers; the database
 * constraints in V1 are the layer that actually cannot be bypassed.
 */
public record RecordFactRequest(

        @NotBlank
        String reference,

        @NotNull
        Long amountMinor,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO 4217 code")
        String currency,

        @NotNull
        Instant occurredAt
) {}
