package dev.confera.reconciliation.dto;

import dev.confera.reconciliation.entity.DiscrepancyStatus;
import jakarta.validation.constraints.NotNull;

public record ResolveDiscrepancyRequest(
    @NotNull DiscrepancyStatus status,
    String notes
) {}