package dev.confera.ledger.dto;

import dev.confera.ledger.entity.AccountType;
import dev.confera.ledger.entity.Direction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateAccountRequest(
    @NotBlank String name,
    @NotBlank @Pattern(regexp = "^[\\d.]+$", message = "code must follow numeric dot notation, e.g. 1.1.01") String code,
    @NotNull AccountType type,
    @NotNull Direction normalBalance,
    UUID parentId
) {}