package com.learnova.storage;

import com.learnova.config.minio.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService{

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public void upload(String storageKey, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(minioProperties.bucket())
                    .object(storageKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                "Failed to upload file: " + storageKey,
                exception
            );
        }
    }

    @Override
    public InputStream download(String storageKey) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(minioProperties.bucket())
                    .object(storageKey)
                    .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                "Failed to download file: " + storageKey,
                exception
            );
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(minioProperties.bucket())
                    .object(storageKey)
                    .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                "Failed to delete file: " + storageKey,
                exception
            );
        }
    }
}
