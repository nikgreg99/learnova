package com.learnova.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learnova.model.Course;
import com.learnova.model.Lesson;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class LessonRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Course course;
    private Lesson lesson1;
    private Lesson lesson2;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setName("Java Fundamentals");
        course.setDescription("Learn Java");
        course.setPrice(new BigDecimal("49.90"));
        course = entityManager.persistAndFlush(course);

        lesson1 = new Lesson();
        lesson1.setCourse(course);
        lesson1.setTitle("Intro");
        lesson1.setDescription("Lesson 1 Intro");
        lesson1.setPosition(1);
        lesson1.setDuration(10);
        lesson1 = entityManager.persistAndFlush(lesson1);

        lesson2 = new Lesson();
        lesson2.setCourse(course);
        lesson2.setTitle("Variables");
        lesson2.setDescription("Lesson 2 Variables");
        lesson2.setPosition(2);
        lesson2.setDuration(20);
        lesson2 = entityManager.persistAndFlush(lesson2);

        entityManager.clear();
    }

    @Test
    void shouldFindLessonsByCourseIdOrderedByPositionAsc() {
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByPositionAsc(course.getId());

        assertEquals(2, lessons.size());
        assertEquals("Intro", lessons.get(0).getTitle());
        assertEquals(1, lessons.get(0).getPosition());
        assertEquals("Variables", lessons.get(1).getTitle());
        assertEquals(2, lessons.get(1).getPosition());
    }

    @Test
    void shouldFindByIdAndCourseId() {
        Optional<Lesson> result = lessonRepository.findByIdAndCourseId(lesson1.getId(), course.getId());

        assertTrue(result.isPresent());
        assertEquals("Intro", result.get().getTitle());
    }

    @Test
    void shouldReturnEmptyWhenLessonDoesNotExistUnderCourse() {
        Optional<Lesson> result = lessonRepository.findByIdAndCourseId(lesson1.getId(), 999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldCheckPositionExistence() {
        assertTrue(lessonRepository.existsByCourseIdAndPosition(course.getId(), 1));
        assertFalse(lessonRepository.existsByCourseIdAndPosition(course.getId(), 99));
    }

    @Test
    void shouldCheckPositionExistenceExcludingOwnId() {
        // Position 1 exists for lesson1, so excluding lesson1 should return false
        assertFalse(lessonRepository.existsByCourseIdAndPositionAndIdNot(course.getId(), 1, lesson1.getId()));

        // Position 1 exists, so excluding lesson2 should return true
        assertTrue(lessonRepository.existsByCourseIdAndPositionAndIdNot(course.getId(), 1, lesson2.getId()));
    }
}
