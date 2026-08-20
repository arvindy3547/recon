package com.laitusneo.recon.web.dto;

/**
 * The full set of conclusions this service is willing to draw about a
 * reference. Notably there is no MATCHED-by-default and no "probably
 * fine" - every reference lands in exactly one of these, and each one
 * says plainly what evidence it does or doesn't have.
 */
public enum MatchStatus {
    MATCHED,            // latest sent and latest reported agree on amount and currency
    AMOUNT_MISMATCH,    // both sides present, same currency, different amount
    CURRENCY_MISMATCH,  // both sides present, different currency
    PENDING_REPORT,     // we sent it, partner hasn't reported it back (yet, as of the query time)
    ORPHAN_REPORT,      // partner reported it, we have no record of sending it
    UNKNOWN_REFERENCE   // neither side has any record of this reference as of the query time
}
