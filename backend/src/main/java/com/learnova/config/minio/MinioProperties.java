package com.learnova.config.minio;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "minio")
@Validated
public record MinioProperties(
    String endpoint,
    String accessKey,
    String secretKey,
    String bucket
) {
}
