package com.learnova.model;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LessonTest {

    @Test
    void lesson_ShouldCreateLessonUsingNoArgsConstructor() {
        Lesson lesson = new Lesson();

        assertNull(lesson.getId());
        assertNull(lesson.getCourse());
        assertNull(lesson.getTitle());
        assertNull(lesson.getDescription());
        assertNull(lesson.getContent());
        assertNull(lesson.getPosition());
        assertNull(lesson.getDuration());
        assertNull(lesson.getCreatedAt());
        assertNull(lesson.getUpdatedAt());
    }

    @Test
    void lesson_ShouldCreateLessonUsingAllArgsConstructor() {
        Course course = new Course();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        Lesson lesson = new Lesson(
            1L,
            course,
            "Introduction to Java",
            "Java introduction",
            "Java lesson content",
            1,
            3600,
            createdAt,
            updatedAt
        );

        assertEquals(1L, lesson.getId());
        assertSame(course, lesson.getCourse());
        assertEquals("Introduction to Java", lesson.getTitle());
        assertEquals("Java introduction", lesson.getDescription());
        assertEquals("Java lesson content", lesson.getContent());
        assertEquals(1, lesson.getPosition());
        assertEquals(3600, lesson.getDuration());
        assertEquals(createdAt, lesson.getCreatedAt());
        assertEquals(updatedAt, lesson.getUpdatedAt());
    }

    @Test
    void lesson_ShouldCreateLessonUsingBuilder() {
        Course course = new Course();

        Lesson lesson = Lesson.builder()
            .id(1L)
            .course(course)
            .title("Spring Boot Basics")
            .description("Introduction to Spring Boot")
            .content("Spring Boot lesson content")
            .position(2)
            .duration(1800)
            .build();

        assertEquals(1L, lesson.getId());
        assertSame(course, lesson.getCourse());
        assertEquals("Spring Boot Basics", lesson.getTitle());
        assertEquals("Introduction to Spring Boot", lesson.getDescription());
        assertEquals("Spring Boot lesson content", lesson.getContent());
        assertEquals(2, lesson.getPosition());
        assertEquals(1800, lesson.getDuration());
        assertNull(lesson.getCreatedAt());
        assertNull(lesson.getUpdatedAt());
    }

    @Test
    void lesson_ShouldUpdateLessonFields() {
        Course course = new Course();
        Lesson lesson = new Lesson();

        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        lesson.setId(1L);
        lesson.setCourse(course);
        lesson.setTitle("Updated lesson");
        lesson.setDescription("Updated description");
        lesson.setContent("Updated content");
        lesson.setPosition(3);
        lesson.setDuration(2400);
        lesson.setCreatedAt(createdAt);
        lesson.setUpdatedAt(updatedAt);

        assertEquals(1L, lesson.getId());
        assertSame(course, lesson.getCourse());
        assertEquals("Updated lesson", lesson.getTitle());
        assertEquals("Updated description", lesson.getDescription());
        assertEquals("Updated content", lesson.getContent());
        assertEquals(3, lesson.getPosition());
        assertEquals(2400, lesson.getDuration());
        assertEquals(createdAt, lesson.getCreatedAt());
        assertEquals(updatedAt, lesson.getUpdatedAt());
    }

    @Test
    void lesson_ShouldAllowOptionalFieldsToBeNull() {
        Lesson lesson = Lesson.builder()
            .title("Basic lesson")
            .position(1)
            .build();

        assertNotNull(lesson);
        assertNull(lesson.getDescription());
        assertNull(lesson.getContent());
        assertNull(lesson.getDuration());
    }

    @Test
    void lesson_ShouldGenerateEqualsAndHashCode() {
        Course course = new Course();

        Lesson first = Lesson.builder()
            .id(1L)
            .course(course)
            .title("Java Basics")
            .position(1)
            .build();

        Lesson second = Lesson.builder()
            .id(1L)
            .course(course)
            .title("Java Basics")
            .position(1)
            .build();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}

