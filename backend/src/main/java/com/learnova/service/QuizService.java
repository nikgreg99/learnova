package com.learnova.service;

import com.learnova.dto.quiz.AnswerOptionRequest;
import com.learnova.dto.quiz.QuestionRequest;
import com.learnova.dto.quiz.QuestionResponse;
import com.learnova.dto.quiz.QuizPatchRequest;
import com.learnova.dto.quiz.QuizRequest;
import com.learnova.dto.quiz.QuizResponse;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.exception.InvalidQuizStateException;
import com.learnova.exception.QuestionNotFoundException;
import com.learnova.exception.QuizConflictException;
import com.learnova.exception.QuizNotFoundException;
import com.learnova.mapper.QuizMapper;
import com.learnova.model.AnswerOption;
import com.learnova.model.Course;
import com.learnova.model.Question;
import com.learnova.model.QuestionType;
import com.learnova.model.Quiz;
import com.learnova.repository.CourseRepository;
import com.learnova.repository.QuestionRepository;
import com.learnova.repository.QuizRepository;
import com.learnova.service.security.CourseSecurityService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final CourseRepository courseRepository;
    private final QuizMapper quizMapper;
    private final CourseSecurityService courseSecurityService;

    public QuizResponse createQuiz(Long courseId, QuizRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        Quiz quiz = quizMapper.toEntity(request);
        quiz.setCourse(course);
        Quiz savedQuiz = quizRepository.save(quiz);
        return quizMapper.toResponse(savedQuiz);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> getCourseQuizzes(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }

        List<Quiz> quizzes;
        if (courseSecurityService.isOwner(courseId)) {
            quizzes = quizRepository.findByCourseId(courseId);
        } else {
            quizzes = quizRepository.findByCourseIdAndIsActiveTrue(courseId);
        }

        return quizzes.stream()
                .map(quizMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuizById(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));
        return quizMapper.toResponse(quiz);
    }

    public QuizResponse updateQuiz(Long quizId, QuizRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));

        quiz.setTitle(request.title());
        quiz.setDescription(request.description());
        quiz.setPassingScore(request.passingScore());
        quiz.setMaxAttempts(request.maxAttempts());
        quiz.setTimeLimitMinutes(request.timeLimitMinutes());
        if (request.isActive() != null) {
            quiz.setIsActive(request.isActive());
        }

        Quiz savedQuiz = quizRepository.save(quiz);
        return quizMapper.toResponse(savedQuiz);
    }

    public QuizResponse patchQuiz(Long quizId, QuizPatchRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));

        quizMapper.patchQuiz(request, quiz);
        Quiz savedQuiz = quizRepository.save(quiz);
        return quizMapper.toResponse(savedQuiz);
    }

    public void deleteQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));
        quizRepository.delete(quiz);
    }

    public QuestionResponse addQuestion(Long quizId, QuestionRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));

        if (questionRepository.existsByQuizIdAndPosition(quizId, request.position())) {
            throw new QuizConflictException("A question with position " + request.position() + " already exists in this quiz");
        }

        validateQuestionOptions(request);

        Question question = quizMapper.toEntity(request);
        question.setQuiz(quiz);

        if (question.getOptions() != null) {
            for (AnswerOption option : question.getOptions()) {
                option.setQuestion(question);
            }
        }

        Question savedQuestion = questionRepository.save(question);
        return quizMapper.toQuestionResponse(savedQuestion);
    }

    public QuestionResponse updateQuestion(Long quizId, Long questionId, QuestionRequest request) {
        if (!quizRepository.existsById(quizId)) {
            throw new QuizNotFoundException(quizId);
        }

        Question question = questionRepository.findByIdAndQuizId(questionId, quizId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        if (questionRepository.existsByQuizIdAndPositionAndIdNot(quizId, request.position(), questionId)) {
            throw new QuizConflictException("A question with position " + request.position() + " already exists in this quiz");
        }

        validateQuestionOptions(request);

        question.setText(request.text());
        question.setType(request.type());
        question.setPoints(request.points());
        question.setPosition(request.position());

        question.getOptions().clear();
        if (request.options() != null) {
            for (AnswerOptionRequest optReq : request.options()) {
                AnswerOption opt = quizMapper.toEntity(optReq);
                opt.setQuestion(question);
                question.getOptions().add(opt);
            }
        }

        Question savedQuestion = questionRepository.save(question);
        return quizMapper.toQuestionResponse(savedQuestion);
    }

    public void deleteQuestion(Long quizId, Long questionId) {
        if (!quizRepository.existsById(quizId)) {
            throw new QuizNotFoundException(quizId);
        }

        Question question = questionRepository.findByIdAndQuizId(questionId, quizId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        questionRepository.delete(question);
    }

    private void validateQuestionOptions(QuestionRequest request) {
        List<AnswerOptionRequest> options = request.options();
        if (options == null || options.isEmpty()) {
            throw new InvalidQuizStateException("Question must have at least two answer options");
        }

        long distinctPositions = options.stream().map(AnswerOptionRequest::position).distinct().count();
        if (distinctPositions != options.size()) {
            throw new InvalidQuizStateException("Answer options must have unique positions");
        }

        if (request.type() == QuestionType.SINGLE_CHOICE) {
            if (options.size() < 2) {
                throw new InvalidQuizStateException("Single choice questions must have at least two options");
            }
            long correctCount = options.stream().filter(opt -> Boolean.TRUE.equals(opt.isCorrect())).count();
            if (correctCount != 1) {
                throw new InvalidQuizStateException("Single choice questions must have exactly one correct option");
            }
        } else if (request.type() == QuestionType.MULTIPLE_CHOICE) {
            if (options.size() < 2) {
                throw new InvalidQuizStateException("Multiple choice questions must have at least two options");
            }
            long correctCount = options.stream().filter(opt -> Boolean.TRUE.equals(opt.isCorrect())).count();
            if (correctCount < 1) {
                throw new InvalidQuizStateException("Multiple choice questions must have at least one correct option");
            }
        } else if (request.type() == QuestionType.TRUE_FALSE) {
            if (options.size() != 2) {
                throw new InvalidQuizStateException("True/False questions must have exactly two options");
            }
            long correctCount = options.stream().filter(opt -> Boolean.TRUE.equals(opt.isCorrect())).count();
            if (correctCount != 1) {
                throw new InvalidQuizStateException("True/False questions must have exactly one correct option");
            }
        }
    }
}
