package com.learnova.dto.attachment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.InputStream;

@Schema(description = "Internal representation of an attachment prepared for download")
public record AttachmentDownload(

    @Schema(hidden = true)
    InputStream inputStream,

    @Schema(
        description = "Original name of the downloaded file",
        example = "course-material.pdf"
    )
    String originalFilename,

    @Schema(
        description = "MIME type of the downloaded file",
        example = "application/pdf"
    )
    String contentType,

    @Schema(
        description = "File size in bytes",
        example = "204800"
    )
    long size

) {
}
