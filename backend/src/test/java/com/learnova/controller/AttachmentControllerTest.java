
package com.learnova.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnova.config.ratelimit.RateLimitInterceptor;
import com.learnova.config.security.SecurityConfig;
import com.learnova.dto.attachment.AttachmentDownload;
import com.learnova.dto.attachment.AttachmentResponse;
import com.learnova.service.AttachmentService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AttachmentController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AttachmentControllerTest {

    private static final Long ATTACHMENT_ID = 1L;
    private static final Long COURSE_ID = 10L;
    private static final Long LESSON_ID = 20L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttachmentService attachmentService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    private AttachmentResponse courseAttachmentResponse;
    private AttachmentResponse lessonAttachmentResponse;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any()))
            .thenReturn(true);

        courseAttachmentResponse = new AttachmentResponse(
            ATTACHMENT_ID,
            "document.pdf",
            "application/pdf",
            1024L,
            "user-123",
            COURSE_ID,
            null,
            LocalDateTime.now()
        );

        lessonAttachmentResponse = new AttachmentResponse(
            ATTACHMENT_ID,
            "lesson-document.pdf",
            "application/pdf",
            2048L,
            "user-123",
            null,
            LESSON_ID,
            LocalDateTime.now()
        );
    }

    @Test
    void upload_shouldReturn201WhenInstructor() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "test content".getBytes(StandardCharsets.UTF_8)
        );

        when(attachmentService.upload(any(), eq(COURSE_ID), isNull()))
            .thenReturn(courseAttachmentResponse);

        mockMvc.perform(multipart("/api/v1/attachments")
                .file(file)
                .param("courseId", COURSE_ID.toString())
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_INSTRUCTOR")
                )))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(ATTACHMENT_ID))
            .andExpect(jsonPath("$.originalFilename").value("document.pdf"))
            .andExpect(jsonPath("$.contentType").value("application/pdf"))
            .andExpect(jsonPath("$.courseId").value(COURSE_ID));

        verify(attachmentService).upload(any(), eq(COURSE_ID), isNull());
    }

    @Test
    void upload_shouldReturn201WhenAdmin() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "test content".getBytes(StandardCharsets.UTF_8)
        );

        when(attachmentService.upload(any(), eq(COURSE_ID), isNull()))
            .thenReturn(courseAttachmentResponse);

        mockMvc.perform(multipart("/api/v1/attachments")
                .file(file)
                .param("courseId", COURSE_ID.toString())
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_ADMIN")
                )))
            .andExpect(status().isCreated());

        verify(attachmentService).upload(any(), eq(COURSE_ID), isNull());
    }

    @Test
    void upload_shouldReturn403WhenStudent() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "test content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/attachments")
                .file(file)
                .param("courseId", COURSE_ID.toString())
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_STUDENT")
                )))
            .andExpect(status().isForbidden());
    }

    @Test
    void upload_shouldReturn401WhenUnauthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "test content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/attachments")
                .file(file)
                .param("courseId", COURSE_ID.toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getCourseAttachments_shouldReturn200WhenStudent() throws Exception {
        when(attachmentService.getCourseAttachments(COURSE_ID))
            .thenReturn(List.of(courseAttachmentResponse));

        mockMvc.perform(get(
                "/api/v1/attachments/courses/{courseId}",
                COURSE_ID
            )
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_STUDENT")
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(ATTACHMENT_ID))
            .andExpect(jsonPath("$[0].originalFilename").value("document.pdf"));

        verify(attachmentService).getCourseAttachments(COURSE_ID);
    }

    @Test
    void getCourseAttachments_shouldReturn200WhenInstructor() throws Exception {
        when(attachmentService.getCourseAttachments(COURSE_ID))
            .thenReturn(List.of(courseAttachmentResponse));

        mockMvc.perform(get(
                "/api/v1/attachments/courses/{courseId}",
                COURSE_ID
            )
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_INSTRUCTOR")
                )))
            .andExpect(status().isOk());

        verify(attachmentService).getCourseAttachments(COURSE_ID);
    }

    @Test
    void getCourseAttachments_shouldReturn403WhenUnauthorisedRole() throws Exception {
        mockMvc.perform(get(
                "/api/v1/attachments/courses/{courseId}",
                COURSE_ID
            )
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_UNKNOWN")
                )))
            .andExpect(status().isForbidden());
    }

    @Test
    void getCourseAttachments_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(
                "/api/v1/attachments/courses/{courseId}",
                COURSE_ID
            ))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getLessonAttachments_shouldReturn200WhenStudent() throws Exception {
        when(attachmentService.getLessonAttachments(LESSON_ID))
            .thenReturn(List.of(lessonAttachmentResponse));

        mockMvc.perform(get(
                "/api/v1/attachments/lessons/{lessonId}",
                LESSON_ID
            )
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_STUDENT")
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(ATTACHMENT_ID))
            .andExpect(jsonPath("$[0].lessonId").value(LESSON_ID));

        verify(attachmentService).getLessonAttachments(LESSON_ID);
    }

    @Test
    void getLessonAttachments_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(
                "/api/v1/attachments/lessons/{lessonId}",
                LESSON_ID
            ))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void download_shouldReturn200WhenStudent() throws Exception {
        byte[] fileContent = "attachment content".getBytes(StandardCharsets.UTF_8);

        AttachmentDownload download = new AttachmentDownload(
            new ByteArrayInputStream(fileContent),
            "document.pdf",
            "application/pdf",
            fileContent.length
        );

        when(attachmentService.download(ATTACHMENT_ID))
            .thenReturn(download);

        mockMvc.perform(get(
                "/api/v1/attachments/{attachmentId}/download",
                ATTACHMENT_ID
            )
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_STUDENT")
                )))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(content().bytes(fileContent))
            .andExpect(header().string(
                "Content-Disposition",
                org.hamcrest.Matchers.containsString("document.pdf")
            ));

        verify(attachmentService).download(ATTACHMENT_ID);
    }

    @Test
    void download_shouldReturn200WhenInstructor() throws Exception {
        AttachmentDownload download = new AttachmentDownload(
            new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)),
            "document.pdf",
            "application/pdf",
            7L
        );

        when(attachmentService.download(ATTACHMENT_ID))
            .thenReturn(download);

        mockMvc.perform(get(
                "/api/v1/attachments/{attachmentId}/download",
                ATTACHMENT_ID
            )
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_INSTRUCTOR")
                )))
            .andExpect(status().isOk());

        verify(attachmentService).download(ATTACHMENT_ID);
    }

    @Test
    void download_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(
                "/api/v1/attachments/{attachmentId}/download",
                ATTACHMENT_ID
            ))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_shouldReturn204WhenAdmin() throws Exception {
        doNothing().when(attachmentService).delete(ATTACHMENT_ID);

        mockMvc.perform(delete(
                "/api/v1/attachments/{attachmentId}",
                ATTACHMENT_ID
            )
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_ADMIN")
                )))
            .andExpect(status().isNoContent());

        verify(attachmentService).delete(ATTACHMENT_ID);
    }

    @Test
    void delete_shouldReturn204WhenStudent() throws Exception {
        doNothing().when(attachmentService).delete(ATTACHMENT_ID);

        mockMvc.perform(delete(
                "/api/v1/attachments/{attachmentId}",
                ATTACHMENT_ID
            )
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_STUDENT")
                )))
            .andExpect(status().isNoContent());

        verify(attachmentService).delete(ATTACHMENT_ID);
    }

    @Test
    void delete_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(delete(
                "/api/v1/attachments/{attachmentId}",
                ATTACHMENT_ID
            ))
            .andExpect(status().isUnauthorized());
    }
}
