package com.parentSchool.config;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        log.info("Configuring MinIO client with URL: {}, Access Key: {}", minioUrl, accessKey);
        
        if (minioUrl == null || minioUrl.isEmpty()) {
            log.error("MinIO URL is not configured!");
            throw new IllegalStateException("MinIO URL must be configured");
        }
        
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }
}
