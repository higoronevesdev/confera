package dev.confera.reconciliation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ManualMatchRequest(
    @NotNull UUID bankStatementId,
    @NotNull UUID receivableId,
    String notes
) {}