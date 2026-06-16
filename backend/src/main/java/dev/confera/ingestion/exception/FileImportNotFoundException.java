package dev.confera.ingestion.exception;

import dev.confera.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class FileImportNotFoundException extends ApiException {

    public FileImportNotFoundException(UUID id) {
        super("File import not found: " + id, HttpStatus.NOT_FOUND);
    }
}