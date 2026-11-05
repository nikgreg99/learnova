package com.learnova.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class CourseTest {

    @Test
    void shouldCreateCourseWithExpectedValues() {
        Course course = new Course();
        course.setName("Java Fundamentals");
        course.setDescription("Introduction to Java");
        course.setPrice(new BigDecimal("49.90"));

        assertEquals("Java Fundamentals", course.getName());
        assertEquals("Introduction to Java", course.getDescription());
        assertEquals(0, new BigDecimal("49.90").compareTo(course.getPrice()));

        assertNull(course.getId());
        assertNull(course.getCreatedAt());
        assertNull(course.getUpdatedAt());
    }
}
