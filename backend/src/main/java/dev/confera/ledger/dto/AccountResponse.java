package dev.confera.ledger.dto;

import dev.confera.ledger.entity.Account;
import dev.confera.ledger.entity.AccountType;
import dev.confera.ledger.entity.Direction;

import java.util.UUID;

public record AccountResponse(
    UUID id,
    String name,
    String code,
    AccountType type,
    Direction normalBalance,
    long balanceCents
) {
    public static AccountResponse from(Account account, long balanceCents) {
        return new AccountResponse(
            account.getId(),
            account.getName(),
            account.getCode(),
            account.getType(),
            account.getNormalBalance(),
            balanceCents
        );
    }
}