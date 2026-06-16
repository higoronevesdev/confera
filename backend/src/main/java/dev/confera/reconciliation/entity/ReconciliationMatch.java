package dev.confera.reconciliation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "reconciliation_matches",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_reconciliation_matches",
        columnNames = {"bank_statement_id", "receivable_id"}
    )
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "bank_statement_id", nullable = false)
    private UUID bankStatementId;

    @Column(name = "receivable_id", nullable = false)
    private UUID receivableId;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 20)
    private MatchType matchType;

    @Column(name = "matched_by_user_id")
    private UUID matchedByUserId;

    @Column(name = "difference_cents", nullable = false)
    @Builder.Default
    private Long differenceCents = 0L;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}