package dev.confera.identity.security;

import dev.confera.identity.entity.Role;
import dev.confera.identity.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${confera.jwt.secret}")
    private String secret;

    @Value("${confera.jwt.expiration}")
    private long expiration;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("tenantId", user.getTenant().getId().toString())
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(signingKey())
            .compact();
    }

    public UserPrincipal extractPrincipal(String token) {
        Claims claims = parseClaims(token);
        return new UserPrincipal(
            UUID.fromString(claims.getSubject()),
            UUID.fromString(claims.get("tenantId", String.class)),
            claims.get("email", String.class),
            Role.valueOf(claims.get("role", String.class))
        );
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}