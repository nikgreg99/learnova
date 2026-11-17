package com.learnova.mapper;

import com.learnova.dto.quiz.AnswerOptionRequest;
import com.learnova.dto.quiz.AnswerOptionResponse;
import com.learnova.dto.quiz.AnswerOptionTakeResponse;
import com.learnova.dto.quiz.QuestionAnswerSubmission;
import com.learnova.dto.quiz.QuestionRequest;
import com.learnova.dto.quiz.QuestionResponse;
import com.learnova.dto.quiz.QuestionTakeResponse;
import com.learnova.dto.quiz.QuizAttemptResponse;
import com.learnova.dto.quiz.QuizPatchRequest;
import com.learnova.dto.quiz.QuizRequest;
import com.learnova.dto.quiz.QuizResponse;
import com.learnova.dto.quiz.QuizTakeResponse;
import com.learnova.model.AnswerOption;
import com.learnova.model.Question;
import com.learnova.model.Quiz;
import com.learnova.model.QuizAttempt;
import com.learnova.model.QuizAttemptAnswer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QuizMapper {

    Quiz toEntity(QuizRequest request);

    @Mapping(source = "course.id", target = "courseId")
    QuizResponse toResponse(Quiz quiz);

    @Mapping(source = "course.id", target = "courseId")
    QuizTakeResponse toTakeResponse(Quiz quiz);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void patchQuiz(QuizPatchRequest request, @MappingTarget Quiz quiz);

    Question toEntity(QuestionRequest request);

    @Mapping(source = "quiz.id", target = "quizId")
    QuestionResponse toQuestionResponse(Question question);

    @Mapping(source = "quiz.id", target = "quizId")
    QuestionTakeResponse toQuestionTakeResponse(Question question);

    AnswerOption toEntity(AnswerOptionRequest request);

    AnswerOptionResponse toOptionResponse(AnswerOption option);

    AnswerOptionTakeResponse toOptionTakeResponse(AnswerOption option);

    @Mapping(source = "quiz.id", target = "quizId")
    @Mapping(source = "quiz.title", target = "quizTitle")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "answers", expression = "java(mapAttemptAnswers(attempt.getAnswers()))")
    QuizAttemptResponse toAttemptResponse(QuizAttempt attempt);

    default List<QuestionAnswerSubmission> mapAttemptAnswers(List<QuizAttemptAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Long>> grouped = new LinkedHashMap<>();
        for (QuizAttemptAnswer ans : answers) {
            if (ans.getQuestion() != null && ans.getSelectedOption() != null) {
                grouped.computeIfAbsent(ans.getQuestion().getId(), k -> new ArrayList<>())
                        .add(ans.getSelectedOption().getId());
            }
        }
        List<QuestionAnswerSubmission> result = new ArrayList<>();
        for (Map.Entry<Long, List<Long>> entry : grouped.entrySet()) {
            result.add(new QuestionAnswerSubmission(entry.getKey(), entry.getValue()));
        }
        return result;
    }
}
