package dev.confera.reconciliation.dto;

import java.util.UUID;

public record ReconciliationSummaryResponse(
    UUID tenantId,
    int totalBankStatements,
    int totalReceivables,
    int exactMatches,
    int fuzzyMatches,
    int unmatched,
    int discrepanciesCreated,
    long executionTimeMs
) {}