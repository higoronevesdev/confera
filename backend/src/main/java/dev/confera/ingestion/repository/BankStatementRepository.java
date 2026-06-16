package dev.confera.ingestion.repository;

import dev.confera.ingestion.entity.BankStatement;
import dev.confera.ingestion.entity.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BankStatementRepository extends JpaRepository<BankStatement, UUID> {

    List<BankStatement> findByFileImportId(UUID fileImportId);

    List<BankStatement> findByTenantIdAndReconciliationStatus(UUID tenantId, ReconciliationStatus status);

    long countByTenantIdAndReconciliationStatus(UUID tenantId, ReconciliationStatus status);
}