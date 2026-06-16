package dev.confera.reconciliation.repository;

import dev.confera.reconciliation.entity.DiscrepancyStatus;
import dev.confera.reconciliation.entity.ReconciliationDiscrepancy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReconciliationDiscrepancyRepository extends JpaRepository<ReconciliationDiscrepancy, UUID> {

    Page<ReconciliationDiscrepancy> findByTenantId(UUID tenantId, Pageable pageable);

    Page<ReconciliationDiscrepancy> findByTenantIdAndStatus(UUID tenantId, DiscrepancyStatus status, Pageable pageable);

    Optional<ReconciliationDiscrepancy> findByIdAndTenantId(UUID id, UUID tenantId);
}