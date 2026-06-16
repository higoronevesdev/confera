package dev.confera.ingestion.exception;

import dev.confera.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

public class StorageException extends ApiException {

    public StorageException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}