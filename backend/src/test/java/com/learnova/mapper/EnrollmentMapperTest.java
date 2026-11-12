package com.learnova.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnova.dto.enrollment.EnrollmentResponse;
import com.learnova.model.Course;
import com.learnova.model.Enrollment;
import com.learnova.model.EnrollmentStatus;
import com.learnova.model.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class EnrollmentMapperTest {

    private final EnrollmentMapper enrollmentMapper = Mappers.getMapper(EnrollmentMapper.class);

    @Test
    void toResponse_shouldMapEnrollmentSuccessfully() {
        User student = User.builder()
                .id(1L)
                .username("student1")
                .build();

        Course course = new Course();
        course.setId(10L);
        course.setName("Spring Boot Microservices");

        LocalDateTime now = LocalDateTime.now();
        Enrollment enrollment = Enrollment.builder()
                .id(100L)
                .student(student)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(now)
                .updatedAt(now)
                .build();

        EnrollmentResponse response = enrollmentMapper.toResponse(enrollment);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.studentId()).isEqualTo(1L);
        assertThat(response.studentUsername()).isEqualTo("student1");
        assertThat(response.courseId()).isEqualTo(10L);
        assertThat(response.courseName()).isEqualTo("Spring Boot Microservices");
        assertThat(response.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(response.enrolledAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }

    @Test
    void toResponse_shouldReturnNullWhenEnrollmentIsNull() {
        assertThat(enrollmentMapper.toResponse(null)).isNull();
    }
}
