package dev.confera.ledger.dto;

import dev.confera.ledger.entity.Direction;
import dev.confera.ledger.entity.Entry;

import java.util.UUID;

public record EntryResponse(
    UUID id,
    String accountCode,
    String accountName,
    long amountCents,
    Direction direction
) {
    public static EntryResponse from(Entry entry) {
        return new EntryResponse(
            entry.getId(),
            entry.getAccount().getCode(),
            entry.getAccount().getName(),
            entry.getAmountCents(),
            entry.getDirection()
        );
    }
}