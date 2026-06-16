package dev.confera.ledger.exception;

import dev.confera.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AccountAlreadyExistsException extends ApiException {

    public AccountAlreadyExistsException(String code) {
        super("Account with code '%s' already exists in this tenant".formatted(code), HttpStatus.CONFLICT);
    }
}