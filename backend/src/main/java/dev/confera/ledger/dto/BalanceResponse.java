package dev.confera.ledger.dto;

import java.util.UUID;

public record BalanceResponse(
    UUID accountId,
    String accountCode,
    String accountName,
    long balanceCents,
    long totalDebitCents,
    long totalCreditCents
) {}