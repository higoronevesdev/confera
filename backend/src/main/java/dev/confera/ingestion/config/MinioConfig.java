package dev.confera.ingestion.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@RequiredArgsConstructor
@Slf4j
public class MinioConfig {

    private final MinioProperties props;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(props.getUrl())
            .credentials(props.getAccessKey(), props.getSecretKey())
            .build();
    }

    @Bean
    public CommandLineRunner ensureBucketExists(MinioClient minioClient) {
        return args -> {
            boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(props.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(props.getBucket()).build());
                log.info("MinIO bucket '{}' created", props.getBucket());
            }
        };
    }
}