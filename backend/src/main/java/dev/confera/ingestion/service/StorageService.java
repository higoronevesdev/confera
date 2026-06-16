package dev.confera.ingestion.service;

import dev.confera.ingestion.config.MinioProperties;
import dev.confera.ingestion.exception.StorageException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public String uploadFile(UUID tenantId, String fileName, InputStream content,
                             String contentType, long size) {
        String key = "imports/%s/%s-%s".formatted(tenantId, UUID.randomUUID(), fileName);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(key)
                .stream(content, size, -1)
                .contentType(contentType)
                .build());
            return key;
        } catch (Exception e) {
            throw new StorageException("Failed to upload file '%s'".formatted(fileName), e);
        }
    }

    public InputStream downloadFile(String storageKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(storageKey)
                .build());
        } catch (Exception e) {
            throw new StorageException("Failed to download file '%s'".formatted(storageKey), e);
        }
    }
}