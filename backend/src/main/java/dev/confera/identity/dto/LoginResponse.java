package dev.confera.identity.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    String role
) {
    public static LoginResponse of(String accessToken, String refreshToken, String role) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", role);
    }
}