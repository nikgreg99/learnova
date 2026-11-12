package com.learnova.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnova.config.ratelimit.RateLimitInterceptor;
import com.learnova.config.security.SecurityConfig;
import com.learnova.dto.enrollment.EnrollmentResponse;
import com.learnova.exception.EnrollmentNotFoundException;
import com.learnova.exception.InvalidEnrollmentStateException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EnrollmentController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class EnrollmentControllerTest {

    private static final Long ENROLLMENT_ID = 10L;
    private static final Long COURSE_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    private EnrollmentResponse activeResponse;
    private EnrollmentResponse cancelledResponse;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        activeResponse = new EnrollmentResponse(
                ENROLLMENT_ID,
                2L,
                "alicesmith",
                COURSE_ID,
                "Docker Mastery",
                EnrollmentStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        cancelledResponse = new EnrollmentResponse(
                ENROLLMENT_ID,
                2L,
                "alicesmith",
                COURSE_ID,
                "Docker Mastery",
                EnrollmentStatus.CANCELLED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // --- GET /api/v1/enrollments/me ---

    @Test
    void getMyEnrollments_shouldReturn200WhenStudent() throws Exception {
        PageImpl<EnrollmentResponse> page = new PageImpl<>(List.of(activeResponse), PageRequest.of(0, 20), 1);
        when(enrollmentService.getMyEnrollments(any(), eq(EnrollmentStatus.ACTIVE))).thenReturn(page);

        mockMvc.perform(get("/api/v1/enrollments/me")
                .param("status", "ACTIVE")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ENROLLMENT_ID))
                .andExpect(jsonPath("$.content[0].courseName").value("Docker Mastery"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(enrollmentService).getMyEnrollments(any(), eq(EnrollmentStatus.ACTIVE));
    }

    @Test
    void getMyEnrollments_shouldReturn403WhenInstructor() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/me")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyEnrollments_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/me"))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/v1/enrollments/{id} ---

    @Test
    void getEnrollmentById_shouldReturn200WhenStudent() throws Exception {
        when(enrollmentService.getEnrollmentById(ENROLLMENT_ID)).thenReturn(activeResponse);

        mockMvc.perform(get("/api/v1/enrollments/{id}", ENROLLMENT_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ENROLLMENT_ID))
                .andExpect(jsonPath("$.courseName").value("Docker Mastery"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(enrollmentService).getEnrollmentById(ENROLLMENT_ID);
    }

    @Test
    void getEnrollmentById_shouldReturn200WhenInstructor() throws Exception {
        when(enrollmentService.getEnrollmentById(ENROLLMENT_ID)).thenReturn(activeResponse);

        mockMvc.perform(get("/api/v1/enrollments/{id}", ENROLLMENT_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ENROLLMENT_ID));

        verify(enrollmentService).getEnrollmentById(ENROLLMENT_ID);
    }

    @Test
    void getEnrollmentById_shouldReturn403WhenForbidden() throws Exception {
        when(enrollmentService.getEnrollmentById(ENROLLMENT_ID))
                .thenThrow(new AccessDeniedException("Not authorized to view this enrollment"));

        mockMvc.perform(get("/api/v1/enrollments/{id}", ENROLLMENT_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEnrollmentById_shouldReturn404WhenNotFound() throws Exception {
        when(enrollmentService.getEnrollmentById(ENROLLMENT_ID))
                .thenThrow(new EnrollmentNotFoundException(ENROLLMENT_ID));

        mockMvc.perform(get("/api/v1/enrollments/{id}", ENROLLMENT_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/v1/enrollments/{id} ---

    @Test
    void cancelEnrollment_shouldReturn200WhenStudent() throws Exception {
        when(enrollmentService.cancelEnrollment(ENROLLMENT_ID)).thenReturn(cancelledResponse);

        mockMvc.perform(delete("/api/v1/enrollments/{id}", ENROLLMENT_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ENROLLMENT_ID))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(enrollmentService).cancelEnrollment(ENROLLMENT_ID);
    }

    @Test
    void cancelEnrollment_shouldReturn403WhenInstructor() throws Exception {
        mockMvc.perform(delete("/api/v1/enrollments/{id}", ENROLLMENT_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelEnrollment_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(delete("/api/v1/enrollments/{id}", ENROLLMENT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelEnrollment_shouldReturn400WhenCompleted() throws Exception {
        when(enrollmentService.cancelEnrollment(ENROLLMENT_ID))
                .thenThrow(new InvalidEnrollmentStateException("Completed enrollments cannot be cancelled"));

        mockMvc.perform(delete("/api/v1/enrollments/{id}", ENROLLMENT_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Completed enrollments cannot be cancelled"));
    }

    @Test
    void cancelEnrollment_shouldReturn404WhenNotFound() throws Exception {
        when(enrollmentService.cancelEnrollment(ENROLLMENT_ID))
                .thenThrow(new EnrollmentNotFoundException(ENROLLMENT_ID));

        mockMvc.perform(delete("/api/v1/enrollments/{id}", ENROLLMENT_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelEnrollment_shouldReturn403WhenAccessDenied() throws Exception {
        when(enrollmentService.cancelEnrollment(ENROLLMENT_ID))
                .thenThrow(new AccessDeniedException("You are not authorized to cancel this enrollment"));

        mockMvc.perform(delete("/api/v1/enrollments/{id}", ENROLLMENT_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
    }
}
