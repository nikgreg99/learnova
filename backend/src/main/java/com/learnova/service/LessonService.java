package com.learnova.service;

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
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final LessonMapper lessonMapper;

    public LessonService(LessonRepository lessonRepository, CourseRepository courseRepository,
            LessonMapper lessonMapper) {
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
        this.lessonMapper = lessonMapper;
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> getLessons(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }
        return lessonRepository.findByCourseIdOrderByPositionAsc(courseId).stream().map(lessonMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LessonResponse getLessonById(Long courseId, Long lessonId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }
        return lessonRepository.findByIdAndCourseId(lessonId, courseId).map(lessonMapper::toResponse)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));
    }

    public LessonResponse createLesson(Long courseId, LessonRequest request) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new CourseNotFoundException(courseId));

        if (lessonRepository.existsByCourseIdAndPosition(courseId, request.position())) {
            throw new LessonPositionConflictException(request.position());
        }

        Lesson lesson = lessonMapper.toEntity(request);
        lesson.setCourse(course);
        Lesson savedLesson = lessonRepository.save(lesson);
        return lessonMapper.toResponse(savedLesson);
    }

    public LessonResponse updateLesson(Long courseId, Long lessonId, LessonRequest request) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }

        Lesson lesson = lessonRepository.findByIdAndCourseId(lessonId, courseId)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));

        if (lessonRepository.existsByCourseIdAndPositionAndIdNot(courseId, request.position(), lessonId)) {
            throw new LessonPositionConflictException(request.position());
        }

        lessonMapper.updateLesson(request, lesson);
        Lesson updatedLesson = lessonRepository.save(lesson);
        return lessonMapper.toResponse(updatedLesson);
    }

    public LessonResponse patchLesson(Long courseId, Long lessonId, LessonPatchRequest request) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }

        Lesson lesson = lessonRepository.findByIdAndCourseId(lessonId, courseId)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));

        if (request.position() != null
                && lessonRepository.existsByCourseIdAndPositionAndIdNot(courseId, request.position(), lessonId)) {
            throw new LessonPositionConflictException(request.position());
        }

        lessonMapper.patchLesson(request, lesson);
        Lesson patchedLesson = lessonRepository.save(lesson);
        return lessonMapper.toResponse(patchedLesson);
    }

    public void deleteLesson(Long courseId, Long lessonId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }

        Lesson lesson = lessonRepository.findByIdAndCourseId(lessonId, courseId)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));

        lessonRepository.delete(lesson);
    }
}
