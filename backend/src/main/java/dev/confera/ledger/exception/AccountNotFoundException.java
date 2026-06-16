package dev.confera.ledger.exception;

import dev.confera.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AccountNotFoundException extends ApiException {

    public AccountNotFoundException(String code) {
        super("Account not found: " + code, HttpStatus.NOT_FOUND);
    }

    public AccountNotFoundException(UUID id) {
        super("Account not found: " + id, HttpStatus.NOT_FOUND);
    }
}