package dev.confera.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record
RefreshRequest(@NotBlank String refreshToken) {}