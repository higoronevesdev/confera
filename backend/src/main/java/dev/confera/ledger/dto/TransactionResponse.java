package dev.confera.ledger.dto;

import dev.confera.ledger.entity.Entry;
import dev.confera.ledger.entity.Transaction;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    String description,
    OffsetDateTime occurredAt,
    List<EntryResponse> entries
) {
    public static TransactionResponse from(Transaction transaction, List<Entry> entries) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getDescription(),
            transaction.getOccurredAt(),
            entries.stream().map(EntryResponse::from).toList()
        );
    }

    public static TransactionResponse from(Transaction transaction) {
        return from(transaction, transaction.getEntries());
    }
}