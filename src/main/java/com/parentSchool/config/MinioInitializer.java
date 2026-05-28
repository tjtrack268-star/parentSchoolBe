package com.parentSchool.config;
import com.parentSchool.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioInitializer implements CommandLineRunner {
    
    private final FileStorageService fileStorageService;
    
    @Override
    public void run(String... args) {
        try {
            log.info("Initializing MinIO buckets...");
            fileStorageService.initializeBucket();
            log.info("MinIO buckets initialized successfully");
        } catch (Exception e) {
            log.warn("MinIO initialization failed: {}. MinIO may not be running.", e.getMessage());
            log.info("To start MinIO, run: start-minio.bat");
        }
    }
}
