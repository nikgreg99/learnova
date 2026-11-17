package com.learnova.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnova.config.security.SecurityUtils;
import com.learnova.dto.quiz.QuestionAnswerSubmission;
import com.learnova.dto.quiz.QuizAttemptResponse;
import com.learnova.dto.quiz.QuizAttemptSubmitRequest;
import com.learnova.dto.quiz.QuizTakeResponse;
import com.learnova.exception.InvalidQuizStateException;
import com.learnova.exception.QuizAttemptNotFoundException;
import com.learnova.exception.QuizConflictException;
import com.learnova.exception.QuizNotFoundException;
import com.learnova.mapper.QuizMapper;
import com.learnova.model.AnswerOption;
import com.learnova.model.Course;
import com.learnova.model.CourseStatus;
import com.learnova.model.EnrollmentStatus;
import com.learnova.model.Question;
import com.learnova.model.QuestionType;
import com.learnova.model.Quiz;
import com.learnova.model.QuizAttempt;
import com.learnova.model.QuizAttemptStatus;
import com.learnova.model.User;
import com.learnova.repository.EnrollmentRepository;
import com.learnova.repository.QuestionRepository;
import com.learnova.repository.QuizAttemptRepository;
import com.learnova.repository.QuizRepository;
import com.learnova.repository.UserRepository;
import com.learnova.service.security.CourseSecurityService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

    private static final String KEYCLOAK_ID = "student-kc-id";

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseSecurityService courseSecurityService;

    @Mock
    private QuizMapper quizMapper;

    @InjectMocks
    private QuizAttemptService quizAttemptService;

    private MockedStatic<SecurityUtils> mockedSecurityUtils;
    private User student;
    private Course course;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        mockedSecurityUtils = mockStatic(SecurityUtils.class);
        mockedSecurityUtils.when(SecurityUtils::getCurrentKeycloakId).thenReturn(KEYCLOAK_ID);

        student = User.builder().id(50L).keycloakId(KEYCLOAK_ID).username("student1").build();
        course = new Course();
        course.setId(1L);
        course.setName("Course 1");
        course.setStatus(CourseStatus.PUBLISHED);

        quiz = Quiz.builder()
                .id(10L)
                .course(course)
                .title("Quiz 1")
                .passingScore(70)
                .maxAttempts(2)
                .timeLimitMinutes(15)
                .isActive(true)
                .questions(new ArrayList<>())
                .build();
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtils.close();
    }

    @Test
    void getQuizForTaking_Success() {
        QuizTakeResponse takeResponse = new QuizTakeResponse(10L, 1L, "Quiz 1", "Desc", 70, 2, 15, List.of());

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(courseSecurityService.isOwner(1L)).thenReturn(false);
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(50L, 1L, EnrollmentStatus.ACTIVE)).thenReturn(true);
        when(quizMapper.toTakeResponse(quiz)).thenReturn(takeResponse);

        QuizTakeResponse result = quizAttemptService.getQuizForTaking(10L);

        assertNotNull(result);
        assertEquals(10L, result.id());
    }

    @Test
    void getQuizForTaking_InactiveQuiz_ThrowsException() {
        quiz.setIsActive(false);
        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));

        assertThrows(InvalidQuizStateException.class, () -> quizAttemptService.getQuizForTaking(10L));
    }

    @Test
    void getQuizForTaking_UnpublishedCourse_ThrowsException() {
        course.setStatus(CourseStatus.DRAFT);
        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(courseSecurityService.isOwner(1L)).thenReturn(false);

        assertThrows(InvalidQuizStateException.class, () -> quizAttemptService.getQuizForTaking(10L));
    }

    @Test
    void getQuizForTaking_NotEnrolled_ThrowsException() {
        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(courseSecurityService.isOwner(1L)).thenReturn(false);
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(50L, 1L, EnrollmentStatus.ACTIVE)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> quizAttemptService.getQuizForTaking(10L));
    }

    @Test
    void startAttempt_Success() {
        QuizAttempt savedAttempt = QuizAttempt.builder()
                .id(100L)
                .quiz(quiz)
                .user(student)
                .attemptNumber(1)
                .status(QuizAttemptStatus.IN_PROGRESS)
                .build();
        QuizAttemptResponse response = new QuizAttemptResponse(100L, 10L, "Quiz 1", 50L, 1, null, null, null, null, QuizAttemptStatus.IN_PROGRESS, null, null, List.of());

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(courseSecurityService.isOwner(1L)).thenReturn(false);
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(50L, 1L, EnrollmentStatus.ACTIVE)).thenReturn(true);
        when(quizAttemptRepository.existsByQuizIdAndUserIdAndStatus(10L, 50L, QuizAttemptStatus.IN_PROGRESS)).thenReturn(false);
        when(quizAttemptRepository.countByQuizIdAndUserId(10L, 50L)).thenReturn(0);
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenReturn(savedAttempt);
        when(quizMapper.toAttemptResponse(savedAttempt)).thenReturn(response);

        QuizAttemptResponse result = quizAttemptService.startAttempt(10L);

        assertNotNull(result);
        assertEquals(100L, result.id());
        verify(quizAttemptRepository).save(any(QuizAttempt.class));
    }

    @Test
    void startAttempt_AlreadyInProgress_ThrowsException() {
        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(courseSecurityService.isOwner(1L)).thenReturn(false);
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(50L, 1L, EnrollmentStatus.ACTIVE)).thenReturn(true);
        when(quizAttemptRepository.existsByQuizIdAndUserIdAndStatus(10L, 50L, QuizAttemptStatus.IN_PROGRESS)).thenReturn(true);

        assertThrows(QuizConflictException.class, () -> quizAttemptService.startAttempt(10L));
    }

    @Test
    void startAttempt_MaxAttemptsExceeded_ThrowsException() {
        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(courseSecurityService.isOwner(1L)).thenReturn(false);
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(50L, 1L, EnrollmentStatus.ACTIVE)).thenReturn(true);
        when(quizAttemptRepository.existsByQuizIdAndUserIdAndStatus(10L, 50L, QuizAttemptStatus.IN_PROGRESS)).thenReturn(false);
        when(quizAttemptRepository.countByQuizIdAndUserId(10L, 50L)).thenReturn(2);

        assertThrows(InvalidQuizStateException.class, () -> quizAttemptService.startAttempt(10L));
    }

    @Test
    void submitAttempt_SingleChoice_Correct_Passed() {
        QuizAttempt attempt = QuizAttempt.builder()
                .id(100L)
                .quiz(quiz)
                .user(student)
                .attemptNumber(1)
                .status(QuizAttemptStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .answers(new ArrayList<>())
                .build();

        AnswerOption opt1 = AnswerOption.builder().id(201L).text("Correct").isCorrect(true).position(1).build();
        AnswerOption opt2 = AnswerOption.builder().id(202L).text("Wrong").isCorrect(false).position(2).build();
        Question q1 = Question.builder().id(301L).quiz(quiz).type(QuestionType.SINGLE_CHOICE).points(10).options(List.of(opt1, opt2)).build();

        QuizAttemptSubmitRequest submitRequest = new QuizAttemptSubmitRequest(List.of(
                new QuestionAnswerSubmission(301L, List.of(201L))
        ));

        QuizAttemptResponse response = new QuizAttemptResponse(100L, 10L, "Quiz 1", 50L, 1, BigDecimal.valueOf(100.00), 10, 10, true, QuizAttemptStatus.COMPLETED, null, null, List.of());

        when(quizAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(questionRepository.findByQuizIdOrderByPositionAsc(10L)).thenReturn(List.of(q1));
        when(quizAttemptRepository.save(attempt)).thenReturn(attempt);
        when(quizMapper.toAttemptResponse(attempt)).thenReturn(response);

        QuizAttemptResponse result = quizAttemptService.submitAttempt(10L, 100L, submitRequest);

        assertNotNull(result);
        assertEquals(QuizAttemptStatus.COMPLETED, attempt.getStatus());
        assertEquals(10, attempt.getEarnedPoints());
        assertEquals(10, attempt.getTotalPoints());
        assertTrue(attempt.getPassed());
        assertEquals(BigDecimal.valueOf(100.0).setScale(2), attempt.getScore());
    }

    @Test
    void submitAttempt_MultipleChoice_ExactMatch_Passed() {
        QuizAttempt attempt = QuizAttempt.builder()
                .id(100L)
                .quiz(quiz)
                .user(student)
                .attemptNumber(1)
                .status(QuizAttemptStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .answers(new ArrayList<>())
                .build();

        AnswerOption opt1 = AnswerOption.builder().id(201L).text("A").isCorrect(true).position(1).build();
        AnswerOption opt2 = AnswerOption.builder().id(202L).text("B").isCorrect(true).position(2).build();
        AnswerOption opt3 = AnswerOption.builder().id(203L).text("C").isCorrect(false).position(3).build();
        Question q1 = Question.builder().id(301L).quiz(quiz).type(QuestionType.MULTIPLE_CHOICE).points(10).options(List.of(opt1, opt2, opt3)).build();

        QuizAttemptSubmitRequest submitRequest = new QuizAttemptSubmitRequest(List.of(
                new QuestionAnswerSubmission(301L, List.of(201L, 202L))
        ));

        when(quizAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(questionRepository.findByQuizIdOrderByPositionAsc(10L)).thenReturn(List.of(q1));
        when(quizAttemptRepository.save(attempt)).thenReturn(attempt);
        when(quizMapper.toAttemptResponse(attempt)).thenReturn(new QuizAttemptResponse(100L, 10L, "Quiz 1", 50L, 1, BigDecimal.valueOf(100.00), 10, 10, true, QuizAttemptStatus.COMPLETED, null, null, List.of()));

        quizAttemptService.submitAttempt(10L, 100L, submitRequest);

        assertEquals(10, attempt.getEarnedPoints());
        assertTrue(attempt.getPassed());
    }

    @Test
    void submitAttempt_MultipleChoice_PartialMatch_FailsQuestion() {
        QuizAttempt attempt = QuizAttempt.builder()
                .id(100L)
                .quiz(quiz)
                .user(student)
                .attemptNumber(1)
                .status(QuizAttemptStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .answers(new ArrayList<>())
                .build();

        AnswerOption opt1 = AnswerOption.builder().id(201L).text("A").isCorrect(true).position(1).build();
        AnswerOption opt2 = AnswerOption.builder().id(202L).text("B").isCorrect(true).position(2).build();
        Question q1 = Question.builder().id(301L).quiz(quiz).type(QuestionType.MULTIPLE_CHOICE).points(10).options(List.of(opt1, opt2)).build();

        // Student only selected 201L, missed 202L
        QuizAttemptSubmitRequest submitRequest = new QuizAttemptSubmitRequest(List.of(
                new QuestionAnswerSubmission(301L, List.of(201L))
        ));

        when(quizAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(questionRepository.findByQuizIdOrderByPositionAsc(10L)).thenReturn(List.of(q1));
        when(quizAttemptRepository.save(attempt)).thenReturn(attempt);
        when(quizMapper.toAttemptResponse(attempt)).thenReturn(new QuizAttemptResponse(100L, 10L, "Quiz 1", 50L, 1, BigDecimal.ZERO, 0, 10, false, QuizAttemptStatus.COMPLETED, null, null, List.of()));

        quizAttemptService.submitAttempt(10L, 100L, submitRequest);

        assertEquals(0, attempt.getEarnedPoints());
        assertFalse(attempt.getPassed());
    }

    @Test
    void submitAttempt_TimedOut_SetsTimedOutStatusAndZeroPoints() {
        QuizAttempt attempt = QuizAttempt.builder()
                .id(100L)
                .quiz(quiz)
                .user(student)
                .attemptNumber(1)
                .status(QuizAttemptStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now().minusHours(1)) // Started 1 hour ago (limit was 15 min)
                .answers(new ArrayList<>())
                .build();

        AnswerOption opt1 = AnswerOption.builder().id(201L).text("Correct").isCorrect(true).position(1).build();
        Question q1 = Question.builder().id(301L).quiz(quiz).type(QuestionType.SINGLE_CHOICE).points(10).options(List.of(opt1)).build();

        QuizAttemptSubmitRequest submitRequest = new QuizAttemptSubmitRequest(List.of(
                new QuestionAnswerSubmission(301L, List.of(201L))
        ));

        when(quizAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(questionRepository.findByQuizIdOrderByPositionAsc(10L)).thenReturn(List.of(q1));
        when(quizAttemptRepository.save(attempt)).thenReturn(attempt);
        when(quizMapper.toAttemptResponse(attempt)).thenReturn(new QuizAttemptResponse(100L, 10L, "Quiz 1", 50L, 1, BigDecimal.ZERO, 0, 10, false, QuizAttemptStatus.TIMED_OUT, null, null, List.of()));

        quizAttemptService.submitAttempt(10L, 100L, submitRequest);

        assertEquals(QuizAttemptStatus.TIMED_OUT, attempt.getStatus());
        assertEquals(0, attempt.getEarnedPoints());
        assertFalse(attempt.getPassed());
    }

    @Test
    void submitAttempt_NotOwner_ThrowsAccessDenied() {
        User otherStudent = User.builder().id(99L).keycloakId("other-id").build();
        QuizAttempt attempt = QuizAttempt.builder()
                .id(100L)
                .quiz(quiz)
                .user(otherStudent)
                .status(QuizAttemptStatus.IN_PROGRESS)
                .build();

        when(quizAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(student));

        assertThrows(AccessDeniedException.class, () -> quizAttemptService.submitAttempt(10L, 100L, null));
    }

    @Test
    void submitAttempt_AlreadyCompleted_ThrowsInvalidQuizState() {
        QuizAttempt attempt = QuizAttempt.builder()
                .id(100L)
                .quiz(quiz)
                .user(student)
                .status(QuizAttemptStatus.COMPLETED)
                .build();

        when(quizAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(student));

        assertThrows(InvalidQuizStateException.class, () -> quizAttemptService.submitAttempt(10L, 100L, null));
    }

    @Test
    void getStudentAttempts_Success() {
        QuizAttempt attempt = QuizAttempt.builder().id(100L).quiz(quiz).user(student).build();
        QuizAttemptResponse response = new QuizAttemptResponse(100L, 10L, "Quiz 1", 50L, 1, null, null, null, null, QuizAttemptStatus.COMPLETED, null, null, List.of());

        when(quizRepository.existsById(10L)).thenReturn(true);
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(quizAttemptRepository.findByQuizIdAndUserIdOrderByAttemptNumberDesc(10L, 50L)).thenReturn(List.of(attempt));
        when(quizMapper.toAttemptResponse(attempt)).thenReturn(response);

        List<QuizAttemptResponse> results = quizAttemptService.getStudentAttempts(10L);

        assertEquals(1, results.size());
    }

    @Test
    void getAttemptById_Owner_Success() {
        QuizAttempt attempt = QuizAttempt.builder().id(100L).quiz(quiz).user(student).build();
        QuizAttemptResponse response = new QuizAttemptResponse(100L, 10L, "Quiz 1", 50L, 1, null, null, null, null, QuizAttemptStatus.COMPLETED, null, null, List.of());

        when(quizAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(courseSecurityService.isOwner(1L)).thenReturn(false);
        when(quizMapper.toAttemptResponse(attempt)).thenReturn(response);

        QuizAttemptResponse result = quizAttemptService.getAttemptById(10L, 100L);

        assertNotNull(result);
        assertEquals(100L, result.id());
    }

    @Test
    void getAttemptById_NotOwnerNotStaff_ThrowsAccessDenied() {
        User otherStudent = User.builder().id(99L).keycloakId("other-id").build();
        QuizAttempt attempt = QuizAttempt.builder().id(100L).quiz(quiz).user(otherStudent).build();

        when(quizAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(student));
        when(courseSecurityService.isOwner(1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> quizAttemptService.getAttemptById(10L, 100L));
    }

    @Test
    void getAllAttemptsForQuiz_Instructor_Success() {
        QuizAttempt attempt = QuizAttempt.builder().id(100L).quiz(quiz).user(student).build();
        QuizAttemptResponse response = new QuizAttemptResponse(100L, 10L, "Quiz 1", 50L, 1, null, null, null, null, QuizAttemptStatus.COMPLETED, null, null, List.of());

        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(courseSecurityService.isOwner(1L)).thenReturn(true);
        when(quizAttemptRepository.findByQuizIdOrderByCompletedAtDesc(10L)).thenReturn(List.of(attempt));
        when(quizMapper.toAttemptResponse(attempt)).thenReturn(response);

        List<QuizAttemptResponse> results = quizAttemptService.getAllAttemptsForQuiz(10L);

        assertEquals(1, results.size());
    }

    @Test
    void getAllAttemptsForQuiz_NotInstructor_ThrowsAccessDenied() {
        when(quizRepository.findById(10L)).thenReturn(Optional.of(quiz));
        when(courseSecurityService.isOwner(1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> quizAttemptService.getAllAttemptsForQuiz(10L));
    }
}
