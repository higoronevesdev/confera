package dev.confera.reconciliation.exception;

import dev.confera.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class DiscrepancyNotFoundException extends ApiException {
    public DiscrepancyNotFoundException(UUID id) {
        super("Discrepancy not found: " + id, HttpStatus.NOT_FOUND);
    }
}