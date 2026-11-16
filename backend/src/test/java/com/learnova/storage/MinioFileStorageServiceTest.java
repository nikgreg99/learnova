package com.learnova.storage;

import com.learnova.config.minio.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioFileStorageServiceTest {

    private static final String BUCKET = "learnova-files";
    private static final String STORAGE_KEY = "test-file-123";
    private static final String CONTENT_TYPE = "text/plain";

    @Mock
    private MinioClient minioClient;

    private MinioFileStorageService storageService;

    @BeforeEach
    void setUp() {
        MinioProperties properties = new MinioProperties(
            "http://localhost:9000",
            "learnova",
            "password",
            BUCKET
        );

        storageService = new MinioFileStorageService(
            minioClient,
            properties
        );
    }

    @Test
    void shouldUploadFile() throws Exception {
        InputStream inputStream = new ByteArrayInputStream(
            "test content".getBytes()
        );

        storageService.upload(
            STORAGE_KEY,
            inputStream,
            12,
            CONTENT_TYPE
        );

        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void shouldThrowExceptionWhenUploadFails() throws Exception {
        InputStream inputStream = new ByteArrayInputStream(
            "test content".getBytes()
        );

        when(minioClient.putObject(any(PutObjectArgs.class)))
            .thenThrow(new RuntimeException("Upload failed"));

        assertThrows(
            IllegalStateException.class,
            () -> storageService.upload(
                STORAGE_KEY,
                inputStream,
                12,
                CONTENT_TYPE
            )
        );

        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void shouldDownloadFile() throws Exception {
        GetObjectResponse expectedResponse = org.mockito.Mockito.mock(
            GetObjectResponse.class
        );

        doReturn(expectedResponse)
            .when(minioClient)
            .getObject(any(GetObjectArgs.class));

        InputStream result = storageService.download(STORAGE_KEY);

        assertSame(expectedResponse, result);

        verify(minioClient).getObject(any(GetObjectArgs.class));
    }

    @Test
    void shouldThrowExceptionWhenDownloadFails() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class)))
            .thenThrow(new RuntimeException("Download failed"));

        assertThrows(
            IllegalStateException.class,
            () -> storageService.download(STORAGE_KEY)
        );

        verify(minioClient).getObject(any(GetObjectArgs.class));
    }

    @Test
    void shouldDeleteFile() throws Exception {
        storageService.delete(STORAGE_KEY);

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void shouldThrowExceptionWhenDeleteFails() throws Exception {
        doThrow(new RuntimeException("Delete failed"))
            .when(minioClient)
            .removeObject(any(RemoveObjectArgs.class));

        assertThrows(
            IllegalStateException.class,
            () -> storageService.delete(STORAGE_KEY)
        );

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }
}
