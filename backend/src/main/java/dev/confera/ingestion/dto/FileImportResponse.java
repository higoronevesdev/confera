package dev.confera.ingestion.dto;

import dev.confera.ingestion.entity.FileImport;
import dev.confera.ingestion.entity.FileType;
import dev.confera.ingestion.entity.ImportStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record FileImportResponse(
    UUID id,
    String fileName,
    FileType fileType,
    ImportStatus status,
    Integer totalRecords,
    Integer processedRecords,
    String errorMessage,
    LocalDateTime createdAt
) {
    public static FileImportResponse from(FileImport entity) {
        return new FileImportResponse(
            entity.getId(),
            entity.getFileName(),
            entity.getFileType(),
            entity.getStatus(),
            entity.getTotalRecords(),
            entity.getProcessedRecords(),
            entity.getErrorMessage(),
            entity.getCreatedAt()
        );
    }
}