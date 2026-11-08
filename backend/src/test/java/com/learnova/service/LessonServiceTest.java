package com.learnova.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnova.dto.lesson.LessonPatchRequest;
import com.learnova.dto.lesson.LessonRequest;
import com.learnova.dto.lesson.LessonResponse;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.exception.LessonNotFoundException;
import com.learnova.exception.LessonPositionConflictException;
import com.learnova.mapper.LessonMapper;
import com.learnova.model.Course;
import com.learnova.model.Lesson;
import com.learnova.repository.CourseRepository;
import com.learnova.repository.LessonRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonMapper lessonMapper;

    @InjectMocks
    private LessonService lessonService;

    private Course course;
    private Lesson lesson;
    private LessonRequest lessonRequest;
    private LessonResponse lessonResponse;
    private final Long courseId = 1L;
    private final Long lessonId = 10L;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(courseId);
        course.setName("Java Fundamentals");

        lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setCourse(course);
        lesson.setTitle("Variables & Data Types");
        lesson.setDescription("Learn primitive types");
        lesson.setContent("Content here");
        lesson.setPosition(1);
        lesson.setDuration(15);
        lesson.setCreatedAt(LocalDateTime.now());
        lesson.setUpdatedAt(LocalDateTime.now());

        lessonRequest = new LessonRequest("Variables & Data Types", "Learn primitive types", "Content here", 1, 15);

        lessonResponse = new LessonResponse(lessonId, courseId, "Variables & Data Types", "Learn primitive types",
                "Content here", 1, 15, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void getLessons_shouldReturnSortedLessons() {
        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(lessonRepository.findByCourseIdOrderByPositionAsc(courseId)).thenReturn(List.of(lesson));
        when(lessonMapper.toResponse(lesson)).thenReturn(lessonResponse);

        List<LessonResponse> result = lessonService.getLessons(courseId);

        assertEquals(1, result.size());
        assertEquals(lessonResponse, result.getFirst());
        verify(courseRepository).existsById(courseId);
        verify(lessonRepository).findByCourseIdOrderByPositionAsc(courseId);
    }

    @Test
    void getLessons_shouldThrowCourseNotFound_whenCourseDoesNotExist() {
        when(courseRepository.existsById(courseId)).thenReturn(false);

        assertThrows(CourseNotFoundException.class, () -> lessonService.getLessons(courseId));
        verify(lessonRepository, never()).findByCourseIdOrderByPositionAsc(courseId);
    }

    @Test
    void getLessonById_shouldReturnLesson() {
        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(lessonRepository.findByIdAndCourseId(lessonId, courseId)).thenReturn(Optional.of(lesson));
        when(lessonMapper.toResponse(lesson)).thenReturn(lessonResponse);

        LessonResponse result = lessonService.getLessonById(courseId, lessonId);

        assertNotNull(result);
        assertEquals(lessonResponse, result);
        verify(lessonRepository).findByIdAndCourseId(lessonId, courseId);
    }

    @Test
    void getLessonById_shouldThrowCourseNotFound_whenCourseDoesNotExist() {
        when(courseRepository.existsById(courseId)).thenReturn(false);

        assertThrows(CourseNotFoundException.class, () -> lessonService.getLessonById(courseId, lessonId));
        verify(lessonRepository, never()).findByIdAndCourseId(lessonId, courseId);
    }

    @Test
    void getLessonById_shouldThrowLessonNotFound_whenLessonNotFound() {
        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(lessonRepository.findByIdAndCourseId(lessonId, courseId)).thenReturn(Optional.empty());

        assertThrows(LessonNotFoundException.class, () -> lessonService.getLessonById(courseId, lessonId));
    }

    @Test
    void createLesson_shouldSaveAndReturnLesson() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(lessonRepository.existsByCourseIdAndPosition(courseId, lessonRequest.position())).thenReturn(false);
        when(lessonMapper.toEntity(lessonRequest)).thenReturn(lesson);
        when(lessonRepository.save(lesson)).thenReturn(lesson);
        when(lessonMapper.toResponse(lesson)).thenReturn(lessonResponse);

        LessonResponse result = lessonService.createLesson(courseId, lessonRequest);

        assertNotNull(result);
        assertEquals(lessonResponse, result);
        verify(lessonRepository).save(lesson);
    }

    @Test
    void createLesson_shouldThrowCourseNotFound_whenCourseDoesNotExist() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class, () -> lessonService.createLesson(courseId, lessonRequest));
        verify(lessonRepository, never()).save(lesson);
    }

    @Test
    void createLesson_shouldThrowConflict_whenPositionConflictExists() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(lessonRepository.existsByCourseIdAndPosition(courseId, lessonRequest.position())).thenReturn(true);

        assertThrows(LessonPositionConflictException.class, () -> lessonService.createLesson(courseId, lessonRequest));
        verify(lessonRepository, never()).save(lesson);
    }

    @Test
    void updateLesson_shouldUpdateAndReturnLesson() {
        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(lessonRepository.findByIdAndCourseId(lessonId, courseId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.existsByCourseIdAndPositionAndIdNot(courseId, lessonRequest.position(), lessonId))
                .thenReturn(false);
        when(lessonRepository.save(lesson)).thenReturn(lesson);
        when(lessonMapper.toResponse(lesson)).thenReturn(lessonResponse);

        LessonResponse result = lessonService.updateLesson(courseId, lessonId, lessonRequest);

        assertNotNull(result);
        assertEquals(lessonResponse, result);
        verify(lessonMapper).updateLesson(lessonRequest, lesson);
        verify(lessonRepository).save(lesson);
    }

    @Test
    void updateLesson_shouldThrowCourseNotFound_whenCourseDoesNotExist() {
        when(courseRepository.existsById(courseId)).thenReturn(false);

        assertThrows(CourseNotFoundException.class,
                () -> lessonService.updateLesson(courseId, lessonId, lessonRequest));
        verify(lessonRepository, never()).save(lesson);
    }

    @Test
    void updateLesson_shouldThrowLessonNotFound_whenLessonDoesNotExist() {
        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(lessonRepository.findByIdAndCourseId(lessonId, courseId)).thenReturn(Optional.empty());

        assertThrows(LessonNotFoundException.class,
                () -> lessonService.updateLesson(courseId, lessonId, lessonRequest));
        verify(lessonRepository, never()).save(lesson);
    }

    @Test
    void updateLesson_shouldThrowConflict_whenPositionConflictExists() {
        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(lessonRepository.findByIdAndCourseId(lessonId, courseId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.existsByCourseIdAndPositionAndIdNot(courseId, lessonRequest.position(), lessonId))
                .thenReturn(true);

        assertThrows(LessonPositionConflictException.class,
                () -> lessonService.updateLesson(courseId, lessonId, lessonRequest));
        verify(lessonRepository, never()).save(lesson);
    }

    @Test
    void patchLesson_shouldPatchAndReturnLesson() {
        LessonPatchRequest patchRequest = new LessonPatchRequest("New Title", null, null, null, null);

        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(lessonRepository.findByIdAndCourseId(lessonId, courseId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(lesson)).thenReturn(lesson);
        when(lessonMapper.toResponse(lesson)).thenReturn(lessonResponse);

        LessonResponse result = lessonService.patchLesson(courseId, lessonId, patchRequest);

        assertNotNull(result);
        assertEquals(lessonResponse, result);
        verify(lessonMapper).patchLesson(patchRequest, lesson);
        verify(lessonRepository).save(lesson);
    }

    @Test
    void patchLesson_shouldThrowConflict_whenPositionConflictExists() {
        LessonPatchRequest patchRequest = new LessonPatchRequest(null, null, null, 2, null);

        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(lessonRepository.findByIdAndCourseId(lessonId, courseId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.existsByCourseIdAndPositionAndIdNot(courseId, 2, lessonId)).thenReturn(true);

        assertThrows(LessonPositionConflictException.class,
                () -> lessonService.patchLesson(courseId, lessonId, patchRequest));
        verify(lessonRepository, never()).save(lesson);
    }

    @Test
    void deleteLesson_shouldDeleteLesson() {
        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(lessonRepository.findByIdAndCourseId(lessonId, courseId)).thenReturn(Optional.of(lesson));

        lessonService.deleteLesson(courseId, lessonId);

        verify(lessonRepository).delete(lesson);
    }

    @Test
    void deleteLesson_shouldThrowCourseNotFound_whenCourseDoesNotExist() {
        when(courseRepository.existsById(courseId)).thenReturn(false);

        assertThrows(CourseNotFoundException.class, () -> lessonService.deleteLesson(courseId, lessonId));
        verify(lessonRepository, never()).delete(lesson);
    }

    @Test
    void deleteLesson_shouldThrowLessonNotFound_whenLessonDoesNotExist() {
        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(lessonRepository.findByIdAndCourseId(lessonId, courseId)).thenReturn(Optional.empty());

        assertThrows(LessonNotFoundException.class, () -> lessonService.deleteLesson(courseId, lessonId));
        verify(lessonRepository, never()).delete(lesson);
    }
}
