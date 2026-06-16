package dev.confera.ledger.repository;

import dev.confera.ledger.entity.Direction;
import dev.confera.ledger.entity.Entry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface EntryRepository extends JpaRepository<Entry, UUID> {

    @Query("SELECT COALESCE(SUM(e.amountCents), 0) FROM Entry e WHERE e.account.id = :accountId AND e.direction = :direction")
    long sumAmountByAccountIdAndDirection(@Param("accountId") UUID accountId, @Param("direction") Direction direction);

    @Query(
        value = """
            SELECT e FROM Entry e
            JOIN FETCH e.account
            JOIN e.transaction t
            WHERE e.account.id = :accountId
              AND t.occurredAt >= :from
              AND t.occurredAt <= :to
            """,
        countQuery = """
            SELECT COUNT(e) FROM Entry e
            JOIN e.transaction t
            WHERE e.account.id = :accountId
              AND t.occurredAt >= :from
              AND t.occurredAt <= :to
            """
    )
    Page<Entry> findStatementEntries(
        @Param("accountId") UUID accountId,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to,
        Pageable pageable
    );
}