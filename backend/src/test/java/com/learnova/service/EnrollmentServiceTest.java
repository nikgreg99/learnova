package com.learnova.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnova.dto.enrollment.EnrollmentResponse;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.exception.DuplicateEnrollmentException;
import com.learnova.exception.EnrollmentNotFoundException;
import com.learnova.exception.InvalidEnrollmentStateException;
import com.learnova.mapper.EnrollmentMapper;
import com.learnova.model.Course;
import com.learnova.model.Enrollment;
import com.learnova.model.EnrollmentStatus;
import com.learnova.model.User;
import com.learnova.repository.CourseRepository;
import com.learnova.repository.EnrollmentRepository;
import com.learnova.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    private static final String STUDENT_KEYCLOAK_ID = "student-kc-id";
    private static final String INSTRUCTOR_KEYCLOAK_ID = "instructor-kc-id";
    private static final String ADMIN_KEYCLOAK_ID = "admin-kc-id";
    private static final String STRANGER_KEYCLOAK_ID = "stranger-kc-id";

    private static final Long STUDENT_ID = 1L;
    private static final Long INSTRUCTOR_ID = 2L;
    private static final Long STRANGER_ID = 3L;
    private static final Long COURSE_ID = 10L;
    private static final Long ENROLLMENT_ID = 100L;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentMapper enrollmentMapper;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private User student;
    private User instructor;
    private User stranger;
    private Course course;
    private Enrollment enrollment;
    private EnrollmentResponse enrollmentResponse;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        student = User.builder()
                .id(STUDENT_ID)
                .keycloakId(STUDENT_KEYCLOAK_ID)
                .username("student1")
                .build();

        instructor = User.builder()
                .id(INSTRUCTOR_ID)
                .keycloakId(INSTRUCTOR_KEYCLOAK_ID)
                .username("prof_smith")
                .build();

        stranger = User.builder()
                .id(STRANGER_ID)
                .keycloakId(STRANGER_KEYCLOAK_ID)
                .username("stranger")
                .build();

        course = new Course();
        course.setId(COURSE_ID);
        course.setName("Spring Boot Microservices");
        course.setInstructor(instructor);

        LocalDateTime now = LocalDateTime.now();
        enrollment = Enrollment.builder()
                .id(ENROLLMENT_ID)
                .student(student)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(now)
                .updatedAt(now)
                .build();

        enrollmentResponse = new EnrollmentResponse(
                ENROLLMENT_ID,
                STUDENT_ID,
                "student1",
                COURSE_ID,
                "Spring Boot Microservices",
                EnrollmentStatus.ACTIVE,
                now,
                now
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String keycloakId, String role) {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject(keycloakId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList(role))
        );
    }

    // ==================== Enroll Student ====================

    @Test
    void enrollStudent_shouldCreateEnrollmentWhenValid() {
        authenticate(STUDENT_KEYCLOAK_ID, "ROLE_STUDENT");

        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(userRepository.findByKeycloakId(STUDENT_KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);
        when(enrollmentMapper.toResponse(enrollment)).thenReturn(enrollmentResponse);

        EnrollmentResponse result = enrollmentService.enrollStudent(COURSE_ID);

        assertNotNull(result);
        assertEquals(ENROLLMENT_ID, result.id());
        assertEquals(EnrollmentStatus.ACTIVE, result.status());
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void enrollStudent_shouldThrowWhenCourseNotFound() {
        authenticate(STUDENT_KEYCLOAK_ID, "ROLE_STUDENT");
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class, () -> enrollmentService.enrollStudent(COURSE_ID));
    }

    @Test
    void enrollStudent_shouldThrowWhenAlreadyEnrolled() {
        authenticate(STUDENT_KEYCLOAK_ID, "ROLE_STUDENT");

        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(userRepository.findByKeycloakId(STUDENT_KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(true);

        assertThrows(DuplicateEnrollmentException.class, () -> enrollmentService.enrollStudent(COURSE_ID));
    }

    // ==================== Cancel Enrollment ====================

    @Test
    void cancelEnrollment_shouldCancelWhenActive() {
        authenticate(STUDENT_KEYCLOAK_ID, "ROLE_STUDENT");

        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
        when(userRepository.findByKeycloakId(STUDENT_KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        EnrollmentResponse cancelledResponse = new EnrollmentResponse(
                ENROLLMENT_ID, STUDENT_ID, "student1", COURSE_ID, "Spring Boot Microservices",
                EnrollmentStatus.CANCELLED, enrollment.getEnrolledAt(), LocalDateTime.now());
        when(enrollmentMapper.toResponse(enrollment)).thenReturn(cancelledResponse);

        EnrollmentResponse result = enrollmentService.cancelEnrollment(ENROLLMENT_ID);

        assertNotNull(result);
        assertEquals(EnrollmentStatus.CANCELLED, result.status());
        assertEquals(EnrollmentStatus.CANCELLED, enrollment.getStatus());
    }

    @Test
    void cancelEnrollment_shouldThrowWhenNotFound() {
        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.empty());

        assertThrows(EnrollmentNotFoundException.class, () -> enrollmentService.cancelEnrollment(ENROLLMENT_ID));
    }

    @Test
    void cancelEnrollment_shouldThrowWhenUserNotOwnerAndNotAdmin() {
        authenticate(STRANGER_KEYCLOAK_ID, "ROLE_STUDENT");

        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
        when(userRepository.findByKeycloakId(STRANGER_KEYCLOAK_ID)).thenReturn(Optional.of(stranger));

        assertThrows(AccessDeniedException.class, () -> enrollmentService.cancelEnrollment(ENROLLMENT_ID));
    }

    @Test
    void cancelEnrollment_shouldThrowWhenAlreadyCompleted() {
        authenticate(STUDENT_KEYCLOAK_ID, "ROLE_STUDENT");
        enrollment.setStatus(EnrollmentStatus.COMPLETED);

        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
        when(userRepository.findByKeycloakId(STUDENT_KEYCLOAK_ID)).thenReturn(Optional.of(student));

        assertThrows(InvalidEnrollmentStateException.class, () -> enrollmentService.cancelEnrollment(ENROLLMENT_ID));
    }

    @Test
    void cancelEnrollment_shouldThrowWhenAlreadyCancelled() {
        authenticate(STUDENT_KEYCLOAK_ID, "ROLE_STUDENT");
        enrollment.setStatus(EnrollmentStatus.CANCELLED);

        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
        when(userRepository.findByKeycloakId(STUDENT_KEYCLOAK_ID)).thenReturn(Optional.of(student));

        assertThrows(InvalidEnrollmentStateException.class, () -> enrollmentService.cancelEnrollment(ENROLLMENT_ID));
    }

    @Test
    void cancelEnrollment_shouldAllowAdminToCancelAnyEnrollment() {
        authenticate(ADMIN_KEYCLOAK_ID, "ROLE_ADMIN");

        User admin = User.builder().id(99L).keycloakId(ADMIN_KEYCLOAK_ID).username("admin").build();
        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
        when(userRepository.findByKeycloakId(ADMIN_KEYCLOAK_ID)).thenReturn(Optional.of(admin));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        enrollmentService.cancelEnrollment(ENROLLMENT_ID);

        assertEquals(EnrollmentStatus.CANCELLED, enrollment.getStatus());
    }

    // ==================== Get My Enrollments ====================

    @Test
    void getMyEnrollments_shouldReturnPagedEnrollmentsWithoutStatusFilter() {
        authenticate(STUDENT_KEYCLOAK_ID, "ROLE_STUDENT");
        when(userRepository.findByKeycloakId(STUDENT_KEYCLOAK_ID)).thenReturn(Optional.of(student));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Enrollment> page = new PageImpl<>(List.of(enrollment), pageable, 1);
        when(enrollmentRepository.findByStudentId(STUDENT_ID, pageable)).thenReturn(page);
        when(enrollmentMapper.toResponse(enrollment)).thenReturn(enrollmentResponse);

        Page<EnrollmentResponse> result = enrollmentService.getMyEnrollments(pageable, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(enrollmentResponse, result.getContent().get(0));
    }

    @Test
    void getMyEnrollments_shouldReturnPagedEnrollmentsWithStatusFilter() {
        authenticate(STUDENT_KEYCLOAK_ID, "ROLE_STUDENT");
        when(userRepository.findByKeycloakId(STUDENT_KEYCLOAK_ID)).thenReturn(Optional.of(student));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Enrollment> page = new PageImpl<>(List.of(enrollment), pageable, 1);
        when(enrollmentRepository.findByStudentIdAndStatus(STUDENT_ID, EnrollmentStatus.ACTIVE, pageable))
                .thenReturn(page);
        when(enrollmentMapper.toResponse(enrollment)).thenReturn(enrollmentResponse);

        Page<EnrollmentResponse> result = enrollmentService.getMyEnrollments(pageable, EnrollmentStatus.ACTIVE);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // ==================== Get Course Enrollments ====================

    @Test
    void getCourseEnrollments_shouldReturnPagedEnrollmentsForOwnerInstructor() {
        authenticate(INSTRUCTOR_KEYCLOAK_ID, "ROLE_INSTRUCTOR");

        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(userRepository.findByKeycloakId(INSTRUCTOR_KEYCLOAK_ID)).thenReturn(Optional.of(instructor));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Enrollment> page = new PageImpl<>(List.of(enrollment), pageable, 1);
        when(enrollmentRepository.findByCourseId(COURSE_ID, pageable)).thenReturn(page);
        when(enrollmentMapper.toResponse(enrollment)).thenReturn(enrollmentResponse);

        Page<EnrollmentResponse> result = enrollmentService.getCourseEnrollments(COURSE_ID, pageable, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getCourseEnrollments_shouldReturnPagedEnrollmentsForAdmin() {
        authenticate(ADMIN_KEYCLOAK_ID, "ROLE_ADMIN");

        User admin = User.builder().id(99L).keycloakId(ADMIN_KEYCLOAK_ID).username("admin").build();
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(userRepository.findByKeycloakId(ADMIN_KEYCLOAK_ID)).thenReturn(Optional.of(admin));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Enrollment> page = new PageImpl<>(List.of(enrollment), pageable, 1);
        when(enrollmentRepository.findByCourseId(COURSE_ID, pageable)).thenReturn(page);
        when(enrollmentMapper.toResponse(enrollment)).thenReturn(enrollmentResponse);

        Page<EnrollmentResponse> result = enrollmentService.getCourseEnrollments(COURSE_ID, pageable, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getCourseEnrollments_shouldThrowWhenNotInstructorOwnerAndNotAdmin() {
        authenticate(STRANGER_KEYCLOAK_ID, "ROLE_INSTRUCTOR");

        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(userRepository.findByKeycloakId(STRANGER_KEYCLOAK_ID)).thenReturn(Optional.of(stranger));

        Pageable pageable = PageRequest.of(0, 10);
        assertThrows(AccessDeniedException.class,
                () -> enrollmentService.getCourseEnrollments(COURSE_ID, pageable, null));
    }

    @Test
    void getCourseEnrollments_shouldThrowWhenCourseNotFound() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 10);
        assertThrows(CourseNotFoundException.class,
                () -> enrollmentService.getCourseEnrollments(COURSE_ID, pageable, null));
    }

    // ==================== Get Enrollment By ID ====================

    @Test
    void getEnrollmentById_shouldReturnEnrollmentWhenRequestedByStudent() {
        authenticate(STUDENT_KEYCLOAK_ID, "ROLE_STUDENT");

        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
        when(userRepository.findByKeycloakId(STUDENT_KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(enrollmentMapper.toResponse(enrollment)).thenReturn(enrollmentResponse);

        EnrollmentResponse result = enrollmentService.getEnrollmentById(ENROLLMENT_ID);

        assertNotNull(result);
        assertEquals(ENROLLMENT_ID, result.id());
    }

    @Test
    void getEnrollmentById_shouldReturnEnrollmentWhenRequestedByInstructor() {
        authenticate(INSTRUCTOR_KEYCLOAK_ID, "ROLE_INSTRUCTOR");

        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
        when(userRepository.findByKeycloakId(INSTRUCTOR_KEYCLOAK_ID)).thenReturn(Optional.of(instructor));
        when(enrollmentMapper.toResponse(enrollment)).thenReturn(enrollmentResponse);

        EnrollmentResponse result = enrollmentService.getEnrollmentById(ENROLLMENT_ID);

        assertNotNull(result);
        assertEquals(ENROLLMENT_ID, result.id());
    }

    @Test
    void getEnrollmentById_shouldReturnEnrollmentWhenRequestedByAdmin() {
        authenticate(ADMIN_KEYCLOAK_ID, "ROLE_ADMIN");

        User admin = User.builder().id(99L).keycloakId(ADMIN_KEYCLOAK_ID).username("admin").build();
        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
        when(userRepository.findByKeycloakId(ADMIN_KEYCLOAK_ID)).thenReturn(Optional.of(admin));
        when(enrollmentMapper.toResponse(enrollment)).thenReturn(enrollmentResponse);

        EnrollmentResponse result = enrollmentService.getEnrollmentById(ENROLLMENT_ID);

        assertNotNull(result);
        assertEquals(ENROLLMENT_ID, result.id());
    }

    @Test
    void getEnrollmentById_shouldThrowWhenUnauthorizedUser() {
        authenticate(STRANGER_KEYCLOAK_ID, "ROLE_STUDENT");

        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
        when(userRepository.findByKeycloakId(STRANGER_KEYCLOAK_ID)).thenReturn(Optional.of(stranger));

        assertThrows(AccessDeniedException.class, () -> enrollmentService.getEnrollmentById(ENROLLMENT_ID));
    }

    @Test
    void getEnrollmentById_shouldThrowWhenNotFound() {
        when(enrollmentRepository.findById(ENROLLMENT_ID)).thenReturn(Optional.empty());

        assertThrows(EnrollmentNotFoundException.class, () -> enrollmentService.getEnrollmentById(ENROLLMENT_ID));
    }
}
