package com.learnova.service;

import com.learnova.config.security.SecurityUtils;
import com.learnova.dto.enrollment.EnrollmentResponse;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.exception.DuplicateEnrollmentException;
import com.learnova.exception.EnrollmentNotFoundException;
import com.learnova.exception.InvalidEnrollmentStateException;
import com.learnova.exception.KeycloakUserNotFoundException;
import com.learnova.mapper.EnrollmentMapper;
import com.learnova.model.Course;
import com.learnova.model.Enrollment;
import com.learnova.model.EnrollmentStatus;
import com.learnova.model.User;
import com.learnova.repository.CourseRepository;
import com.learnova.repository.EnrollmentRepository;
import com.learnova.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentMapper enrollmentMapper;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             CourseRepository courseRepository,
                             UserRepository userRepository,
                             EnrollmentMapper enrollmentMapper) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentMapper = enrollmentMapper;
    }

    public EnrollmentResponse enrollStudent(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        User student = getCurrentUser();

        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new DuplicateEnrollmentException("Student is already enrolled in this course");
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        return enrollmentMapper.toResponse(saved);
    }

    public EnrollmentResponse cancelEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("Enrollment not found with id: " + enrollmentId));

        User currentUser = getCurrentUser();
        boolean isAdmin = hasRole("ROLE_ADMIN");

        if (!isAdmin && !enrollment.getStudent().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to cancel this enrollment");
        }

        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            throw new InvalidEnrollmentStateException("Cannot cancel a completed enrollment");
        }

        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new InvalidEnrollmentStateException("Enrollment is already cancelled");
        }

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        Enrollment updated = enrollmentRepository.save(enrollment);
        return enrollmentMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getMyEnrollments(Pageable pageable, EnrollmentStatus status) {
        User student = getCurrentUser();

        Page<Enrollment> page;
        if (status != null) {
            page = enrollmentRepository.findByStudentIdAndStatus(student.getId(), status, pageable);
        } else {
            page = enrollmentRepository.findByStudentId(student.getId(), pageable);
        }

        return page.map(enrollmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getCourseEnrollments(Long courseId, Pageable pageable, EnrollmentStatus status) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        User currentUser = getCurrentUser();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        boolean isOwner = course.getInstructor() != null && course.getInstructor().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You are not authorized to view enrollments for this course");
        }

        Page<Enrollment> page;
        if (status != null) {
            page = enrollmentRepository.findByCourseIdAndStatus(courseId, status, pageable);
        } else {
            page = enrollmentRepository.findByCourseId(courseId, pageable);
        }

        return page.map(enrollmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("Enrollment not found with id: " + enrollmentId));

        User currentUser = getCurrentUser();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        boolean isStudent = enrollment.getStudent().getId().equals(currentUser.getId());
        boolean isInstructor = enrollment.getCourse().getInstructor() != null
                && enrollment.getCourse().getInstructor().getId().equals(currentUser.getId());

        if (!isAdmin && !isStudent && !isInstructor) {
            throw new AccessDeniedException("You are not authorized to view this enrollment");
        }

        return enrollmentMapper.toResponse(enrollment);
    }

    private User getCurrentUser() {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new KeycloakUserNotFoundException(keycloakId));
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
    }
}
