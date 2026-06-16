package dev.confera.ingestion.repository;

import dev.confera.ingestion.entity.Receivable;
import dev.confera.ingestion.entity.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReceivableRepository extends JpaRepository<Receivable, UUID> {

    List<Receivable> findByTenantIdAndReconciliationStatus(UUID tenantId, ReconciliationStatus status);

    List<Receivable> findByTenantIdAndExpectedSettlementDateBetween(UUID tenantId, LocalDate from, LocalDate to);
}