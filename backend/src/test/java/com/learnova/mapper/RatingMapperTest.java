package com.learnova.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnova.dto.rating.RatingResponse;
import com.learnova.model.Course;
import com.learnova.model.Lesson;
import com.learnova.model.Rating;
import com.learnova.model.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RatingMapperTest {

    private final RatingMapper ratingMapper = Mappers.getMapper(RatingMapper.class);

    @Test
    void toResponse_shouldMapCourseRatingSuccessfully() {
        User user = User.builder().id(1L).username("testuser").build();
        Course course = new Course();
        course.setId(10L);

        LocalDateTime now = LocalDateTime.now();
        Rating rating = Rating.builder()
                .id(100L)
                .user(user)
                .course(course)
                .rating(5)
                .review("Great course!")
                .createdAt(now)
                .updatedAt(now)
                .build();

        RatingResponse response = ratingMapper.toResponse(rating);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.courseId()).isEqualTo(10L);
        assertThat(response.lessonId()).isNull();
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.review()).isEqualTo("Great course!");
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }

    @Test
    void toResponse_shouldMapLessonRatingSuccessfully() {
        User user = User.builder().id(2L).username("student1").build();
        Lesson lesson = new Lesson();
        lesson.setId(25L);

        LocalDateTime now = LocalDateTime.now();
        Rating rating = Rating.builder()
                .id(101L)
                .user(user)
                .lesson(lesson)
                .rating(4)
                .review("Helpful lesson.")
                .createdAt(now)
                .updatedAt(now)
                .build();

        RatingResponse response = ratingMapper.toResponse(rating);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.username()).isEqualTo("student1");
        assertThat(response.courseId()).isNull();
        assertThat(response.lessonId()).isEqualTo(25L);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.review()).isEqualTo("Helpful lesson.");
    }

    @Test
    void toResponse_shouldReturnNullWhenRatingIsNull() {
        assertThat(ratingMapper.toResponse(null)).isNull();
    }
}
