package com.learnova.service;

import com.learnova.config.security.SecurityUtils;
import com.learnova.dto.attachment.AttachmentDownload;
import com.learnova.dto.attachment.AttachmentResponse;
import com.learnova.exception.AttachmentNotFoundException;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.exception.LessonNotFoundException;
import com.learnova.handler.AttachmentStorageTransactionHandler;
import com.learnova.mapper.AttachmentMapper;
import com.learnova.model.Attachment;
import com.learnova.model.Course;
import com.learnova.model.Lesson;
import com.learnova.repository.AttachmentRepository;
import com.learnova.repository.CourseRepository;
import com.learnova.repository.LessonRepository;
import com.learnova.service.security.AttachmentSecurityService;
import com.learnova.storage.FileStorageService;
import com.learnova.validator.AttachmentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private AttachmentMapper attachmentMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AttachmentValidator attachmentValidator;

    @Mock
    private AttachmentSecurityService attachmentSecurityService;

    @Mock
    private AttachmentStorageTransactionHandler attachmentStorageTransactionHandler;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private AttachmentService attachmentService;

    private AttachmentResponse attachmentResponse;

    @BeforeEach
    void setUp() {
        attachmentResponse = new AttachmentResponse(
            1L,
            "document.pdf",
            "application/pdf",
            2048L,
            "user-123",
            10L,
            null,
            null
        );
    }

    // =========================
    // UPLOAD
    // =========================

    @Test
    void shouldUploadAttachmentSuccessfully() throws IOException {

        Course course = mock(Course.class);

        when(courseRepository.findById(10L))
            .thenReturn(Optional.of(course));

        when(file.getOriginalFilename()).thenReturn("document.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(2048L);
        when(file.getInputStream())
            .thenReturn(new ByteArrayInputStream(new byte[2048]));

        when(attachmentRepository.save(any(Attachment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(attachmentMapper.toResponse(any(Attachment.class)))
            .thenReturn(attachmentResponse);

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {

            securityUtils.when(SecurityUtils::getCurrentKeycloakId)
                .thenReturn("user-123");

            AttachmentResponse result = attachmentService.upload(
                file,
                10L,
                null
            );

            assertThat(result).isEqualTo(attachmentResponse);

            ArgumentCaptor<Attachment> captor =
                ArgumentCaptor.forClass(Attachment.class);

            verify(attachmentRepository).save(captor.capture());

            Attachment savedAttachment = captor.getValue();

            assertThat(savedAttachment.getOriginalFilename())
                .isEqualTo("document.pdf");

            assertThat(savedAttachment.getContentType())
                .isEqualTo("application/pdf");

            assertThat(savedAttachment.getSize())
                .isEqualTo(2048L);

            assertThat(savedAttachment.getUploadedBy())
                .isEqualTo("user-123");

            assertThat(savedAttachment.getStorageKey())
                .isNotBlank();

            assertThat(savedAttachment.getCourse())
                .isEqualTo(course);

            assertThat(savedAttachment.getLesson())
                .isNull();

            verify(fileStorageService).upload(
                anyString(),
                any(InputStream.class),
                eq(2048L),
                eq("application/pdf")
            );

            verify(attachmentStorageTransactionHandler)
                .registerUploadRollback(anyString());

            verify(attachmentMapper).toResponse(savedAttachment);
        }
    }

    @Test
    void shouldUploadAttachmentAssociatedWithLesson() throws IOException {

        Lesson lesson = mock(Lesson.class);

        when(lessonRepository.findById(20L))
            .thenReturn(Optional.of(lesson));

        when(file.getOriginalFilename()).thenReturn("lesson.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(1024L);
        when(file.getInputStream())
            .thenReturn(new ByteArrayInputStream(new byte[1024]));

        when(attachmentRepository.save(any(Attachment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(attachmentMapper.toResponse(any(Attachment.class)))
            .thenReturn(attachmentResponse);

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {

            securityUtils.when(SecurityUtils::getCurrentKeycloakId)
                .thenReturn("user-123");

            attachmentService.upload(file, null, 20L);

            ArgumentCaptor<Attachment> captor =
                ArgumentCaptor.forClass(Attachment.class);

            verify(attachmentRepository).save(captor.capture());

            assertThat(captor.getValue().getLesson())
                .isEqualTo(lesson);

            assertThat(captor.getValue().getCourse())
                .isNull();

            verify(lessonRepository).findById(20L);
        }
    }

    @Test
    void shouldThrowExceptionWhenUploadValidationFails() {

        doThrow(new IllegalArgumentException("Invalid file"))
            .when(attachmentValidator)
            .validateUpload(file, null, null);

        assertThatThrownBy(() ->
            attachmentService.upload(file, null, null)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid file");

        verifyNoInteractions(fileStorageService);
        verifyNoInteractions(attachmentRepository);
    }

    @Test
    void shouldThrowExceptionWhenCourseDoesNotExist() {

        when(courseRepository.findById(10L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            attachmentService.upload(file, 10L, null)
        )
            .isInstanceOf(CourseNotFoundException.class);

        verifyNoInteractions(fileStorageService);
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenLessonDoesNotExist() {

        when(lessonRepository.findById(20L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            attachmentService.upload(file, null, 20L)
        )
            .isInstanceOf(LessonNotFoundException.class);

        verifyNoInteractions(fileStorageService);
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenFileCannotBeRead() throws IOException {

        when(file.getOriginalFilename()).thenReturn("document.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getInputStream())
            .thenThrow(new IOException("Read error"));

        assertThatThrownBy(() ->
            attachmentService.upload(file, null, null)
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to read uploaded file")
            .hasCauseInstanceOf(IOException.class);

        verify(fileStorageService, never()).upload(
            anyString(),
            any(InputStream.class),
            anyLong(),
            anyString()
        );

        verify(attachmentRepository, never()).save(any());
        verify(attachmentStorageTransactionHandler, never())
            .registerUploadRollback(anyString());
    }

    @Test
    void shouldUseDefaultContentTypeWhenContentTypeIsNull()
        throws IOException {

        when(file.getOriginalFilename()).thenReturn("document.bin");
        when(file.getContentType()).thenReturn(null);
        when(file.getSize()).thenReturn(100L);
        when(file.getInputStream())
            .thenReturn(new ByteArrayInputStream(new byte[100]));

        when(attachmentRepository.save(any(Attachment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(attachmentMapper.toResponse(any(Attachment.class)))
            .thenReturn(attachmentResponse);

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {

            securityUtils.when(SecurityUtils::getCurrentKeycloakId)
                .thenReturn("user-123");

            attachmentService.upload(file, null, null);

            verify(fileStorageService).upload(
                anyString(),
                any(InputStream.class),
                eq(100L),
                eq("application/octet-stream")
            );
        }
    }

    // =========================
    // GET COURSE ATTACHMENTS
    // =========================

    @Test
    void shouldReturnCourseAttachments() {

        Attachment attachment = mock(Attachment.class);

        when(courseRepository.existsById(10L)).thenReturn(true);

        when(attachmentRepository
            .findAllByCourse_IdOrderByCreatedAtDesc(10L))
            .thenReturn(List.of(attachment));

        when(attachmentMapper.toResponse(attachment))
            .thenReturn(attachmentResponse);

        List<AttachmentResponse> result =
            attachmentService.getCourseAttachments(10L);

        assertThat(result)
            .hasSize(1)
            .containsExactly(attachmentResponse);

        verify(courseRepository).existsById(10L);

        verify(attachmentRepository)
            .findAllByCourse_IdOrderByCreatedAtDesc(10L);

        verify(attachmentMapper).toResponse(attachment);
    }

    @Test
    void shouldReturnEmptyListWhenCourseHasNoAttachments() {

        when(courseRepository.existsById(10L)).thenReturn(true);

        when(attachmentRepository
            .findAllByCourse_IdOrderByCreatedAtDesc(10L))
            .thenReturn(List.of());

        List<AttachmentResponse> result =
            attachmentService.getCourseAttachments(10L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenCourseDoesNotExistForRetrieval() {

        when(courseRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() ->
            attachmentService.getCourseAttachments(10L)
        )
            .isInstanceOf(CourseNotFoundException.class);

        verify(attachmentRepository, never())
            .findAllByCourse_IdOrderByCreatedAtDesc(anyLong());
    }

    // =========================
    // GET LESSON ATTACHMENTS
    // =========================

    @Test
    void shouldReturnLessonAttachments() {

        Attachment attachment = mock(Attachment.class);

        when(lessonRepository.existsById(20L)).thenReturn(true);

        when(attachmentRepository
            .findAllByLesson_IdOrderByCreatedAtDesc(20L))
            .thenReturn(List.of(attachment));

        when(attachmentMapper.toResponse(attachment))
            .thenReturn(attachmentResponse);

        List<AttachmentResponse> result =
            attachmentService.getLessonAttachments(20L);

        assertThat(result)
            .hasSize(1)
            .containsExactly(attachmentResponse);

        verify(lessonRepository).existsById(20L);

        verify(attachmentRepository)
            .findAllByLesson_IdOrderByCreatedAtDesc(20L);

        verify(attachmentMapper).toResponse(attachment);
    }

    @Test
    void shouldThrowExceptionWhenLessonDoesNotExistForRetrieval() {

        when(lessonRepository.existsById(20L)).thenReturn(false);

        assertThatThrownBy(() ->
            attachmentService.getLessonAttachments(20L)
        )
            .isInstanceOf(LessonNotFoundException.class);

        verify(attachmentRepository, never())
            .findAllByLesson_IdOrderByCreatedAtDesc(anyLong());
    }

    // =========================
    // DELETE
    // =========================

    @Test
    void shouldDeleteAttachmentSuccessfully() {

        Attachment attachment = mock(Attachment.class);

        when(attachmentRepository.findById(1L))
            .thenReturn(Optional.of(attachment));

        when(attachment.getStorageKey())
            .thenReturn("storage-key-123");

        attachmentService.delete(1L);

        verify(attachmentSecurityService)
            .validateOwnership(attachment);

        verify(attachmentRepository).delete(attachment);

        verify(attachmentStorageTransactionHandler)
            .registerDeleteAfterCommit("storage-key-123");
    }

    @Test
    void shouldThrowExceptionWhenAttachmentDoesNotExistForDeletion() {

        when(attachmentRepository.findById(1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            attachmentService.delete(1L)
        )
            .isInstanceOf(AttachmentNotFoundException.class);

        verify(attachmentRepository, never()).delete(any());
        verifyNoInteractions(attachmentSecurityService);
        verifyNoInteractions(attachmentStorageTransactionHandler);
    }

    @Test
    void shouldNotDeleteAttachmentWhenOwnershipValidationFails() {

        Attachment attachment = mock(Attachment.class);

        when(attachmentRepository.findById(1L))
            .thenReturn(Optional.of(attachment));

        doThrow(new org.springframework.security.access.AccessDeniedException(
            "Access denied"
        ))
            .when(attachmentSecurityService)
            .validateOwnership(attachment);

        assertThatThrownBy(() ->
            attachmentService.delete(1L)
        )
            .isInstanceOf(
                org.springframework.security.access.AccessDeniedException.class
            );

        verify(attachmentRepository, never()).delete(any());

        verify(attachmentStorageTransactionHandler, never())
            .registerDeleteAfterCommit(anyString());
    }

    // =========================
    // DOWNLOAD
    // =========================

    @Test
    void shouldDownloadAttachmentSuccessfully() {

        Attachment attachment = mock(Attachment.class);

        InputStream inputStream =
            new ByteArrayInputStream(new byte[]{1, 2, 3});

        when(attachmentRepository.findById(1L))
            .thenReturn(Optional.of(attachment));

        when(attachment.getStorageKey())
            .thenReturn("storage-key-123");

        when(attachment.getOriginalFilename())
            .thenReturn("document.pdf");

        when(attachment.getContentType())
            .thenReturn("application/pdf");

        when(attachment.getSize())
            .thenReturn(3L);

        when(fileStorageService.download("storage-key-123"))
            .thenReturn(inputStream);

        AttachmentDownload result =
            attachmentService.download(1L);

        assertThat(result).isNotNull();
        assertThat(result.inputStream()).isSameAs(inputStream);
        assertThat(result.originalFilename()).isEqualTo("document.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.size()).isEqualTo(3L);

        verify(fileStorageService).download("storage-key-123");
    }

    @Test
    void shouldThrowExceptionWhenAttachmentDoesNotExistForDownload() {

        when(attachmentRepository.findById(1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            attachmentService.download(1L)
        )
            .isInstanceOf(AttachmentNotFoundException.class);

        verifyNoInteractions(fileStorageService);
    }
}
