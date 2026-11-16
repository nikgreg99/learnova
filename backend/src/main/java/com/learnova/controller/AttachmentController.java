
package com.learnova.controller;

import com.learnova.dto.attachment.AttachmentDownload;
import com.learnova.dto.attachment.AttachmentResponse;
import com.learnova.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@Tag(
    name = "Attachments",
    description = "Endpoints for uploading, retrieving, downloading and deleting attachments"
)
@SecurityRequirement(name = "bearerAuth")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(
        summary = "Upload an attachment",
        description = """
            Uploads a file and optionally associates it with a course or lesson.
            Only instructors and administrators can upload attachments.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Attachment successfully uploaded"),
        @ApiResponse(responseCode = "400", description = "Invalid file or request parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Course or lesson not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AttachmentResponse> upload(
        @Parameter(description = "File to upload", required = true)
        @RequestPart("file") MultipartFile file,

        @Parameter(description = "Optional course ID")
        @RequestParam(required = false) Long courseId,

        @Parameter(description = "Optional lesson ID")
        @RequestParam(required = false) Long lessonId
    ) {
        AttachmentResponse response = attachmentService.upload(
            file,
            courseId,
            lessonId
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/courses/{courseId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(
        summary = "Get course attachments",
        description = "Returns all attachments associated with a specific course."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Attachments successfully retrieved"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public ResponseEntity<List<AttachmentResponse>> getCourseAttachments(
        @Parameter(
            description = "Course ID",
            in = ParameterIn.PATH,
            required = true
        )
        @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(
            attachmentService.getCourseAttachments(courseId)
        );
    }

    @GetMapping("/lessons/{lessonId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(
        summary = "Get lesson attachments",
        description = "Returns all attachments associated with a specific lesson."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Attachments successfully retrieved"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    public ResponseEntity<List<AttachmentResponse>> getLessonAttachments(
        @Parameter(
            description = "Lesson ID",
            in = ParameterIn.PATH,
            required = true
        )
        @PathVariable Long lessonId
    ) {
        return ResponseEntity.ok(
            attachmentService.getLessonAttachments(lessonId)
        );
    }

    @GetMapping("/{attachmentId}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(
        summary = "Download an attachment",
        description = "Downloads an attachment using its ID. The original filename is preserved."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File successfully downloaded"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    public ResponseEntity<Resource> download(
        @Parameter(
            description = "Attachment ID",
            in = ParameterIn.PATH,
            required = true
        )
        @PathVariable Long attachmentId
    ) {
        AttachmentDownload download =
            attachmentService.download(attachmentId);

        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(download.originalFilename(), StandardCharsets.UTF_8)
            .build();

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(download.contentType()))
            .contentLength(download.size())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(new InputStreamResource(download.inputStream()));
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(
        summary = "Delete an attachment",
        description = "Deletes an attachment and its associated stored file."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Attachment successfully deleted"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    public ResponseEntity<Void> delete(
        @Parameter(
            description = "Attachment ID",
            in = ParameterIn.PATH,
            required = true
        )
        @PathVariable Long attachmentId
    ) {
        attachmentService.delete(attachmentId);
        return ResponseEntity.noContent().build();
    }
}
