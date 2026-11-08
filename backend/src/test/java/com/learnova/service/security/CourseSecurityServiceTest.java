
package com.learnova.service.security;

import com.learnova.model.Course;
import com.learnova.model.User;
import com.learnova.repository.CourseRepository;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseSecurityServiceTest {

    private static final Long COURSE_ID = 1L;
    private static final String KEYCLOAK_ID = "instructor-keycloak-id";
    private static final String OTHER_KEYCLOAK_ID = "other-keycloak-id";

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseSecurityService courseSecurityService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String role, String keycloakId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .subject(keycloakId)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        SecurityContextHolder.getContext().setAuthentication(
            new JwtAuthenticationToken(
                jwt,
                AuthorityUtils.createAuthorityList("ROLE_" + role)
            )
        );
    }

    private Course mockCourseWithInstructor() {
        Course course = mock(Course.class);
        User instructor = mock(User.class);

        when(course.getInstructor()).thenReturn(instructor);
        when(instructor.getKeycloakId()).thenReturn(CourseSecurityServiceTest.KEYCLOAK_ID);

        return course;
    }

    @Test
    void isOwnerShouldReturnTrueWhenUserIsAdmin() {
        authenticate("ADMIN", OTHER_KEYCLOAK_ID);

        assertTrue(courseSecurityService.isOwner(COURSE_ID));

        verify(courseRepository, never()).findById(COURSE_ID);
    }

    @Test
    void isOwnerShouldReturnTrueWhenUserOwnsCourse() {
        authenticate("INSTRUCTOR", KEYCLOAK_ID);

        Course course = mockCourseWithInstructor();

        when(courseRepository.findById(COURSE_ID))
            .thenReturn(Optional.of(course));

        assertTrue(courseSecurityService.isOwner(COURSE_ID));

        verify(courseRepository).findById(COURSE_ID);
    }

    @Test
    void isOwnerShouldReturnFalseWhenUserDoesNotOwnCourse() {
        authenticate("INSTRUCTOR", OTHER_KEYCLOAK_ID);

        Course course = mockCourseWithInstructor();

        when(courseRepository.findById(COURSE_ID))
            .thenReturn(Optional.of(course));

        assertFalse(courseSecurityService.isOwner(COURSE_ID));
    }

    @Test
    void isOwnerShouldReturnFalseWhenCourseDoesNotExist() {
        authenticate("INSTRUCTOR", KEYCLOAK_ID);

        when(courseRepository.findById(COURSE_ID))
            .thenReturn(Optional.empty());

        assertFalse(courseSecurityService.isOwner(COURSE_ID));
    }

    @Test
    void isOwnerShouldReturnFalseWhenCourseHasNoInstructor() {
        authenticate("INSTRUCTOR", KEYCLOAK_ID);

        Course course = mock(Course.class);

        when(courseRepository.findById(COURSE_ID))
            .thenReturn(Optional.of(course));

        when(course.getInstructor()).thenReturn(null);

        assertFalse(courseSecurityService.isOwner(COURSE_ID));
    }

    @Test
    void isOwnerShouldReturnFalseWhenAuthenticationIsNull() {
        SecurityContextHolder.clearContext();

        assertFalse(courseSecurityService.isOwner(COURSE_ID));

        verify(courseRepository, never()).findById(COURSE_ID);
    }

    @Test
    void isOwnerShouldReturnFalseWhenUserIsNotAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "anonymous",
                null,
                AuthorityUtils.NO_AUTHORITIES
            )
        );

        SecurityContextHolder.getContext()
            .getAuthentication()
            .setAuthenticated(false);

        assertFalse(courseSecurityService.isOwner(COURSE_ID));

        verify(courseRepository, never()).findById(COURSE_ID);
    }

    @Test
    void verifyOwnershipShouldNotThrowWhenUserIsOwner() {
        authenticate("INSTRUCTOR", KEYCLOAK_ID);

        Course course = mockCourseWithInstructor();

        when(courseRepository.findById(COURSE_ID))
            .thenReturn(Optional.of(course));

        assertDoesNotThrow(
            () -> courseSecurityService.verifyOwnership(COURSE_ID)
        );
    }

    @Test
    void verifyOwnershipShouldNotThrowWhenUserIsAdmin() {
        authenticate("ADMIN", OTHER_KEYCLOAK_ID);

        assertDoesNotThrow(
            () -> courseSecurityService.verifyOwnership(COURSE_ID)
        );

        verify(courseRepository, never()).findById(COURSE_ID);
    }

    @Test
    void verifyOwnershipShouldThrowWhenUserIsNotOwner() {
        authenticate("INSTRUCTOR", OTHER_KEYCLOAK_ID);

        Course course = mockCourseWithInstructor();

        when(courseRepository.findById(COURSE_ID))
            .thenReturn(Optional.of(course));

        assertThrows(
            AccessDeniedException.class,
            () -> courseSecurityService.verifyOwnership(COURSE_ID)
        );
    }

    @Test
    void verifyOwnershipShouldThrowWhenCourseDoesNotExist() {
        authenticate("INSTRUCTOR", KEYCLOAK_ID);

        when(courseRepository.findById(COURSE_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            AccessDeniedException.class,
            () -> courseSecurityService.verifyOwnership(COURSE_ID)
        );
    }

    @Test
    void verifyOwnershipShouldThrowWhenUserIsNotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertThrows(
            AccessDeniedException.class,
            () -> courseSecurityService.verifyOwnership(COURSE_ID)
        );
    }
}
