package com.learnova.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private QuizMapper quizMapper;

    @Mock
    private CourseSecurityService courseSecurityService;

    @InjectMocks
    private QuizService quizService;

    private Course course;
    private Quiz quiz;
    private QuizRequest quizRequest;
    private QuizResponse quizResponse;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
        course.setName("Java Masterclass");

        quiz = Quiz.builder()
                .id(10L)
                .course(course)
                .title("Java Basics Quiz")
                .description("Test your basics")
                .passingScore(75)
                .maxAttempts(3)
                .timeLimitMinutes(30)
                .isActive(true)
                .questions(new ArrayList<>())
                .build();

        quizRequest = new QuizRequest("Java Basics Quiz", "Test your basics", 75, 3, 30, true);
        quizResponse = new QuizResponse(10L, 1L, "Java Basics Quiz", "Test your basics", 75, 3, 30, true, List.of(), null, null);
    }

    @Test
    void createQuiz_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(quizMapper.toEntity(quizRequest)).thenReturn(quiz);
        when(quizRepository.save(quiz)).thenReturn(quiz);
        when(quizMapper.toResponse(quiz)).thenReturn(quizResponse);

        QuizResponse result = quizService.createQuiz(1L, quizRequest);

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("Java Basics Quiz", result.title());
        verify(quizRepository).save(quiz);
    }

    @Test
    void createQuiz_CourseNotFound_ThrowsException() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class, () -> quizService.createQuiz(99L, quizRequest));
    }

    @Test
    void getCourseQuizzes_InstructorOwner_ReturnsAllQuizzes() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(courseSecurityService.isOwner(1L)).thenReturn(true);
        when(quizRepository.findByCourseId(1L)).thenReturn(List.of(quiz));
        when(quizMapper.toResponse(quiz)).thenReturn(quizResponse);

        List<QuizResponse> results = quizService.getCourseQuizzes(1L);

        assertEquals(1, results.size());
        verify(quizRepository).findByCourseId(1L);
    }

    @Test
    void getCourseQuizzes_Student_ReturnsOnlyActiveQuizzes() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(courseSecurityService.isOwner(1L)).thenReturn(false);
        when(quizRepository.findByCourseIdAndIsActiveTrue(1L)).thenReturn(List.of(quiz));
        when(quizMapper.toResponse(quiz)).thenReturn(quizResponse);

        List<QuizResponse> results = quizService.getCourseQuizzes(1L);

        assertEquals(1, results.size());
        verify(quizRepository).findByCourseIdAndIsActiveTrue(1L);
    }

    @Test
    void getQuizById_Success() {
        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(quizMapper.toResponse(quiz)).thenReturn(quizResponse);

        QuizResponse result = quizService.getQuizById(10L);

        assertNotNull(result);
        assertEquals(10L, result.id());
    }

    @Test
    void getQuizById_NotFound_ThrowsException() {
        when(quizRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(QuizNotFoundException.class, () -> quizService.getQuizById(999L));
    }

    @Test
    void updateQuiz_Success() {
        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(quiz)).thenReturn(quiz);
        when(quizMapper.toResponse(quiz)).thenReturn(quizResponse);

        QuizResponse result = quizService.updateQuiz(10L, quizRequest);

        assertNotNull(result);
        verify(quizRepository).save(quiz);
    }

    @Test
    void patchQuiz_Success() {
        QuizPatchRequest patchRequest = new QuizPatchRequest("New Title", null, null, null, null, null);
        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(quiz)).thenReturn(quiz);
        when(quizMapper.toResponse(quiz)).thenReturn(quizResponse);

        QuizResponse result = quizService.patchQuiz(10L, patchRequest);

        assertNotNull(result);
        verify(quizMapper).patchQuiz(patchRequest, quiz);
        verify(quizRepository).save(quiz);
    }

    @Test
    void deleteQuiz_Success() {
        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));

        quizService.deleteQuiz(10L);

        verify(quizRepository).delete(quiz);
    }

    @Test
    void addQuestion_SingleChoice_Success() {
        List<AnswerOptionRequest> options = List.of(
                new AnswerOptionRequest("Option A", true, 1),
                new AnswerOptionRequest("Option B", false, 2)
        );
        QuestionRequest qRequest = new QuestionRequest("What is Java?", QuestionType.SINGLE_CHOICE, 2, 1, options);
        Question question = Question.builder().id(100L).quiz(quiz).text("What is Java?").type(QuestionType.SINGLE_CHOICE).points(2).position(1).options(new ArrayList<>()).build();
        QuestionResponse qResponse = new QuestionResponse(100L, 10L, "What is Java?", QuestionType.SINGLE_CHOICE, 2, 1, List.of());

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(questionRepository.existsByQuizIdAndPosition(10L, 1)).thenReturn(false);
        when(quizMapper.toEntity(qRequest)).thenReturn(question);
        when(questionRepository.save(question)).thenReturn(question);
        when(quizMapper.toQuestionResponse(question)).thenReturn(qResponse);

        QuestionResponse result = quizService.addQuestion(10L, qRequest);

        assertNotNull(result);
        assertEquals(100L, result.id());
        verify(questionRepository).save(question);
    }

    @Test
    void addQuestion_PositionConflict_ThrowsException() {
        List<AnswerOptionRequest> options = List.of(
                new AnswerOptionRequest("A", true, 1),
                new AnswerOptionRequest("B", false, 2)
        );
        QuestionRequest qRequest = new QuestionRequest("Prompt", QuestionType.SINGLE_CHOICE, 1, 1, options);

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(questionRepository.existsByQuizIdAndPosition(10L, 1)).thenReturn(true);

        assertThrows(QuizConflictException.class, () -> quizService.addQuestion(10L, qRequest));
    }

    @Test
    void addQuestion_SingleChoice_MultipleCorrect_ThrowsException() {
        List<AnswerOptionRequest> options = List.of(
                new AnswerOptionRequest("A", true, 1),
                new AnswerOptionRequest("B", true, 2)
        );
        QuestionRequest qRequest = new QuestionRequest("Prompt", QuestionType.SINGLE_CHOICE, 1, 1, options);

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(questionRepository.existsByQuizIdAndPosition(10L, 1)).thenReturn(false);

        assertThrows(InvalidQuizStateException.class, () -> quizService.addQuestion(10L, qRequest));
    }

    @Test
    void addQuestion_MultipleChoice_ZeroCorrect_ThrowsException() {
        List<AnswerOptionRequest> options = List.of(
                new AnswerOptionRequest("A", false, 1),
                new AnswerOptionRequest("B", false, 2)
        );
        QuestionRequest qRequest = new QuestionRequest("Prompt", QuestionType.MULTIPLE_CHOICE, 1, 1, options);

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(questionRepository.existsByQuizIdAndPosition(10L, 1)).thenReturn(false);

        assertThrows(InvalidQuizStateException.class, () -> quizService.addQuestion(10L, qRequest));
    }

    @Test
    void addQuestion_TrueFalse_NotTwoOptions_ThrowsException() {
        List<AnswerOptionRequest> options = List.of(
                new AnswerOptionRequest("True", true, 1),
                new AnswerOptionRequest("False", false, 2),
                new AnswerOptionRequest("Maybe", false, 3)
        );
        QuestionRequest qRequest = new QuestionRequest("Prompt", QuestionType.TRUE_FALSE, 1, 1, options);

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(questionRepository.existsByQuizIdAndPosition(10L, 1)).thenReturn(false);

        assertThrows(InvalidQuizStateException.class, () -> quizService.addQuestion(10L, qRequest));
    }

    @Test
    void updateQuestion_Success() {
        List<AnswerOptionRequest> options = List.of(
                new AnswerOptionRequest("True", true, 1),
                new AnswerOptionRequest("False", false, 2)
        );
        QuestionRequest qRequest = new QuestionRequest("Updated text", QuestionType.TRUE_FALSE, 1, 1, options);
        Question existing = Question.builder().id(100L).quiz(quiz).text("Old text").type(QuestionType.TRUE_FALSE).points(1).position(1).options(new ArrayList<>()).build();
        QuestionResponse qResponse = new QuestionResponse(100L, 10L, "Updated text", QuestionType.TRUE_FALSE, 1, 1, List.of());

        when(quizRepository.existsById(10L)).thenReturn(true);
        when(questionRepository.findByIdAndQuizId(100L, 10L)).thenReturn(Optional.of(existing));
        when(questionRepository.existsByQuizIdAndPositionAndIdNot(10L, 1, 100L)).thenReturn(false);
        when(quizMapper.toEntity(any(AnswerOptionRequest.class))).thenAnswer(inv -> AnswerOption.builder().build());
        when(questionRepository.save(existing)).thenReturn(existing);
        when(quizMapper.toQuestionResponse(existing)).thenReturn(qResponse);

        QuestionResponse result = quizService.updateQuestion(10L, 100L, qRequest);

        assertNotNull(result);
        assertEquals("Updated text", existing.getText());
        verify(questionRepository).save(existing);
    }

    @Test
    void deleteQuestion_Success() {
        Question existing = Question.builder().id(100L).quiz(quiz).build();

        when(quizRepository.existsById(10L)).thenReturn(true);
        when(questionRepository.findByIdAndQuizId(100L, 10L)).thenReturn(Optional.of(existing));

        quizService.deleteQuestion(10L, 100L);

        verify(questionRepository).delete(existing);
    }

    @Test
    void deleteQuestion_NotFound_ThrowsException() {
        when(quizRepository.existsById(10L)).thenReturn(true);
        when(questionRepository.findByIdAndQuizId(999L, 10L)).thenReturn(Optional.empty());

        assertThrows(QuestionNotFoundException.class, () -> quizService.deleteQuestion(10L, 999L));
    }
}
