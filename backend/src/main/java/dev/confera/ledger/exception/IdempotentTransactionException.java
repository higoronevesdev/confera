package dev.confera.ledger.exception;

import dev.confera.ledger.dto.TransactionResponse;

public class IdempotentTransactionException extends RuntimeException {

    private final TransactionResponse existing;

    public IdempotentTransactionException(TransactionResponse existing) {
        super("Transaction already exists for idempotency key");
        this.existing = existing;
    }

    public TransactionResponse getExisting() {
        return existing;
    }
}