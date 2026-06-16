package dev.confera.identity.repository;

import dev.confera.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u JOIN FETCH u.tenant WHERE u.email = :email")
    Optional<User> findByEmailWithTenant(@Param("email") String email);

    boolean existsByEmail(String email);
}