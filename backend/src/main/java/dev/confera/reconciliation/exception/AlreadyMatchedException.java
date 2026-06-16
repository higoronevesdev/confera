package dev.confera.reconciliation.exception;

import dev.confera.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AlreadyMatchedException extends ApiException {
    public AlreadyMatchedException(String entityType, UUID id) {
        super("%s %s is already matched".formatted(entityType, id), HttpStatus.CONFLICT);
    }
}