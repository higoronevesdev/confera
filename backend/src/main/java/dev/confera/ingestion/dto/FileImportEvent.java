package dev.confera.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.confera.ingestion.entity.FileType;

import java.util.UUID;

public record FileImportEvent(
    @JsonProperty("importId")   UUID importId,
    @JsonProperty("tenantId")   UUID tenantId,
    @JsonProperty("storageKey") String storageKey,
    @JsonProperty("fileType")   FileType fileType
) {}