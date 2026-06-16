package dev.confera.reconciliation.dto;

import dev.confera.reconciliation.entity.MatchType;
import dev.confera.reconciliation.entity.ReconciliationMatch;

import java.time.LocalDateTime;
import java.util.UUID;

public record MatchResponse(
    UUID id,
    UUID tenantId,
    UUID bankStatementId,
    UUID receivableId,
    MatchType matchType,
    UUID matchedByUserId,
    Long differenceCents,
    String notes,
    LocalDateTime createdAt
) {
    public static MatchResponse from(ReconciliationMatch match) {
        return new MatchResponse(
            match.getId(),
            match.getTenantId(),
            match.getBankStatementId(),
            match.getReceivableId(),
            match.getMatchType(),
            match.getMatchedByUserId(),
            match.getDifferenceCents(),
            match.getNotes(),
            match.getCreatedAt()
        );
    }
}