package com.learnova.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnova.dto.course.CourseRequest;
import com.learnova.dto.course.CourseResponse;
import com.learnova.model.Course;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CourseMapperTest {

    private final CourseMapper courseMapper =
        Mappers.getMapper(CourseMapper.class);

    private Course course;

    @BeforeEach
    void setUp() {
        course = createCourse();
    }

    @Test
    void toEntity_shouldMapCourseRequest() {
        Course result = courseMapper.toEntity(createRequest());

        assertThat(result.getName()).isEqualTo("Advanced Java");
        assertThat(result.getDescription()).isEqualTo("Advanced concepts");
        assertThat(result.getPrice()).isEqualByComparingTo("99.99");
    }

    @Test
    void toResponse_shouldMapCourse() {
        CourseResponse result = courseMapper.toResponse(course);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Spring Boot");
        assertThat(result.description()).isEqualTo("Spring Boot fundamentals");
        assertThat(result.price()).isEqualByComparingTo("49.99");
    }

    @Test
    void updateCourse_shouldUpdateExistingCourse() {
        courseMapper.updateCourse(createRequest(), course);

        assertThat(course.getName()).isEqualTo("Advanced Java");
        assertThat(course.getDescription()).isEqualTo("Advanced concepts");
        assertThat(course.getPrice()).isEqualByComparingTo("99.99");

        assertUnchangedFields();
    }

    private CourseRequest createRequest() {
        return new CourseRequest(
            "Advanced Java",
            "Advanced concepts",
            new BigDecimal("99.99")
        );
    }

    private Course createCourse() {
        Course entity = new Course();
        entity.setId(1L);
        entity.setName("Spring Boot");
        entity.setDescription("Spring Boot fundamentals");
        entity.setPrice(new BigDecimal("49.99"));
        return entity;
    }

    private void assertUnchangedFields() {
        assertThat(course.getId()).isEqualTo(1L);
    }
}
