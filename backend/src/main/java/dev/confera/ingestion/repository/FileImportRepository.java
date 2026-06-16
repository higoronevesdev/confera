package dev.confera.ingestion.repository;

import dev.confera.ingestion.entity.FileImport;
import dev.confera.ingestion.entity.ImportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileImportRepository extends JpaRepository<FileImport, UUID> {

    Optional<FileImport> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<FileImport> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    long countByTenantIdAndStatus(UUID tenantId, ImportStatus status);
}