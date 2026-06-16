package dev.confera.ledger.dto;

import dev.confera.ledger.entity.Direction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EntryRequest(
    @NotBlank String accountCode,
    @Positive long amountCents,
    @NotNull Direction direction
) {}