package com.learnova.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnova.config.security.SecurityUtils;
import com.learnova.dto.progress.CourseProgressResponse;
import com.learnova.dto.progress.LessonProgressResponse;
import com.learnova.mapper.CourseProgressMapper;
import com.learnova.mapper.LessonProgressMapper;
import com.learnova.model.Course;
import com.learnova.model.EnrollmentStatus;
import com.learnova.model.Lesson;
import com.learnova.model.LessonProgress;
import com.learnova.model.ProgressStatus;
import com.learnova.model.User;
import com.learnova.repository.CourseRepository;
import com.learnova.repository.EnrollmentRepository;
import com.learnova.repository.LessonProgressRepository;
import com.learnova.repository.LessonRepository;
import com.learnova.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class LessonProgressServiceTest {

    private static final String KEYCLOAK_ID = "student-keycloak-id";

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LessonProgressMapper lessonProgressMapper;

    @Mock
    private CourseProgressMapper courseProgressMapper;

    @InjectMocks
    private LessonProgressService lessonProgressService;

    private User user;
    private Course course;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        course = new Course();
        course.setId(10L);
        course.setName("Java Fundamentals");

        lesson = new Lesson();
        lesson.setId(100L);
        lesson.setTitle("Introduction to Java");
        lesson.setCourse(course);
        lesson.setPosition(1);
    }


    @Test
    void shouldUpdateLessonProgressToCompleted() {
        LessonProgressResponse expectedResponse = new LessonProgressResponse(
            100L,
            "Introduction to Java",
            ProgressStatus.COMPLETED,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(userRepository.findByKeycloakId(KEYCLOAK_ID))
            .thenReturn(Optional.of(user));

        when(lessonRepository.findById(100L))
            .thenReturn(Optional.of(lesson));

        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
            1L, 10L, EnrollmentStatus.ACTIVE
        )).thenReturn(true);

        when(lessonProgressRepository.findByUser_IdAndLesson_Id(1L, 100L))
            .thenReturn(Optional.empty());

        when(lessonProgressRepository.save(any(LessonProgress.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(lessonProgressMapper.toResponse(any(LessonProgress.class)))
            .thenReturn(expectedResponse);

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {

            securityUtils.when(SecurityUtils::getCurrentKeycloakId)
                .thenReturn(KEYCLOAK_ID);

            LessonProgressResponse response =
                lessonProgressService.updateLessonProgress(
                    100L,
                    ProgressStatus.COMPLETED
                );

            assertEquals(ProgressStatus.COMPLETED, response.status());
            assertEquals(100L, response.lessonId());
            assertEquals("Introduction to Java", response.lessonTitle());

            verify(lessonProgressRepository).save(any(LessonProgress.class));
            verify(lessonProgressMapper).toResponse(any(LessonProgress.class));
        }
    }

    @Test
    void shouldResetCompletedAtWhenStatusChangesToInProgress() {
        LessonProgress existingProgress = new LessonProgress();
        existingProgress.setUser(user);
        existingProgress.setLesson(lesson);
        existingProgress.setStatus(ProgressStatus.COMPLETED);
        existingProgress.setCompletedAt(LocalDateTime.now());

        when(userRepository.findByKeycloakId(KEYCLOAK_ID))
            .thenReturn(Optional.of(user));

        when(lessonRepository.findById(100L))
            .thenReturn(Optional.of(lesson));

        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
            1L, 10L, EnrollmentStatus.ACTIVE
        )).thenReturn(true);

        when(lessonProgressRepository.findByUser_IdAndLesson_Id(1L, 100L))
            .thenReturn(Optional.of(existingProgress));

        when(lessonProgressRepository.save(any(LessonProgress.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(lessonProgressMapper.toResponse(any(LessonProgress.class)))
            .thenAnswer(invocation -> {
                LessonProgress progress = invocation.getArgument(0);

                return new LessonProgressResponse(
                    100L,
                    "Introduction to Java",
                    progress.getStatus(),
                    progress.getCompletedAt(),
                    progress.getUpdatedAt()
                );
            });

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {

            securityUtils.when(SecurityUtils::getCurrentKeycloakId)
                .thenReturn(KEYCLOAK_ID);

            LessonProgressResponse response =
                lessonProgressService.updateLessonProgress(
                    100L,
                    ProgressStatus.IN_PROGRESS
                );

            assertEquals(ProgressStatus.IN_PROGRESS, response.status());
            assertNull(response.completedAt());

            verify(lessonProgressRepository).save(existingProgress);
        }
    }

    @Test
    void shouldRejectProgressUpdateWithoutActiveEnrollment() {
        when(userRepository.findByKeycloakId(KEYCLOAK_ID))
            .thenReturn(Optional.of(user));

        when(lessonRepository.findById(100L))
            .thenReturn(Optional.of(lesson));

        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
            1L, 10L, EnrollmentStatus.ACTIVE
        )).thenReturn(false);

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {

            securityUtils.when(SecurityUtils::getCurrentKeycloakId)
                .thenReturn(KEYCLOAK_ID);

            assertThrows(
                AccessDeniedException.class,
                () -> lessonProgressService.updateLessonProgress(
                    100L,
                    ProgressStatus.COMPLETED
                )
            );

            verify(lessonProgressRepository, never())
                .save(any(LessonProgress.class));
        }
    }

    @Test
    void shouldCalculateCourseProgress() {
        CourseProgressResponse expectedResponse = new CourseProgressResponse(
            10L,
            "Java Fundamentals",
            1,
            0,
            0,
            1,
            0.0,
            List.of()
        );

        when(userRepository.findByKeycloakId(KEYCLOAK_ID))
            .thenReturn(Optional.of(user));

        when(courseRepository.findById(10L))
            .thenReturn(Optional.of(course));

        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
            1L, 10L, EnrollmentStatus.ACTIVE
        )).thenReturn(true);

        when(lessonRepository.findByCourseIdOrderByPositionAsc(10L))
            .thenReturn(List.of(lesson));

        when(lessonProgressRepository
            .findAllByUser_IdAndLesson_Course_IdOrderByLesson_PositionAsc(
                1L, 10L
            ))
            .thenReturn(List.of());

        when(courseProgressMapper.toResponse(
            eq(course),
            eq(1),
            eq(0),
            eq(0),
            eq(1),
            eq(0.0),
            any(List.class)
        )).thenReturn(expectedResponse);

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {

            securityUtils.when(SecurityUtils::getCurrentKeycloakId)
                .thenReturn(KEYCLOAK_ID);

            CourseProgressResponse response =
                lessonProgressService.getCourseProgress(10L);

            assertEquals(10L, response.courseId());
            assertEquals(1, response.totalLessons());
            assertEquals(0, response.completedLessons());
            assertEquals(1, response.notStartedLessons());
            assertEquals(0.0, response.completionPercentage());

            verify(courseProgressMapper).toResponse(
                eq(course),
                eq(1),
                eq(0),
                eq(0),
                eq(1),
                eq(0.0),
                any(List.class)
            );
        }
    }
}
