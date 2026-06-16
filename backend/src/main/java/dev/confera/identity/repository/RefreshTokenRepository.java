package dev.confera.identity.repository;

import dev.confera.identity.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.user u JOIN FETCH u.tenant WHERE rt.token = :token")
    Optional<RefreshToken> findByTokenWithUser(@Param("token") String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = CURRENT_TIMESTAMP WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") UUID userId);
}