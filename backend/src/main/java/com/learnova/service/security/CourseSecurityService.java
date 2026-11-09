package com.learnova.service.security;

import com.learnova.config.security.SecurityUtils;
import com.learnova.model.Course;
import com.learnova.repository.CourseRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("courseSecurityService")
@RequiredArgsConstructor
public class CourseSecurityService {

    private final CourseRepository courseRepository;

    public boolean isOwner(Long courseId) {

        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (authentication.getAuthorities().stream()
            .anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        String keycloakId = SecurityUtils.getCurrentKeycloakId();

        return courseRepository.findById(courseId)
            .map(Course::getInstructor)
            .map(instructor ->
                instructor.getKeycloakId().equals(keycloakId))
            .orElse(false);
    }

    public void verifyOwnership(Long courseId) {
        if (!isOwner(courseId)) {
            throw new AccessDeniedException("You are not authorized to access this course");
        }
    }
}

