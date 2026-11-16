package com.learnova.config.minio;


import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MinioBucketInitializer implements ApplicationRunner {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        String bucket = minioProperties.bucket();

        try {
            boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucket)
                    .build()
            );

            if (bucketExists) {
                log.info("MinIO bucket '{}' already exists", bucket);
                return;
            }

            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(bucket)
                    .build()
            );

            log.info("MinIO bucket '{}' created successfully", bucket);

        } catch (Exception exception) {
            throw new IllegalStateException(
                "Failed to initialize MinIO bucket: " + bucket,
                exception
            );
        }
    }
}
