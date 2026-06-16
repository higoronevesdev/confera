package dev.confera.ingestion.service;

import dev.confera.config.RabbitConfig;
import dev.confera.ingestion.dto.FileImportEvent;
import dev.confera.ingestion.dto.FileImportResponse;
import dev.confera.ingestion.entity.FileImport;
import dev.confera.ingestion.entity.FileType;
import dev.confera.ingestion.entity.ImportStatus;
import dev.confera.ingestion.exception.FileImportNotFoundException;
import dev.confera.ingestion.repository.FileImportRepository;
import dev.confera.shared.outbox.OutboxEvent;
import dev.confera.shared.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileImportService {

    private final FileImportRepository fileImportRepository;
    private final StorageService storageService;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public FileImportResponse importFile(UUID tenantId, UUID userId, MultipartFile file) throws IOException {
        byte[] content  = file.getBytes();
        FileType type   = detectFileType(file.getOriginalFilename(), content);
        String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        String storageKey = storageService.uploadFile(
            tenantId, file.getOriginalFilename(), new ByteArrayInputStream(content), mimeType, content.length);

        FileImport fileImport = FileImport.builder()
            .tenantId(tenantId)
            .userId(userId)
            .fileName(file.getOriginalFilename())
            .fileType(type)
            .storageKey(storageKey)
            .status(ImportStatus.PENDING)
            .build();
        FileImport saved = fileImportRepository.save(fileImport);

        // Outbox event — same @Transactional block
        FileImportEvent event = new FileImportEvent(saved.getId(), tenantId, storageKey, type);
        String payload = buildPayload(event);
        outboxEventRepository.save(OutboxEvent.builder()
            .aggregateType("FileImport")
            .aggregateId(saved.getId().toString())
            .eventType("FileImportCreated")
            .routingKey(RabbitConfig.ROUTING_KEY)
            .payload(payload)
            .build());

        return FileImportResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public FileImportResponse getImport(UUID tenantId, UUID importId) {
        return fileImportRepository.findByIdAndTenantId(importId, tenantId)
            .map(FileImportResponse::from)
            .orElseThrow(() -> new FileImportNotFoundException(importId));
    }

    @Transactional(readOnly = true)
    public Page<FileImportResponse> listImports(UUID tenantId, Pageable pageable) {
        return fileImportRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
            .map(FileImportResponse::from);
    }

    private FileType detectFileType(String fileName, byte[] content) {
        if (fileName == null) return FileType.CNAB_240;
        String name = fileName.toLowerCase();
        if (name.endsWith(".ofx")) return FileType.OFX;
        if (name.endsWith(".csv")) return FileType.CSV_ADQUIRENTE;
        // Detect CNAB variant by line length of first line
        int newline = -1;
        for (int i = 0; i < content.length; i++) {
            if (content[i] == '\n') { newline = i; break; }
        }
        if (newline > 0) {
            int lineLen = content[newline - 1] == '\r' ? newline - 1 : newline;
            if (lineLen == 400) return FileType.CNAB_400;
        }
        return FileType.CNAB_240;
    }

    private String buildPayload(FileImportEvent event) {
        return "{\"importId\":\"%s\",\"tenantId\":\"%s\",\"storageKey\":\"%s\",\"fileType\":\"%s\"}"
            .formatted(event.importId(), event.tenantId(), event.storageKey(), event.fileType().name());
    }
}