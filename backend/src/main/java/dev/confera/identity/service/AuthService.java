package dev.confera.identity.service;

import dev.confera.identity.dto.LoginRequest;
import dev.confera.identity.dto.LoginResponse;
import dev.confera.identity.dto.RefreshRequest;
import dev.confera.identity.entity.RefreshToken;
import dev.confera.identity.entity.User;
import dev.confera.identity.repository.RefreshTokenRepository;
import dev.confera.identity.repository.UserRepository;
import dev.confera.identity.security.JwtService;
import dev.confera.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${confera.jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailWithTenant(request.email())
            .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is disabled");
        }

        if (!user.getTenant().isActive()) {
            throw new UnauthorizedException("Tenant account is disabled");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        return LoginResponse.of(accessToken, refreshToken, user.getRole().name());
    }

    @Transactional
    public LoginResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenWithUser(request.refreshToken())
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!stored.isValid()) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }

        stored.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        String accessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = createRefreshToken(user);

        return LoginResponse.of(accessToken, newRefreshToken, user.getRole().name());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByTokenWithUser(request.refreshToken())
            .filter(RefreshToken::isValid)
            .ifPresent(token -> {
                token.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(token);
            });
    }

    private String createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
            .user(user)
            .token(UUID.randomUUID().toString())
            .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
            .build();
        return refreshTokenRepository.save(token).getToken();
    }
}