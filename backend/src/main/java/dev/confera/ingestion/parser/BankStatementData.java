package dev.confera.ingestion.parser;

import java.time.LocalDate;

public record BankStatementData(
    String bankCode,
    String agency,
    String accountNumber,
    LocalDate transactionDate,
    LocalDate valueDate,
    Long amountCents,
    String description,
    String documentNumber
) {}