package com.learnova.service;

import com.learnova.config.security.SecurityUtils;
import com.learnova.dto.course.CoursePatchRequest;
import com.learnova.dto.course.CourseReadinessResponse;
import com.learnova.dto.course.CourseRequest;
import com.learnova.dto.course.CourseResponse;
import com.learnova.exception.CourseConflictException;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.exception.InvalidCourseStateException;
import com.learnova.exception.KeycloakUserNotFoundException;
import com.learnova.mapper.CourseMapper;
import com.learnova.model.Course;
import com.learnova.model.CourseStatus;
import com.learnova.model.EnrollmentStatus;
import com.learnova.model.User;
import com.learnova.repository.CourseRepository;
import com.learnova.repository.EnrollmentRepository;
import com.learnova.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CourseService(CourseRepository courseRepository, CourseMapper courseMapper, UserRepository userRepository,
            EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<CourseResponse> getCourses() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        if (isAuthenticated && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
        return courseRepository.findAll().stream()
                .map(courseMapper::toResponse)
                .toList();
        }
        if (isAuthenticated && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_INSTRUCTOR"))) {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        return courseRepository.findAll().stream()
                .filter(course -> course.getStatus() == CourseStatus.PUBLISHED
                        || (course.getInstructor() != null
                                && keycloakId.equals(course.getInstructor().getKeycloakId())))
                .map(courseMapper::toResponse)
                .toList();
        }
        return courseRepository.findByStatus(CourseStatus.PUBLISHED).stream()
                .map(courseMapper::toResponse)
                .toList();
    }

    public Optional<CourseResponse> getCourseById(Long id) {
        return courseRepository.findById(id).map(courseMapper::toResponse);
    }

    public CourseResponse addCourse(CourseRequest courseRequest) {
        var course = courseMapper.toEntity(courseRequest);

        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        User instructor = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new KeycloakUserNotFoundException(keycloakId));
        course.setInstructor(instructor);

        var savedCourse = courseRepository.save(course);
        return courseMapper.toResponse(savedCourse);
    }

    public Optional<CourseResponse> updateCourse(Long courseId, CourseRequest courseRequest) {
        return courseRepository.findById(courseId).map(course -> {
            courseMapper.updateCourse(courseRequest, course);
            var updatedCourse = courseRepository.save(course);
            return courseMapper.toResponse(updatedCourse);
        });
    }

    public Optional<CourseResponse> patchCourse(Long courseId, CoursePatchRequest request) {
        return courseRepository.findById(courseId).map(course -> {
            courseMapper.patchCourse(request, course);
            var updatedCourse = courseRepository.save(course);
            return courseMapper.toResponse(updatedCourse);
        });
    }

    public Optional<CourseResponse> updateCourseStatus(Long courseId, CourseStatus status) {
        return courseRepository.findById(courseId).map(course -> {
            if (course.getStatus() == status) {
                return courseMapper.toResponse(course);
            }

            if (course.getStatus() == CourseStatus.DRAFT && status == CourseStatus.PUBLISHED) {
                List<String> reasons = getReadinessIssues(course);
                if (!reasons.isEmpty()) {
                    throw new InvalidCourseStateException("Course cannot be published: " + String.join(", ", reasons));
                }
            }

            if (course.getStatus() == CourseStatus.PUBLISHED && status == CourseStatus.DRAFT) {
                if (enrollmentRepository.existsByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE)) {
                    throw new CourseConflictException("Cannot revert course to DRAFT while active enrollments exist");
                }
            }

            if (course.getStatus() == CourseStatus.ARCHIVED && status == CourseStatus.DRAFT) {
                throw new InvalidCourseStateException("Archived course cannot be reverted directly to DRAFT");
            }

            course.setStatus(status);
            var updatedCourse = courseRepository.save(course);
            return courseMapper.toResponse(updatedCourse);
        });
    }

    public CourseReadinessResponse checkCourseReadiness(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        List<String> reasons = getReadinessIssues(course);
        return new CourseReadinessResponse(reasons.isEmpty(), reasons);
    }

    private List<String> getReadinessIssues(Course course) {
        List<String> reasons = new ArrayList<>();
        if (course.getName() == null || course.getName().isBlank()) {
            reasons.add("Course name is required");
        }
        if (course.getDescription() == null || course.getDescription().isBlank()) {
            reasons.add("Course description is required");
        }
        if (course.getLessons() == null || course.getLessons().isEmpty()) {
            reasons.add("Course must contain at least one lesson");
        }
        return reasons;
    }

    public void deleteCourseById(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new CourseNotFoundException(id);
        }
        if (enrollmentRepository.existsByCourseIdAndStatus(id, EnrollmentStatus.ACTIVE)) {
            throw new CourseConflictException("Cannot delete course while active enrollments exist");
        }
        courseRepository.deleteById(id);
    }
}
