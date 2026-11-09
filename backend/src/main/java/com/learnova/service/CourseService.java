package com.learnova.service;

import com.learnova.config.security.SecurityUtils;
import com.learnova.dto.course.CoursePatchRequest;
import com.learnova.dto.course.CourseRequest;
import com.learnova.dto.course.CourseResponse;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.exception.KeycloakUserNotFoundException;
import com.learnova.mapper.CourseMapper;
import com.learnova.model.CourseStatus;
import com.learnova.model.User;
import com.learnova.repository.CourseRepository;
import com.learnova.repository.UserRepository;
import jakarta.transaction.Transactional;
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

    public CourseService(CourseRepository courseRepository, CourseMapper courseMapper, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
        this.userRepository = userRepository;
    }

    public List<CourseResponse> getCourses() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return courseRepository.findAll().stream().map(courseMapper::toResponse).toList();
        }

        boolean isInstructor = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INSTRUCTOR"));

        if (isInstructor) {
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
            course.setStatus(status);
            var updatedCourse = courseRepository.save(course);
            return courseMapper.toResponse(updatedCourse);
        });
    }

    public void deleteCourseById(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new CourseNotFoundException(id);
        }
        courseRepository.deleteById(id);
    }
}
