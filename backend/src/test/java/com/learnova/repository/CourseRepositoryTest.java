package com.learnova.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learnova.model.Course;
import java.math.BigDecimal;
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
public class CourseRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Course sampleCourse;

    @BeforeEach
    void setUp() {
        sampleCourse = new Course();
        sampleCourse.setName("Docker");
        sampleCourse.setDescription("Master Docker");
        sampleCourse.setPrice(new BigDecimal("49.90"));
    }

    @Test
    void shouldSaveCourse() {
        Course savedCourse = courseRepository.save(sampleCourse);

        assertNotNull(savedCourse.getId());
        assertNotNull(savedCourse.getCreatedAt());
        assertEquals("Docker", savedCourse.getName());
    }

    @Test
    void shouldFindCourseById() {
        Course savedCourse = entityManager.persistAndFlush(sampleCourse);
        entityManager.clear();

        Optional<Course> retrievedCourse = courseRepository.findById(savedCourse.getId());

        assertTrue(retrievedCourse.isPresent());
        assertEquals("Docker", retrievedCourse.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenCourseNotFound() {
        Optional<Course> result = courseRepository.findById(999L);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldDeleteCourse() {
        Course savedCourse = entityManager.persistAndFlush(sampleCourse);
        entityManager.clear();

        courseRepository.deleteById(savedCourse.getId());

        Optional<Course> result = courseRepository.findById(savedCourse.getId());
        assertTrue(result.isEmpty());
    }
}
