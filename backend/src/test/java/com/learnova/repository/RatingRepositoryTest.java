package com.learnova.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learnova.model.Course;
import com.learnova.model.Lesson;
import com.learnova.model.Rating;
import com.learnova.model.User;
import com.learnova.model.UserStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
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
public class RatingRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user1;
    private User user2;
    private Course course;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        user1 = User.builder().keycloakId("kc-user-1").firstName("Alice").lastName("Smith").email("alice@example.com")
                .username("alicesmith").status(UserStatus.ACTIVE).build();
        user1 = entityManager.persistAndFlush(user1);

        user2 = User.builder().keycloakId("kc-user-2").firstName("Bob").lastName("Jones").email("bob@example.com")
                .username("bobjones").status(UserStatus.ACTIVE).build();
        user2 = entityManager.persistAndFlush(user2);

        course = new Course();
        course.setName("Spring Boot Microservices");
        course.setDescription("Learn modern microservices");
        course.setPrice(new BigDecimal("59.99"));
        course = entityManager.persistAndFlush(course);

        lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setTitle("Service Discovery with Eureka");
        lesson.setDescription("Setup Eureka server and client");
        lesson.setPosition(1);
        lesson.setDuration(15);
        lesson = entityManager.persistAndFlush(lesson);

        entityManager.clear();
    }

    @Test
    void shouldSaveAndFindCourseRating() {
        Rating rating = Rating.builder().user(user1).course(course).rating(5).review("Outstanding course!").build();

        Rating saved = ratingRepository.save(rating);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(5, saved.getRating());
        assertEquals("Outstanding course!", saved.getReview());
        assertEquals(course.getId(), saved.getCourse().getId());
    }

    @Test
    void shouldSaveAndFindLessonRating() {
        Rating rating = Rating.builder().user(user1).lesson(lesson).rating(4).review("Very informative lesson.")
                .build();

        Rating saved = ratingRepository.save(rating);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(4, saved.getRating());
        assertEquals("Very informative lesson.", saved.getReview());
        assertEquals(lesson.getId(), saved.getLesson().getId());
    }

    @Test
    void shouldFindRatingsByCourseId() {
        Rating r1 = Rating.builder().user(user1).course(course).rating(5).review("Great!").build();
        Rating r2 = Rating.builder().user(user2).course(course).rating(4).review("Good!").build();
        entityManager.persist(r1);
        entityManager.persist(r2);
        entityManager.flush();
        entityManager.clear();

        List<Rating> ratings = ratingRepository.findByCourseId(course.getId());

        assertEquals(2, ratings.size());
    }

    @Test
    void shouldFindRatingsByLessonId() {
        Rating r1 = Rating.builder().user(user1).lesson(lesson).rating(4).review("Clear!").build();
        Rating r2 = Rating.builder().user(user2).lesson(lesson).rating(3).review("Average").build();
        entityManager.persist(r1);
        entityManager.persist(r2);
        entityManager.flush();
        entityManager.clear();

        List<Rating> ratings = ratingRepository.findByLessonId(lesson.getId());

        assertEquals(2, ratings.size());
    }

    @Test
    void shouldFindByUserIdAndCourseId() {
        Rating r = Rating.builder().user(user1).course(course).rating(5).build();
        entityManager.persistAndFlush(r);
        entityManager.clear();

        Optional<Rating> found = ratingRepository.findByUserIdAndCourseId(user1.getId(), course.getId());
        Optional<Rating> notFound = ratingRepository.findByUserIdAndCourseId(user2.getId(), course.getId());

        assertTrue(found.isPresent());
        assertEquals(5, found.get().getRating());
        assertTrue(notFound.isEmpty());
    }

    @Test
    void shouldFindByUserIdAndLessonId() {
        Rating r = Rating.builder().user(user1).lesson(lesson).rating(4).build();
        entityManager.persistAndFlush(r);
        entityManager.clear();

        Optional<Rating> found = ratingRepository.findByUserIdAndLessonId(user1.getId(), lesson.getId());
        Optional<Rating> notFound = ratingRepository.findByUserIdAndLessonId(user2.getId(), lesson.getId());

        assertTrue(found.isPresent());
        assertEquals(4, found.get().getRating());
        assertTrue(notFound.isEmpty());
    }

    @Test
    void shouldCheckExistsByUserIdAndCourseId() {
        Rating r = Rating.builder().user(user1).course(course).rating(5).build();
        entityManager.persistAndFlush(r);
        entityManager.clear();

        assertTrue(ratingRepository.existsByUserIdAndCourseId(user1.getId(), course.getId()));
        assertFalse(ratingRepository.existsByUserIdAndCourseId(user2.getId(), course.getId()));
    }

    @Test
    void shouldCheckExistsByUserIdAndLessonId() {
        Rating r = Rating.builder().user(user1).lesson(lesson).rating(4).build();
        entityManager.persistAndFlush(r);
        entityManager.clear();

        assertTrue(ratingRepository.existsByUserIdAndLessonId(user1.getId(), lesson.getId()));
        assertFalse(ratingRepository.existsByUserIdAndLessonId(user2.getId(), lesson.getId()));
    }

    @Test
    void shouldCalculateAverageRatingAndCountByCourseId() {
        Rating r1 = Rating.builder().user(user1).course(course).rating(5).build();
        Rating r2 = Rating.builder().user(user2).course(course).rating(4).build();
        entityManager.persist(r1);
        entityManager.persist(r2);
        entityManager.flush();
        entityManager.clear();

        Optional<Double> avg = ratingRepository.calculateAverageRatingByCourseId(course.getId());
        long count = ratingRepository.countByCourseId(course.getId());

        assertTrue(avg.isPresent());
        assertEquals(4.5, avg.get(), 0.001);
        assertEquals(2, count);
    }

    @Test
    void shouldCalculateAverageRatingAndCountByLessonId() {
        Rating r1 = Rating.builder().user(user1).lesson(lesson).rating(5).build();
        Rating r2 = Rating.builder().user(user2).lesson(lesson).rating(3).build();
        entityManager.persist(r1);
        entityManager.persist(r2);
        entityManager.flush();
        entityManager.clear();

        Optional<Double> avg = ratingRepository.calculateAverageRatingByLessonId(lesson.getId());
        long count = ratingRepository.countByLessonId(lesson.getId());

        assertTrue(avg.isPresent());
        assertEquals(4.0, avg.get(), 0.001);
        assertEquals(2, count);
    }

    @Test
    void shouldReturnEmptyAverageWhenNoRatingsExist() {
        Optional<Double> courseAvg = ratingRepository.calculateAverageRatingByCourseId(999L);
        Optional<Double> lessonAvg = ratingRepository.calculateAverageRatingByLessonId(999L);

        assertTrue(courseAvg.isEmpty());
        assertTrue(lessonAvg.isEmpty());
        assertEquals(0, ratingRepository.countByCourseId(999L));
        assertEquals(0, ratingRepository.countByLessonId(999L));
    }

    @Test
    void shouldFailWhenRatingViolatesMutualExclusivityBothPresent() {
        Rating invalid = Rating.builder().user(user1).course(course).lesson(lesson).rating(5).build();

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persistAndFlush(invalid);
        });
    }

    @Test
    void shouldFailWhenRatingViolatesMutualExclusivityNeitherPresent() {
        Rating invalid = Rating.builder().user(user1).course(null).lesson(null).rating(5).build();

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persistAndFlush(invalid);
        });
    }

    @Test
    void shouldFailWhenRatingScoreIsBelowMinimum() {
        Rating invalid = Rating.builder().user(user1).course(course).rating(0).build();

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persistAndFlush(invalid);
        });
    }

    @Test
    void shouldFailWhenRatingScoreIsAboveMaximum() {
        Rating invalid = Rating.builder().user(user1).course(course).rating(6).build();

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persistAndFlush(invalid);
        });
    }

    @Test
    void shouldFailOnDuplicateRatingByUserForSameCourse() {
        Rating r1 = Rating.builder().user(user1).course(course).rating(5).build();
        entityManager.persistAndFlush(r1);

        Rating r2 = Rating.builder().user(user1).course(course).rating(4).build();

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persistAndFlush(r2);
        });
    }

    @Test
    void shouldFailOnDuplicateRatingByUserForSameLesson() {
        Rating r1 = Rating.builder().user(user1).lesson(lesson).rating(5).build();
        entityManager.persistAndFlush(r1);

        Rating r2 = Rating.builder().user(user1).lesson(lesson).rating(4).build();

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persistAndFlush(r2);
        });
    }
}
