package com.learnova.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learnova.dto.course.CoursePatchRequest;
import com.learnova.dto.course.CourseRequest;
import com.learnova.dto.course.CourseResponse;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.exception.KeycloakUserNotFoundException;
import com.learnova.mapper.CourseMapper;
import com.learnova.model.Course;
import com.learnova.model.CourseStatus;
import com.learnova.model.User;
import com.learnova.repository.CourseRepository;
import com.learnova.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseService courseService;

    private Long courseId;
    private Course course;
    private CourseRequest courseRequest;
    private CourseResponse courseResponse;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        courseId = 1L;

        course = createCourse(courseId, "Java Fundamentals", "Introduction to Java", "49.90");

        courseRequest = new CourseRequest("Advanced Java", "Advanced Java concepts", new BigDecimal("59.90"));

        courseResponse = new CourseResponse(courseId, courseRequest.name(), CourseStatus.DRAFT, 1L,
                courseRequest.description(), courseRequest.price(), null, null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String keycloakId) {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject(keycloakId).issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300)).build();

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_INSTRUCTOR")));
    }

    private Course createCourse(Long id, String name, String description, String price) {
        Course course = new Course();
        course.setId(id);
        course.setName(name);
        course.setDescription(description);
        course.setPrice(new BigDecimal(price));
        return course;
    }

    @Test
    void shouldReturnCourse() {
        Course secondCourse = createCourse(2L, "Spring Boot Essentials", "Learn Spring Boot", "79.90");

        CourseResponse secondResponse = new CourseResponse(2L, "Spring Boot Essentials", null, courseId,
                "Learn Spring Boot", new BigDecimal("79.90"), null, null);

        when(courseRepository.findAll()).thenReturn(List.of(course, secondCourse));
        when(courseMapper.toResponse(course)).thenReturn(courseResponse);
        when(courseMapper.toResponse(secondCourse)).thenReturn(secondResponse);

        var result = courseService.getCourses();

        assertEquals(2, result.size());
        assertEquals(List.of(courseResponse, secondResponse), result);

        verify(courseRepository).findAll();
        verify(courseMapper).toResponse(course);
        verify(courseMapper).toResponse(secondCourse);
    }

    @Test
    void shouldReturnEmptyListWhenNoCourseExists() {
        when(courseRepository.findAll()).thenReturn(List.of());

        var result = courseService.getCourses();

        assertTrue(result.isEmpty());

        verify(courseRepository).findAll();
        verifyNoInteractions(courseMapper);
    }

    @Test
    void shouldReturnCourseWhenCourseExists() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseMapper.toResponse(course)).thenReturn(courseResponse);

        var result = courseService.getCourseById(courseId);

        assertTrue(result.isPresent());
        assertEquals(courseResponse, result.get());

        verify(courseRepository).findById(courseId);
        verify(courseMapper).toResponse(course);
    }

    @Test
    void shouldReturnEmptyWhenCourseDoesNotExist() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        var result = courseService.getCourseById(courseId);

        assertTrue(result.isEmpty());

        verify(courseRepository).findById(courseId);
        verifyNoInteractions(courseMapper);
    }

    @Test
    void shouldAddCourse() {
        String keycloakId = "instructor-keycloak-id";
        authenticate(keycloakId);

        User instructor = new User();
        instructor.setId(1L);
        instructor.setKeycloakId(keycloakId);

        Course newCourse = createCourse(courseId, courseRequest.name(), courseRequest.description(), "59.90");

        when(courseMapper.toEntity(courseRequest)).thenReturn(newCourse);
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(instructor));
        when(courseRepository.save(newCourse)).thenReturn(newCourse);
        when(courseMapper.toResponse(newCourse)).thenReturn(courseResponse);

        var result = courseService.addCourse(courseRequest);

        assertEquals(courseResponse, result);
        assertEquals(instructor, newCourse.getInstructor());

        verify(courseMapper).toEntity(courseRequest);
        verify(userRepository).findByKeycloakId(keycloakId);
        verify(courseRepository).save(newCourse);
        verify(courseMapper).toResponse(newCourse);
    }

    @Test
    void shouldThrowKeycloakUserNotFoundExceptionWhenInstructorNotInDatabase() {
        String keycloakId = "unknown-keycloak-id";
        authenticate(keycloakId);

        Course newCourse = createCourse(courseId, courseRequest.name(), courseRequest.description(), "59.90");
        when(courseMapper.toEntity(courseRequest)).thenReturn(newCourse);
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());

        assertThrows(KeycloakUserNotFoundException.class, () -> courseService.addCourse(courseRequest));

        verify(userRepository).findByKeycloakId(keycloakId);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenUserNotAuthenticated() {
        SecurityContextHolder.clearContext();

        Course newCourse = createCourse(courseId, courseRequest.name(), courseRequest.description(), "59.90");
        when(courseMapper.toEntity(courseRequest)).thenReturn(newCourse);

        assertThrows(IllegalStateException.class, () -> courseService.addCourse(courseRequest));

        verifyNoInteractions(userRepository);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void shouldDeleteCourseWhenCourseExists() {
        when(courseRepository.existsById(courseId)).thenReturn(true);

        assertDoesNotThrow(() -> courseService.deleteCourseById(courseId));

        verify(courseRepository).existsById(courseId);
        verify(courseRepository).deleteById(courseId);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingCourse() {
        when(courseRepository.existsById(courseId)).thenReturn(false);

        assertThrows(CourseNotFoundException.class, () -> courseService.deleteCourseById(courseId));

        verify(courseRepository).existsById(courseId);
        verify(courseRepository, never()).deleteById(courseId);
    }

    @Test
    void shouldUpdateCourseWhenCourseExists() {
        Course updatedCourse = createCourse(courseId, courseRequest.name(), courseRequest.description(), "59.90");

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(updatedCourse);
        when(courseMapper.toResponse(updatedCourse)).thenReturn(courseResponse);

        var result = courseService.updateCourse(courseId, courseRequest);

        assertTrue(result.isPresent());
        assertEquals(courseResponse, result.get());

        verify(courseRepository).findById(courseId);
        verify(courseMapper).updateCourse(courseRequest, course);
        verify(courseRepository).save(course);
        verify(courseMapper).toResponse(updatedCourse);
    }

    @Test
    void shouldReturnEmptyWhenUpdatingNonExistingCourse() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        var result = courseService.updateCourse(courseId, courseRequest);

        assertTrue(result.isEmpty());

        verify(courseRepository).findById(courseId);
        verify(courseRepository, never()).save(any(Course.class));
        verifyNoInteractions(courseMapper);
    }

    @Test
    void shouldPatchCourseWhenCourseExists() {
        CoursePatchRequest patchRequest = new CoursePatchRequest("Updated Title", null, null, CourseStatus.PUBLISHED);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);
        when(courseMapper.toResponse(course)).thenReturn(courseResponse);

        var result = courseService.patchCourse(courseId, patchRequest);

        assertTrue(result.isPresent());
        assertEquals(courseResponse, result.get());
        verify(courseMapper).patchCourse(patchRequest, course);
        verify(courseRepository).save(course);
    }

    @Test
    void shouldUpdateCourseStatusWhenCourseExists() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);
        when(courseMapper.toResponse(course)).thenReturn(courseResponse);

        var result = courseService.updateCourseStatus(courseId, CourseStatus.PUBLISHED);

        assertTrue(result.isPresent());
        assertEquals(courseResponse, result.get());
        assertEquals(CourseStatus.PUBLISHED, course.getStatus());
        verify(courseRepository).save(course);
    }
}
