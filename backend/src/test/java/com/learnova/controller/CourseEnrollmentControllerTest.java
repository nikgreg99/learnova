package com.learnova.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnova.config.ratelimit.RateLimitInterceptor;
import com.learnova.config.security.SecurityConfig;
import com.learnova.dto.enrollment.EnrollmentResponse;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.exception.DuplicateEnrollmentException;
import com.learnova.model.EnrollmentStatus;
import com.learnova.service.EnrollmentService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CourseEnrollmentController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class CourseEnrollmentControllerTest {

    private static final Long COURSE_ID = 1L;
    private static final Long ENROLLMENT_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    private EnrollmentResponse sampleResponse;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        sampleResponse = new EnrollmentResponse(
                ENROLLMENT_ID,
                2L,
                "alicesmith",
                COURSE_ID,
                "Docker Mastery",
                EnrollmentStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void enroll_shouldReturn201WhenStudent() throws Exception {
        when(enrollmentService.enrollStudent(COURSE_ID)).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/courses/{courseId}/enrollments", COURSE_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/enrollments/10"))
                .andExpect(jsonPath("$.id").value(ENROLLMENT_ID))
                .andExpect(jsonPath("$.courseName").value("Docker Mastery"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(enrollmentService).enrollStudent(COURSE_ID);
    }

    @Test
    void enroll_shouldReturn201WhenAdmin() throws Exception {
        when(enrollmentService.enrollStudent(COURSE_ID)).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/courses/{courseId}/enrollments", COURSE_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/enrollments/10"));

        verify(enrollmentService).enrollStudent(COURSE_ID);
    }

    @Test
    void enroll_shouldReturn403WhenInstructor() throws Exception {
        mockMvc.perform(post("/api/v1/courses/{courseId}/enrollments", COURSE_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void enroll_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/courses/{courseId}/enrollments", COURSE_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enroll_shouldReturn409WhenDuplicateEnrollment() throws Exception {
        when(enrollmentService.enrollStudent(COURSE_ID))
                .thenThrow(new DuplicateEnrollmentException("Student is already enrolled in this course"));

        mockMvc.perform(post("/api/v1/courses/{courseId}/enrollments", COURSE_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Student is already enrolled in this course"));
    }

    @Test
    void enroll_shouldReturn404WhenCourseNotFound() throws Exception {
        when(enrollmentService.enrollStudent(COURSE_ID))
                .thenThrow(new CourseNotFoundException(COURSE_ID));

        mockMvc.perform(post("/api/v1/courses/{courseId}/enrollments", COURSE_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCourseEnrollments_shouldReturn200WhenInstructor() throws Exception {
        PageImpl<EnrollmentResponse> page = new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 20), 1);
        when(enrollmentService.getCourseEnrollments(eq(COURSE_ID), any(), eq(EnrollmentStatus.ACTIVE))).thenReturn(page);

        mockMvc.perform(get("/api/v1/courses/{courseId}/enrollments", COURSE_ID)
                .param("status", "ACTIVE")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ENROLLMENT_ID))
                .andExpect(jsonPath("$.content[0].courseName").value("Docker Mastery"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(enrollmentService).getCourseEnrollments(eq(COURSE_ID), any(), eq(EnrollmentStatus.ACTIVE));
    }

    @Test
    void getCourseEnrollments_shouldReturn403WhenStudent() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{courseId}/enrollments", COURSE_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCourseEnrollments_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{courseId}/enrollments", COURSE_ID))
                .andExpect(status().isUnauthorized());
    }
}
