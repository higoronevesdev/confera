package dev.confera.ledger.repository;

import dev.confera.ledger.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("SELECT DISTINCT t FROM Transaction t JOIN FETCH t.entries e JOIN FETCH e.account WHERE t.id = :id")
    Optional<Transaction> findByIdWithEntries(@Param("id") UUID id);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByTenantIdAndOccurredAtBetween(UUID tenantId, OffsetDateTime from, OffsetDateTime to);
}