package dev.confera.ledger.exception;

import dev.confera.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class TransactionNotFoundException extends ApiException {

    public TransactionNotFoundException(UUID id) {
        super("Transaction not found: " + id, HttpStatus.NOT_FOUND);
    }
}