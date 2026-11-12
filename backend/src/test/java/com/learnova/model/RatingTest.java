package com.learnova.model;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RatingTest {

    @Test
    void rating_ShouldCreateRatingUsingNoArgsConstructor() {
        Rating rating = new Rating();

        assertNull(rating.getId());
        assertNull(rating.getUser());
        assertNull(rating.getCourse());
        assertNull(rating.getLesson());
        assertNull(rating.getRating());
        assertNull(rating.getReview());
        assertNull(rating.getCreatedAt());
        assertNull(rating.getUpdatedAt());
    }

    @Test
    void rating_ShouldCreateRatingUsingAllArgsConstructor() {
        User user = new User();
        Course course = new Course();
        Lesson lesson = new Lesson();

        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        Rating rating = new Rating(
            1L,
            user,
            course,
            lesson,
            5,
            "Excellent course!",
            createdAt,
            updatedAt
        );

        assertEquals(1L, rating.getId());
        assertSame(user, rating.getUser());
        assertSame(course, rating.getCourse());
        assertSame(lesson, rating.getLesson());
        assertEquals(5, rating.getRating());
        assertEquals("Excellent course!", rating.getReview());
        assertEquals(createdAt, rating.getCreatedAt());
        assertEquals(updatedAt, rating.getUpdatedAt());
    }

    @Test
    void rating_ShouldCreateRatingUsingBuilder() {
        User user = new User();
        Course course = new Course();

        Rating rating = Rating.builder()
            .id(1L)
            .user(user)
            .course(course)
            .rating(4)
            .review("Very good course!")
            .build();

        assertEquals(1L, rating.getId());
        assertSame(user, rating.getUser());
        assertSame(course, rating.getCourse());
        assertNull(rating.getLesson());
        assertEquals(4, rating.getRating());
        assertEquals("Very good course!", rating.getReview());
        assertNull(rating.getCreatedAt());
        assertNull(rating.getUpdatedAt());
    }

    @Test
    void rating_ShouldCreateLessonRatingUsingBuilder() {
        User user = new User();
        Lesson lesson = new Lesson();

        Rating rating = Rating.builder()
            .user(user)
            .lesson(lesson)
            .rating(5)
            .review("Excellent lesson!")
            .build();

        assertSame(user, rating.getUser());
        assertSame(lesson, rating.getLesson());
        assertNull(rating.getCourse());
        assertEquals(5, rating.getRating());
        assertEquals("Excellent lesson!", rating.getReview());
    }

    @Test
    void rating_ShouldUpdateRatingFields() {
        User user = new User();
        Course course = new Course();
        Lesson lesson = new Lesson();

        Rating rating = new Rating();

        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        rating.setId(1L);
        rating.setUser(user);
        rating.setCourse(course);
        rating.setLesson(lesson);
        rating.setRating(3);
        rating.setReview("Updated review");
        rating.setCreatedAt(createdAt);
        rating.setUpdatedAt(updatedAt);

        assertEquals(1L, rating.getId());
        assertSame(user, rating.getUser());
        assertSame(course, rating.getCourse());
        assertSame(lesson, rating.getLesson());
        assertEquals(3, rating.getRating());
        assertEquals("Updated review", rating.getReview());
        assertEquals(createdAt, rating.getCreatedAt());
        assertEquals(updatedAt, rating.getUpdatedAt());
    }

    @Test
    void rating_ShouldAllowOptionalFieldsToBeNull() {
        Rating rating = Rating.builder()
            .user(new User())
            .rating(5)
            .build();

        assertNotNull(rating);
        assertNull(rating.getCourse());
        assertNull(rating.getLesson());
        assertNull(rating.getReview());
    }

    @Test
    void rating_ShouldGenerateEqualsAndHashCode() {
        User user = new User();
        Course course = new Course();

        Rating first = Rating.builder()
            .id(1L)
            .user(user)
            .course(course)
            .rating(5)
            .review("Great!")
            .build();

        Rating second = Rating.builder()
            .id(1L)
            .user(user)
            .course(course)
            .rating(5)
            .review("Great!")
            .build();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
