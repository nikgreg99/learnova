package com.learnova.repository;

import com.learnova.model.Lesson;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdOrderByPositionAsc(Long courseId);

    Optional<Lesson> findByIdAndCourseId(Long id, Long courseId);

    boolean existsByCourseIdAndPosition(Long courseId, Integer position);

    boolean existsByCourseIdAndPositionAndIdNot(Long courseId, Integer position, Long id);
}
