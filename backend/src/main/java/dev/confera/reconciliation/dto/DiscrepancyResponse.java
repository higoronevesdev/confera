package dev.confera.reconciliation.dto;

import dev.confera.reconciliation.entity.DiscrepancyStatus;
import dev.confera.reconciliation.entity.DiscrepancyType;
import dev.confera.reconciliation.entity.ReconciliationDiscrepancy;

import java.time.LocalDateTime;
import java.util.UUID;

public record DiscrepancyResponse(
    UUID id,
    UUID tenantId,
    DiscrepancyType type,
    UUID bankStatementId,
    UUID receivableId,
    Long amountCents,
    String description,
    DiscrepancyStatus status,
    LocalDateTime resolvedAt,
    UUID resolvedByUserId,
    LocalDateTime createdAt
) {
    public static DiscrepancyResponse from(ReconciliationDiscrepancy d) {
        return new DiscrepancyResponse(
            d.getId(),
            d.getTenantId(),
            d.getType(),
            d.getBankStatementId(),
            d.getReceivableId(),
            d.getAmountCents(),
            d.getDescription(),
            d.getStatus(),
            d.getResolvedAt(),
            d.getResolvedByUserId(),
            d.getCreatedAt()
        );
    }
}