package com.learnova.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnova.dto.rating.RatingRequest;
import com.learnova.dto.rating.RatingResponse;
import com.learnova.dto.rating.RatingSummaryResponse;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.exception.DuplicateRatingException;
import com.learnova.exception.KeycloakUserNotFoundException;
import com.learnova.exception.LessonNotFoundException;
import com.learnova.exception.RatingNotFoundException;
import com.learnova.mapper.RatingMapper;
import com.learnova.model.Course;
import com.learnova.model.Lesson;
import com.learnova.model.Rating;
import com.learnova.model.User;
import com.learnova.repository.CourseRepository;
import com.learnova.repository.LessonRepository;
import com.learnova.repository.RatingRepository;
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
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    private static final String KEYCLOAK_ID = "test-keycloak-id";
    private static final Long USER_ID = 1L;
    private static final Long COURSE_ID = 10L;
    private static final Long LESSON_ID = 20L;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RatingMapper ratingMapper;

    @InjectMocks
    private RatingService ratingService;

    private User user;
    private Course course;
    private Lesson lesson;
    private Rating rating;
    private RatingRequest request;
    private RatingResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        user = new User();
        user.setId(USER_ID);
        user.setKeycloakId(KEYCLOAK_ID);
        user.setUsername("test-user");

        course = new Course();
        course.setId(COURSE_ID);
        course.setName("Java Fundamentals");

        lesson = new Lesson();
        lesson.setId(LESSON_ID);
        lesson.setTitle("Intro to Java");

        request = new RatingRequest(5, "Excellent content!");

        rating = Rating.builder().id(100L).user(user).course(course).rating(5).review("Excellent content!")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        response = new RatingResponse(100L, USER_ID, "test-user", COURSE_ID, null, 5, "Excellent content!",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate() {
        Jwt jwt = Jwt.withTokenValue("mock-token").header("alg", "none").subject(KEYCLOAK_ID).issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300)).build();

        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_STUDENT")));
    }

    // ==================== Course Ratings ====================

    @Test
    void createCourseRating_shouldCreateAndReturnResponse() {
        authenticate();

        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(ratingRepository.existsByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(false);
        when(ratingMapper.toEntity(request)).thenReturn(rating);
        when(ratingRepository.save(rating)).thenReturn(rating);
        when(ratingMapper.toResponse(rating)).thenReturn(response);

        RatingResponse result = ratingService.createCourseRating(COURSE_ID, request);

        assertNotNull(result);
        assertEquals(response, result);
        assertEquals(user, rating.getUser());
        assertEquals(course, rating.getCourse());

        verify(ratingRepository).save(rating);
    }

    @Test
    void createCourseRating_shouldThrowCourseNotFoundExceptionWhenCourseDoesNotExist() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class, () -> ratingService.createCourseRating(COURSE_ID, request));

        verify(ratingRepository, never()).save(any());
    }

    @Test
    void createCourseRating_shouldThrowDuplicateRatingExceptionWhenUserAlreadyRated() {
        authenticate();

        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(ratingRepository.existsByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(true);

        assertThrows(DuplicateRatingException.class, () -> ratingService.createCourseRating(COURSE_ID, request));

        verify(ratingRepository, never()).save(any());
    }

    @Test
    void createCourseRating_shouldThrowKeycloakUserNotFoundExceptionWhenUserNotFound() {
        authenticate();

        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());

        assertThrows(KeycloakUserNotFoundException.class, () -> ratingService.createCourseRating(COURSE_ID, request));

        verify(ratingRepository, never()).save(any());
    }

    @Test
    void getCourseRatings_shouldReturnList() {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
        when(ratingRepository.findByCourseId(COURSE_ID)).thenReturn(List.of(rating));
        when(ratingMapper.toResponse(rating)).thenReturn(response);

        List<RatingResponse> result = ratingService.getCourseRatings(COURSE_ID);

        assertEquals(1, result.size());
        assertEquals(response, result.get(0));
    }

    @Test
    void getCourseRatings_shouldThrowCourseNotFoundException() {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(false);

        assertThrows(CourseNotFoundException.class, () -> ratingService.getCourseRatings(COURSE_ID));
    }

    @Test
    void getCourseRatingSummary_shouldReturnCalculatedAverageAndCount() {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
        when(ratingRepository.calculateAverageRatingByCourseId(COURSE_ID)).thenReturn(Optional.of(4.7666));
        when(ratingRepository.countByCourseId(COURSE_ID)).thenReturn(12L);

        RatingSummaryResponse summary = ratingService.getCourseRatingSummary(COURSE_ID);

        assertEquals(4.8, summary.averageRating());
        assertEquals(12L, summary.totalRatings());
    }

    @Test
    void getCourseRatingSummary_shouldReturnNullAverageWhenNoRatings() {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
        when(ratingRepository.calculateAverageRatingByCourseId(COURSE_ID)).thenReturn(Optional.empty());
        when(ratingRepository.countByCourseId(COURSE_ID)).thenReturn(0L);

        RatingSummaryResponse summary = ratingService.getCourseRatingSummary(COURSE_ID);

        assertNull(summary.averageRating());
        assertEquals(0L, summary.totalRatings());
    }

    @Test
    void getMyCourseRating_shouldReturnRating() {
        authenticate();

        when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(ratingRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(rating));
        when(ratingMapper.toResponse(rating)).thenReturn(response);

        RatingResponse result = ratingService.getMyCourseRating(COURSE_ID);

        assertEquals(response, result);
    }

    @Test
    void getMyCourseRating_shouldThrowRatingNotFoundException() {
        authenticate();

        when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(ratingRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.empty());

        assertThrows(RatingNotFoundException.class, () -> ratingService.getMyCourseRating(COURSE_ID));
    }

    @Test
    void updateMyCourseRating_shouldUpdateAndReturn() {
        authenticate();

        when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(ratingRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(rating));
        when(ratingRepository.save(rating)).thenReturn(rating);
        when(ratingMapper.toResponse(rating)).thenReturn(response);

        RatingResponse result = ratingService.updateMyCourseRating(COURSE_ID, request);

        assertEquals(response, result);
        verify(ratingMapper).updateRating(request, rating);
        verify(ratingRepository).save(rating);
    }

    @Test
    void deleteMyCourseRating_shouldDelete() {
        authenticate();

        when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(ratingRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(rating));

        ratingService.deleteMyCourseRating(COURSE_ID);

        verify(ratingRepository).delete(rating);
    }

    // ==================== Lesson Ratings ====================

    @Test
    void createLessonRating_shouldCreateAndReturnResponse() {
        authenticate();

        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(ratingRepository.existsByUserIdAndLessonId(USER_ID, LESSON_ID)).thenReturn(false);
        when(ratingMapper.toEntity(request)).thenReturn(rating);
        when(ratingRepository.save(rating)).thenReturn(rating);
        when(ratingMapper.toResponse(rating)).thenReturn(response);

        RatingResponse result = ratingService.createLessonRating(LESSON_ID, request);

        assertNotNull(result);
        assertEquals(user, rating.getUser());
        assertEquals(lesson, rating.getLesson());
        verify(ratingRepository).save(rating);
    }

    @Test
    void createLessonRating_shouldThrowLessonNotFoundException() {
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());

        assertThrows(LessonNotFoundException.class, () -> ratingService.createLessonRating(LESSON_ID, request));
    }

    @Test
    void createLessonRating_shouldThrowDuplicateRatingException() {
        authenticate();

        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(ratingRepository.existsByUserIdAndLessonId(USER_ID, LESSON_ID)).thenReturn(true);

        assertThrows(DuplicateRatingException.class, () -> ratingService.createLessonRating(LESSON_ID, request));
    }

    @Test
    void getLessonRatings_shouldReturnList() {
        when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);
        when(ratingRepository.findByLessonId(LESSON_ID)).thenReturn(List.of(rating));
        when(ratingMapper.toResponse(rating)).thenReturn(response);

        List<RatingResponse> result = ratingService.getLessonRatings(LESSON_ID);

        assertEquals(1, result.size());
    }

    @Test
    void getLessonRatingSummary_shouldReturnCalculatedAverageAndCount() {
        when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);
        when(ratingRepository.calculateAverageRatingByLessonId(LESSON_ID)).thenReturn(Optional.of(4.2));
        when(ratingRepository.countByLessonId(LESSON_ID)).thenReturn(5L);

        RatingSummaryResponse summary = ratingService.getLessonRatingSummary(LESSON_ID);

        assertEquals(4.2, summary.averageRating());
        assertEquals(5L, summary.totalRatings());
    }

    @Test
    void getMyLessonRating_shouldReturnRating() {
        authenticate();

        when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(ratingRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID)).thenReturn(Optional.of(rating));
        when(ratingMapper.toResponse(rating)).thenReturn(response);

        RatingResponse result = ratingService.getMyLessonRating(LESSON_ID);

        assertEquals(response, result);
    }

    @Test
    void updateMyLessonRating_shouldUpdateAndReturn() {
        authenticate();

        when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(ratingRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID)).thenReturn(Optional.of(rating));
        when(ratingRepository.save(rating)).thenReturn(rating);
        when(ratingMapper.toResponse(rating)).thenReturn(response);

        RatingResponse result = ratingService.updateMyLessonRating(LESSON_ID, request);

        assertEquals(response, result);
        verify(ratingMapper).updateRating(request, rating);
    }

    @Test
    void deleteMyLessonRating_shouldDelete() {
        authenticate();

        when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(ratingRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID)).thenReturn(Optional.of(rating));

        ratingService.deleteMyLessonRating(LESSON_ID);

        verify(ratingRepository).delete(rating);
    }
}
