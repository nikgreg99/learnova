package com.learnova.repository;

import com.learnova.model.QuizAttempt;
import com.learnova.model.QuizAttemptStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByQuizIdAndUserIdOrderByAttemptNumberDesc(Long quizId, Long userId);

    Optional<QuizAttempt> findByIdAndUserId(Long id, Long userId);

    int countByQuizIdAndUserId(Long quizId, Long userId);

    boolean existsByQuizIdAndUserIdAndStatus(Long quizId, Long userId, QuizAttemptStatus status);

    List<QuizAttempt> findByQuizIdOrderByCompletedAtDesc(Long quizId);
}
