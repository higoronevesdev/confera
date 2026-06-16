package dev.confera.ingestion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "receivables",
    indexes = @Index(
        name = "idx_receivables_tenant_status_date",
        columnList = "tenant_id, reconciliation_status, expected_settlement_date"
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receivable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    private UUID fileImportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReceivableSource source;

    @Column(nullable = false)
    private LocalDate saleDate;

    @Column(nullable = false)
    private LocalDate expectedSettlementDate;

    @Column(nullable = false)
    private Long grossAmountCents;

    @Column(nullable = false)
    @Builder.Default
    private Long feeAmountCents = 0L;

    @Column(nullable = false)
    private Long netAmountCents;

    @Column(nullable = false)
    @Builder.Default
    private int installmentNumber = 1;

    @Column(nullable = false)
    @Builder.Default
    private int totalInstallments = 1;

    @Column(length = 50)
    private String authorizationCode;

    @Column(length = 50)
    private String nsu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void computeNet() {
        this.netAmountCents = this.grossAmountCents - (this.feeAmountCents != null ? this.feeAmountCents : 0L);
    }
}