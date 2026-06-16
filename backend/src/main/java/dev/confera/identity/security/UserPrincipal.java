package dev.confera.identity.security;

import dev.confera.identity.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record UserPrincipal(
    UUID userId,
    UUID tenantId,
    String email,
    Role role
) {
    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}