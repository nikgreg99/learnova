package com.learnova.repository;

import com.learnova.model.AnswerOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {

    List<AnswerOption> findByQuestionIdOrderByPositionAsc(Long questionId);

    List<AnswerOption> findByQuestionIdAndIsCorrectTrue(Long questionId);
}
