package dev.confera.ledger.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record PostTransactionRequest(
    @NotBlank String description,
    @NotNull OffsetDateTime occurredAt,
    String idempotencyKey,
    @NotNull @Size(min = 2, message = "A transaction must have at least 2 entries") List<@Valid EntryRequest> entries
) {}