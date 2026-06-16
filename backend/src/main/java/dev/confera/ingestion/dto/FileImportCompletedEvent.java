package dev.confera.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record FileImportCompletedEvent(
    @JsonProperty("importId") UUID importId,
    @JsonProperty("tenantId") UUID tenantId,
    @JsonProperty("totalRecords") int totalRecords
) {}