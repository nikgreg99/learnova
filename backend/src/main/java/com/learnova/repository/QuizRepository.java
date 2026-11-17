package com.learnova.repository;

import com.learnova.model.Quiz;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByCourseId(Long courseId);

    List<Quiz> findByCourseIdAndIsActiveTrue(Long courseId);

    Optional<Quiz> findByIdAndCourseId(Long id, Long courseId);

    boolean existsByIdAndCourseId(Long id, Long courseId);
}
