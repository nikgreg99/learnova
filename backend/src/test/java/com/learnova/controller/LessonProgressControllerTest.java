package com.learnova.controller;

import com.learnova.config.ratelimit.RateLimitInterceptor;
import com.learnova.config.security.SecurityConfig;
import com.learnova.dto.progress.CourseProgressResponse;
import com.learnova.dto.progress.LessonProgressRequest;
import com.learnova.dto.progress.LessonProgressResponse;
import com.learnova.dto.progress.StudentProgressResponse;
import com.learnova.model.ProgressStatus;
import com.learnova.service.LessonProgressService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LessonProgressController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class LessonProgressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private LessonProgressService lessonProgressService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    private LessonProgressRequest progressRequest;
    private LessonProgressResponse progressResponse;
    private CourseProgressResponse courseProgressResponse;
    private StudentProgressResponse studentProgressResponse;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any()))
            .thenReturn(true);

        progressRequest = new LessonProgressRequest(
            ProgressStatus.COMPLETED
        );

        progressResponse = new LessonProgressResponse(
            100L,
            "Introduction to Java",
            ProgressStatus.COMPLETED,
            LocalDateTime.of(2026, 11, 13, 10, 0),
            LocalDateTime.of(2026, 11, 13, 10, 0)
        );

        courseProgressResponse = new CourseProgressResponse(
            10L,
            "Java Fundamentals",
            5,
            2,
            1,
            2,
            40.0,
            List.of(progressResponse)
        );

        studentProgressResponse = new StudentProgressResponse(
            List.of(courseProgressResponse)
        );
    }

    @Test
    @WithAnonymousUser
    void updateLessonProgress_shouldReturnUnauthorizedWhenUserIsNotAuthenticated()
        throws Exception {

        mockMvc.perform(post("/api/v1/lessons/{lessonId}/progress", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(progressRequest)))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(lessonProgressService);
    }

    @Test
    void updateLessonProgress_shouldUpdateProgressWhenUserIsStudent()
        throws Exception {

        when(lessonProgressService.updateLessonProgress(
            100L,
            ProgressStatus.COMPLETED
        )).thenReturn(progressResponse);

        mockMvc.perform(post("/api/v1/lessons/{lessonId}/progress", 100L)
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_STUDENT")
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(progressRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lessonId").value(100))
            .andExpect(jsonPath("$.lessonTitle")
                .value("Introduction to Java"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.completedAt")
                .value("2026-11-13T10:00:00"));

        verify(lessonProgressService).updateLessonProgress(
            100L,
            ProgressStatus.COMPLETED
        );
    }

    @Test
    void updateLessonProgress_shouldReturnBadRequestWhenStatusIsMissing()
        throws Exception {

        LessonProgressRequest invalidRequest =
            new LessonProgressRequest(null);

        mockMvc.perform(post("/api/v1/lessons/{lessonId}/progress", 100L)
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_STUDENT")
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(lessonProgressService);
    }

    @Test
    void updateLessonProgress_shouldReturnBadRequestWhenStatusIsInvalid()
        throws Exception {

        String invalidRequest = """
                {
                    "status": "INVALID_STATUS"
                }
                """;

        mockMvc.perform(post("/api/v1/lessons/{lessonId}/progress", 100L)
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_STUDENT")
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(lessonProgressService);
    }

    @Test
    void updateLessonProgress_shouldReturnForbiddenWhenUserIsInstructor()
        throws Exception {

        mockMvc.perform(post("/api/v1/lessons/{lessonId}/progress", 100L)
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_INSTRUCTOR")
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(progressRequest)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(lessonProgressService);
    }

    @Test
    void updateLessonProgress_shouldReturnForbiddenWhenUserIsAdmin()
        throws Exception {

        mockMvc.perform(post("/api/v1/lessons/{lessonId}/progress", 100L)
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_ADMIN")
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(progressRequest)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(lessonProgressService);
    }

    @Test
    @WithAnonymousUser
    void getCourseProgress_shouldReturnUnauthorizedWhenUserIsNotAuthenticated()
        throws Exception {

        mockMvc.perform(get("/api/v1/courses/{courseId}/progress", 10L))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(lessonProgressService);
    }

    @Test
    void getCourseProgress_shouldReturnCourseProgressWhenUserIsStudent()
        throws Exception {

        when(lessonProgressService.getCourseProgress(10L))
            .thenReturn(courseProgressResponse);

        mockMvc.perform(get("/api/v1/courses/{courseId}/progress", 10L)
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_STUDENT")
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.courseId").value(10))
            .andExpect(jsonPath("$.courseName")
                .value("Java Fundamentals"))
            .andExpect(jsonPath("$.totalLessons").value(5))
            .andExpect(jsonPath("$.completedLessons").value(2))
            .andExpect(jsonPath("$.inProgressLessons").value(1))
            .andExpect(jsonPath("$.notStartedLessons").value(2))
            .andExpect(jsonPath("$.completionPercentage").value(40.0))
            .andExpect(jsonPath("$.lessons").isArray())
            .andExpect(jsonPath("$.lessons.length()").value(1))
            .andExpect(jsonPath("$.lessons[0].lessonId").value(100))
            .andExpect(jsonPath("$.lessons[0].status")
                .value("COMPLETED"));

        verify(lessonProgressService).getCourseProgress(10L);
    }

    @Test
    void getCourseProgress_shouldReturnForbiddenWhenUserIsInstructor()
        throws Exception {

        mockMvc.perform(get("/api/v1/courses/{courseId}/progress", 10L)
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_INSTRUCTOR")
                )))
            .andExpect(status().isForbidden());

        verifyNoInteractions(lessonProgressService);
    }

    @Test
    @WithAnonymousUser
    void getStudentProgress_shouldReturnUnauthorizedWhenUserIsNotAuthenticated()
        throws Exception {

        mockMvc.perform(get("/api/v1/users/me/progress"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(lessonProgressService);
    }

    @Test
    void getStudentProgress_shouldReturnStudentProgressWhenUserIsStudent()
        throws Exception {

        when(lessonProgressService.getStudentProgress())
            .thenReturn(studentProgressResponse);

        mockMvc.perform(get("/api/v1/users/me/progress")
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_STUDENT")
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.courses").isArray())
            .andExpect(jsonPath("$.courses.length()").value(1))
            .andExpect(jsonPath("$.courses[0].courseId").value(10))
            .andExpect(jsonPath("$.courses[0].courseName")
                .value("Java Fundamentals"))
            .andExpect(jsonPath("$.courses[0].completionPercentage")
                .value(40.0));

        verify(lessonProgressService).getStudentProgress();
    }

    @Test
    void getStudentProgress_shouldReturnForbiddenWhenUserIsInstructor()
        throws Exception {

        mockMvc.perform(get("/api/v1/users/me/progress")
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_INSTRUCTOR")
                )))
            .andExpect(status().isForbidden());

        verifyNoInteractions(lessonProgressService);
    }
}
