package dev.confera.reconciliation.repository;

import dev.confera.reconciliation.entity.ReconciliationMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReconciliationMatchRepository extends JpaRepository<ReconciliationMatch, UUID> {

    Page<ReconciliationMatch> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<ReconciliationMatch> findByBankStatementIdAndReceivableId(UUID bankStatementId, UUID receivableId);

    boolean existsByBankStatementIdAndReceivableId(UUID bankStatementId, UUID receivableId);
}