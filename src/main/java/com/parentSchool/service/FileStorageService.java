package com.parentSchool.service;


import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.auto-create-bucket:true}")
    private boolean autoCreateBucket;

    @Value("${minio.url}")
    private String minioUrl;

    public void initializeBucket() {
        try {
            log.info("Checking if bucket '{}' exists...", bucketName);
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            log.info("Bucket '{}' exists: {}", bucketName, bucketExists);
            
            if (!bucketExists && autoCreateBucket) {
                log.info("Creating bucket '{}'", bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket '{}' created successfully", bucketName);
            }
        } catch (Exception e) {
            log.error("Error initializing bucket '{}': {}", bucketName, e.getMessage(), e);
            log.error("MinIO may not be running. Please start MinIO server at: {}", minioUrl);
            throw new RuntimeException("Failed to initialize MinIO bucket. Please ensure MinIO is running at " + minioUrl, e);
        }
    }

    public String uploadFile(MultipartFile file, String folder) throws Exception {
        log.info("Starting file upload - File: {}, Size: {}, Folder: {}", 
                file.getOriginalFilename(), file.getSize(), folder);
        log.info("MinIO Configuration - URL: {}, Bucket: {}", minioUrl, bucketName);
        
        initializeBucket();
        
        String fileName = generateFileName(file.getOriginalFilename());
        String objectName = folder + "/" + fileName;
        
        log.info("Generated object name: {}", objectName);
        
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            
            log.info("File uploaded successfully: {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            throw e;
        }
    }

    public InputStream downloadFile(String objectName) throws Exception {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build()
        );
    }

    public String getFileUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.http.Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(60 * 60 * 24) // 24 hours
                    .build()
            );
        } catch (Exception e) {
            log.warn("MinIO unavailable, returning placeholder URL for: {}", objectName);
            return "/placeholder-image.jpg";
        }
    }

    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build()
        );
        log.info("File deleted successfully: {}", objectName);
    }

    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public String copyFile(String sourceObjectName, String sourceFolder, String targetFolder) throws Exception {
        log.info("Copying file from {} to {}", sourceObjectName, targetFolder);
        
        // Générer un nouveau nom de fichier pour la copie
        String fileName = sourceObjectName.substring(sourceObjectName.lastIndexOf("/") + 1);
        String newObjectName = targetFolder + "/" + fileName;
        
        try {
            // Copier l'objet dans MinIO
            minioClient.copyObject(
                CopyObjectArgs.builder()
                    .bucket(bucketName)
                    .object(newObjectName)
                    .source(
                        CopySource.builder()
                            .bucket(bucketName)
                            .object(sourceObjectName)
                            .build()
                    )
                    .build()
            );
            
            log.info("File copied successfully: {} -> {}", sourceObjectName, newObjectName);
            return newObjectName;
        } catch (Exception e) {
            log.error("Failed to copy file: {}", e.getMessage(), e);
            throw e;
        }
    }
}
