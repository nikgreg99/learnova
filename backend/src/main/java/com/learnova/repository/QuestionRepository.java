package com.learnova.repository;

import com.learnova.model.Question;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuizIdOrderByPositionAsc(Long quizId);

    Optional<Question> findByIdAndQuizId(Long id, Long quizId);

    boolean existsByQuizIdAndPosition(Long quizId, Integer position);

    boolean existsByQuizIdAndPositionAndIdNot(Long quizId, Integer position, Long id);
}
