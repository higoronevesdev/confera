package dev.confera.ingestion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "bank_statements",
    indexes = @Index(name = "idx_bank_statements_tenant_status", columnList = "tenant_id, reconciliation_status")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID fileImportId;

    @Column(length = 10)
    private String bankCode;

    @Column(length = 20)
    private String agency;

    @Column(length = 30)
    private String accountNumber;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false)
    private LocalDate valueDate;

    @Column(nullable = false)
    private Long amountCents;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}