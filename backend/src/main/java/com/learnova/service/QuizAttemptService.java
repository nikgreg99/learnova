package com.learnova.service;

import com.learnova.config.security.SecurityUtils;
import com.learnova.dto.quiz.QuestionAnswerSubmission;
import com.learnova.dto.quiz.QuizAttemptResponse;
import com.learnova.dto.quiz.QuizAttemptSubmitRequest;
import com.learnova.dto.quiz.QuizTakeResponse;
import com.learnova.exception.InvalidQuizStateException;
import com.learnova.exception.KeycloakUserNotFoundException;
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
import com.learnova.model.QuizAttemptAnswer;
import com.learnova.model.QuizAttemptStatus;
import com.learnova.model.User;
import com.learnova.repository.EnrollmentRepository;
import com.learnova.repository.QuestionRepository;
import com.learnova.repository.QuizAttemptRepository;
import com.learnova.repository.QuizRepository;
import com.learnova.repository.UserRepository;
import com.learnova.service.security.CourseSecurityService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuestionRepository questionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseSecurityService courseSecurityService;
    private final QuizMapper quizMapper;

    @Transactional(readOnly = true)
    public QuizTakeResponse getQuizForTaking(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));

        if (!Boolean.TRUE.equals(quiz.getIsActive())) {
            throw new InvalidQuizStateException("Quiz is currently not active");
        }

        Course course = quiz.getCourse();
        if (course.getStatus() != CourseStatus.PUBLISHED && !courseSecurityService.isOwner(course.getId())) {
            throw new InvalidQuizStateException("Cannot take quiz for an unpublished course");
        }

        User student = getCurrentUser();
        verifyActiveEnrollment(student.getId(), course.getId());

        return quizMapper.toTakeResponse(quiz);
    }

    public QuizAttemptResponse startAttempt(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));

        if (!Boolean.TRUE.equals(quiz.getIsActive())) {
            throw new InvalidQuizStateException("Quiz is currently not active");
        }

        Course course = quiz.getCourse();
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new InvalidQuizStateException("Cannot take quiz for an unpublished course");
        }

        User student = getCurrentUser();
        verifyActiveEnrollment(student.getId(), course.getId());

        if (quizAttemptRepository.existsByQuizIdAndUserIdAndStatus(quizId, student.getId(), QuizAttemptStatus.IN_PROGRESS)) {
            throw new QuizConflictException("An attempt is already in progress for this quiz");
        }

        int existingAttempts = quizAttemptRepository.countByQuizIdAndUserId(quizId, student.getId());
        if (quiz.getMaxAttempts() != null && existingAttempts >= quiz.getMaxAttempts()) {
            throw new InvalidQuizStateException("Maximum attempt limit reached for this quiz");
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setUser(student);
        attempt.setAttemptNumber(existingAttempts + 1);
        attempt.setStatus(QuizAttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(LocalDateTime.now());

        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);
        return quizMapper.toAttemptResponse(savedAttempt);
    }

    public QuizAttemptResponse submitAttempt(Long quizId, Long attemptId, QuizAttemptSubmitRequest request) {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new QuizAttemptNotFoundException(attemptId));

        if (!attempt.getQuiz().getId().equals(quizId)) {
            throw new QuizAttemptNotFoundException(attemptId);
        }

        User student = getCurrentUser();
        if (!attempt.getUser().getId().equals(student.getId())) {
            throw new AccessDeniedException("You can only submit your own quiz attempts");
        }

        if (attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS) {
            throw new InvalidQuizStateException("Attempt is already submitted or timed out");
        }

        Quiz quiz = attempt.getQuiz();
        boolean isTimedOut = false;
        if (quiz.getTimeLimitMinutes() != null) {
            LocalDateTime deadline = attempt.getStartedAt().plusSeconds(quiz.getTimeLimitMinutes() * 60L + 30L);
            if (LocalDateTime.now().isAfter(deadline)) {
                isTimedOut = true;
            }
        }

        List<Question> questions = questionRepository.findByQuizIdOrderByPositionAsc(quizId);
        Set<Long> quizQuestionIds = questions.stream().map(Question::getId).collect(Collectors.toSet());

        Map<Long, List<Long>> submissionMap = new HashMap<>();
        if (request != null && request.answers() != null) {
            for (QuestionAnswerSubmission answer : request.answers()) {
                if (!quizQuestionIds.contains(answer.questionId())) {
                    throw new InvalidQuizStateException("Question " + answer.questionId() + " does not belong to quiz " + quizId);
                }
                submissionMap.put(answer.questionId(), answer.selectedOptionIds() != null ? answer.selectedOptionIds() : List.of());
            }
        }

        int totalPoints = 0;
        int earnedPoints = 0;
        List<QuizAttemptAnswer> attemptAnswers = new ArrayList<>();

        for (Question question : questions) {
            totalPoints += question.getPoints();

            List<Long> selectedOptionIds = submissionMap.getOrDefault(question.getId(), List.of());
            Map<Long, AnswerOption> optionMap = question.getOptions().stream()
                    .collect(Collectors.toMap(AnswerOption::getId, o -> o));

            Set<Long> correctOptionIds = question.getOptions().stream()
                    .filter(AnswerOption::getIsCorrect)
                    .map(AnswerOption::getId)
                    .collect(Collectors.toSet());

            if ((question.getType() == QuestionType.SINGLE_CHOICE || question.getType() == QuestionType.TRUE_FALSE)
                    && selectedOptionIds.size() > 1) {
                throw new InvalidQuizStateException("Question " + question.getId() + " allows at most one selected option");
            }

            for (Long selectedOptionId : selectedOptionIds) {
                AnswerOption option = optionMap.get(selectedOptionId);
                if (option == null) {
                    throw new InvalidQuizStateException("Option " + selectedOptionId + " does not belong to question " + question.getId());
                }

                QuizAttemptAnswer answer = new QuizAttemptAnswer();
                answer.setAttempt(attempt);
                answer.setQuestion(question);
                answer.setSelectedOption(option);
                attemptAnswers.add(answer);
            }

            boolean isCorrect = false;
            if (question.getType() == QuestionType.SINGLE_CHOICE || question.getType() == QuestionType.TRUE_FALSE) {
                if (selectedOptionIds.size() == 1 && correctOptionIds.contains(selectedOptionIds.getFirst())) {
                    isCorrect = true;
                }
            } else if (question.getType() == QuestionType.MULTIPLE_CHOICE) {
                Set<Long> selectedSet = new HashSet<>(selectedOptionIds);
                if (!selectedSet.isEmpty() && selectedSet.equals(correctOptionIds)) {
                    isCorrect = true;
                }
            }

            if (isCorrect && !isTimedOut) {
                earnedPoints += question.getPoints();
            }
        }

        attempt.getAnswers().clear();
        attempt.getAnswers().addAll(attemptAnswers);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setEarnedPoints(isTimedOut ? 0 : earnedPoints);
        attempt.setTotalPoints(totalPoints);

        BigDecimal score = BigDecimal.ZERO;
        if (!isTimedOut && totalPoints > 0) {
            score = BigDecimal.valueOf((double) earnedPoints * 100.0 / totalPoints).setScale(2, RoundingMode.HALF_UP);
        }
        attempt.setScore(score);

        boolean passed = !isTimedOut && (totalPoints == 0 || score.compareTo(BigDecimal.valueOf(quiz.getPassingScore())) >= 0);
        attempt.setPassed(passed);
        attempt.setStatus(isTimedOut ? QuizAttemptStatus.TIMED_OUT : QuizAttemptStatus.COMPLETED);

        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);
        return quizMapper.toAttemptResponse(savedAttempt);
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptResponse> getStudentAttempts(Long quizId) {
        if (!quizRepository.existsById(quizId)) {
            throw new QuizNotFoundException(quizId);
        }

        User student = getCurrentUser();
        return quizAttemptRepository.findByQuizIdAndUserIdOrderByAttemptNumberDesc(quizId, student.getId()).stream()
                .map(quizMapper::toAttemptResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizAttemptResponse getAttemptById(Long quizId, Long attemptId) {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new QuizAttemptNotFoundException(attemptId));

        if (!attempt.getQuiz().getId().equals(quizId)) {
            throw new QuizAttemptNotFoundException(attemptId);
        }

        User currentUser = getCurrentUser();
        boolean isOwnerOfAttempt = attempt.getUser().getId().equals(currentUser.getId());
        boolean isCourseStaff = courseSecurityService.isOwner(attempt.getQuiz().getCourse().getId());

        if (!isOwnerOfAttempt && !isCourseStaff) {
            throw new AccessDeniedException("You do not have permission to view this quiz attempt");
        }

        return quizMapper.toAttemptResponse(attempt);
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptResponse> getAllAttemptsForQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));

        if (!courseSecurityService.isOwner(quiz.getCourse().getId())) {
            throw new AccessDeniedException("You do not have permission to view all attempts for this quiz");
        }

        return quizAttemptRepository.findByQuizIdOrderByCompletedAtDesc(quizId).stream()
                .map(quizMapper::toAttemptResponse)
                .toList();
    }

    private void verifyActiveEnrollment(Long studentId, Long courseId) {
        if (courseSecurityService.isOwner(courseId)) {
            return;
        }

        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                studentId,
                courseId,
                EnrollmentStatus.ACTIVE
        );

        if (!enrolled) {
            throw new AccessDeniedException("You must have an active enrollment in this course to take the quiz");
        }
    }

    private User getCurrentUser() {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new KeycloakUserNotFoundException(keycloakId));
    }
}
