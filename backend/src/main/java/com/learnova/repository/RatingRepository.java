package com.learnova.repository;

import com.learnova.model.Rating;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByCourseId(Long courseId);

    List<Rating> findByLessonId(Long lessonId);

    Optional<Rating> findByUserIdAndCourseId(Long userId, Long courseId);

    Optional<Rating> findByUserIdAndLessonId(Long userId, Long lessonId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndLessonId(Long userId, Long lessonId);

    long countByCourseId(Long courseId);

    long countByLessonId(Long lessonId);

    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.course.id = :courseId")
    Optional<Double> calculateAverageRatingByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.lesson.id = :lessonId")
    Optional<Double> calculateAverageRatingByLessonId(@Param("lessonId") Long lessonId);
}
