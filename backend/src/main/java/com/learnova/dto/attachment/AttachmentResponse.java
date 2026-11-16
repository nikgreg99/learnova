package com.learnova.dto.attachment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(
    description = "Response containing attachment metadata and its associated course or lesson"
)
public record AttachmentResponse(

    @Schema(description = "Unique identifier of the attachment", example = "1")
    Long id,

    @Schema(description = "Original name of the uploaded file", example = "course-material.pdf")
    String originalFilename,

    @Schema(description = "MIME type of the uploaded file", example = "application/pdf")
    String contentType,

    @Schema(description = "File size in bytes", example = "204800")
    Long size,

    @Schema(description = "Identifier of the user who uploaded the attachment", example = "user-123")
    String uploadedBy,

    @Schema(description = "Identifier of the associated course", example = "10", nullable = true)
    Long courseId,

    @Schema(description = "Identifier of the associated lesson", example = "25", nullable = true)
    Long lessonId,

    @Schema(description = "Date and time when the attachment was created", example = "2026-11-16T15:30:00")
    LocalDateTime createdAt

) {
}
