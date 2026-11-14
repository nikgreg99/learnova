package com.learnova.service;

import com.learnova.config.security.SecurityUtils;
import com.learnova.dto.progress.CourseProgressResponse;
import com.learnova.dto.progress.LessonProgressResponse;
import com.learnova.dto.progress.StudentProgressResponse;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.exception.KeycloakUserNotFoundException;
import com.learnova.exception.LessonNotFoundException;
import com.learnova.mapper.CourseProgressMapper;
import com.learnova.mapper.LessonProgressMapper;
import com.learnova.model.*;
import com.learnova.repository.*;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class LessonProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final LessonProgressMapper lessonProgressMapper;
    private final CourseProgressMapper courseProgressMapper;

    public LessonProgressService(LessonProgressRepository lessonProgressRepository, LessonRepository lessonRepository, CourseRepository courseRepository, EnrollmentRepository enrollmentRepository, UserRepository userRepository, LessonProgressMapper lessonProgressMapper, CourseProgressMapper courseProgressMapper) {
        this.lessonProgressRepository = lessonProgressRepository;
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.lessonProgressMapper = lessonProgressMapper;
        this.courseProgressMapper = courseProgressMapper;
    }

    public LessonProgressResponse updateLessonProgress(
        Long lessonId,
        ProgressStatus status)
    {
        User student = getCurrentUser();

        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() ->
                new LessonNotFoundException(lessonId));

        Long courseId = lesson.getCourse().getId();

        verifyActiveEnrollment(student.getId(), courseId);

        LessonProgress progress = lessonProgressRepository
            .findByUser_IdAndLesson_Id(student.getId(), lessonId)
            .orElse(null);

        // NOT_STARTED is represented by the absence of a record.
        if (status == ProgressStatus.NOT_STARTED) {
            if (progress != null) {
                lessonProgressRepository.delete(progress);
            }

            return new LessonProgressResponse(
                lessonId,
                lesson.getTitle(),
                ProgressStatus.NOT_STARTED,
                null,
                null
            );
        }

        if (progress == null) {
            progress = new LessonProgress();
            progress.setUser(student);
            progress.setLesson(lesson);
        }

        ProgressStatus previousStatus = progress.getStatus();

        if (status == ProgressStatus.COMPLETED
            && previousStatus != ProgressStatus.COMPLETED) {
            progress.setCompletedAt(LocalDateTime.now());
        } else if (status != ProgressStatus.COMPLETED) {
            progress.setCompletedAt(null);
        }

        progress.setStatus(status);

        LessonProgress saved = lessonProgressRepository.save(progress);

        return lessonProgressMapper.toResponse(saved);
    }


    @Transactional(readOnly = true)
    public StudentProgressResponse getStudentProgress() {

        User student = getCurrentUser();

        List<Enrollment> enrollments =
            enrollmentRepository.findByStudentIdAndStatus(
                student.getId(),
                EnrollmentStatus.ACTIVE
            );

        List<CourseProgressResponse> courses = enrollments.stream()
            .map(enrollment ->
                buildCourseProgress(student, enrollment.getCourse()))
            .toList();

        return new StudentProgressResponse(courses);
    }


    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(Long courseId) {

        User student = getCurrentUser();

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new CourseNotFoundException(courseId));

        verifyActiveEnrollment(student.getId(), course.getId());

        return buildCourseProgress(student, course);

    }

    private CourseProgressResponse buildCourseProgress(User student, Course course) {
        List<Lesson> lessons =
            lessonRepository.findByCourseIdOrderByPositionAsc(
                course.getId()
            );

        List<LessonProgress> existingProgress =
            lessonProgressRepository
                .findAllByUser_IdAndLesson_Course_IdOrderByLesson_PositionAsc(
                    student.getId(),
                    course.getId()
                );

        Map<Long, LessonProgress> progressByLessonId = new HashMap<>();

        for (LessonProgress progress : existingProgress) {
            progressByLessonId.put(
                progress.getLesson().getId(),
                progress
            );
        }

        List<LessonProgressResponse> lessonResponses = new ArrayList<>();

        int completed = 0;
        int inProgress = 0;
        int notStarted = 0;

        for (Lesson lesson : lessons) {
            LessonProgress progress = progressByLessonId.get(lesson.getId());

            if (progress == null) {
                notStarted++;

                lessonResponses.add(new LessonProgressResponse(
                    lesson.getId(),
                    lesson.getTitle(),
                    ProgressStatus.NOT_STARTED,
                    null,
                    null
                ));

                continue;
            }

            switch (progress.getStatus()) {
                case COMPLETED -> completed++;
                case IN_PROGRESS -> inProgress++;
                case NOT_STARTED -> notStarted++;
            }

            lessonResponses.add(lessonProgressMapper.toResponse(progress));
        }

        int totalLessons = lessons.size();

        double completionPercentage = totalLessons != 0
            ? completed * 100.00 / totalLessons
            : 0.0;

        return courseProgressMapper.toResponse(
            course,
            totalLessons,
            completed,
            inProgress,
            notStarted,
            completionPercentage,
            lessonResponses
        );
    }

    private void verifyActiveEnrollment(Long studentId, Long courseId) {

        boolean enrolled = enrollmentRepository
            .existsByStudentIdAndCourseIdAndStatus(
                studentId,
                courseId,
                EnrollmentStatus.ACTIVE
            );

        if (!enrolled) {
            throw new AccessDeniedException(
                "You must have an active enrollment to access course progress"
            );
        }
    }

    private User getCurrentUser() {

        String keycloakId = SecurityUtils.getCurrentKeycloakId();

        return userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() ->
                new KeycloakUserNotFoundException(keycloakId));
    }
}
