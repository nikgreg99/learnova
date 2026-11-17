package com.learnova.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnova.config.ratelimit.RateLimitInterceptor;
import com.learnova.config.security.SecurityConfig;
import com.learnova.dto.quiz.QuestionAnswerSubmission;
import com.learnova.dto.quiz.QuizAttemptResponse;
import com.learnova.dto.quiz.QuizAttemptSubmitRequest;
import com.learnova.dto.quiz.QuizTakeResponse;
import com.learnova.model.QuizAttemptStatus;
import com.learnova.service.QuizAttemptService;
import com.learnova.service.security.QuizSecurityService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(controllers = QuizAttemptController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class QuizAttemptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private QuizAttemptService quizAttemptService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockitoBean(name = "quizSecurityService")
    private QuizSecurityService quizSecurityService;

    private static final Long QUIZ_ID = 10L;
    private static final Long ATTEMPT_ID = 100L;

    private QuizTakeResponse quizTakeResponse;
    private QuizAttemptResponse quizAttemptResponse;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        quizTakeResponse = new QuizTakeResponse(QUIZ_ID, 1L, "Quiz 1", "Desc", 70, 3, 30, List.of());
        quizAttemptResponse = new QuizAttemptResponse(ATTEMPT_ID, QUIZ_ID, "Quiz 1", 50L, 1, BigDecimal.valueOf(100.0), 10, 10, true, QuizAttemptStatus.COMPLETED, null, null, List.of());
    }

    private RequestPostProcessor jwtWithRole(String role) {
        return jwt().authorities(createAuthorityList("ROLE_" + role));
    }

    @Test
    void getQuizForTaking_Authenticated_ReturnsOk() throws Exception {
        when(quizAttemptService.getQuizForTaking(QUIZ_ID)).thenReturn(quizTakeResponse);

        mockMvc.perform(get("/api/v1/quizzes/{quizId}/take", QUIZ_ID)
                        .with(jwtWithRole("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(QUIZ_ID))
                .andExpect(jsonPath("$.title").value("Quiz 1"));
    }

    @Test
    void getQuizForTaking_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/quizzes/{quizId}/take", QUIZ_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startAttempt_Authenticated_ReturnsCreated() throws Exception {
        QuizAttemptResponse inProgressResponse = new QuizAttemptResponse(ATTEMPT_ID, QUIZ_ID, "Quiz 1", 50L, 1, null, null, null, null, QuizAttemptStatus.IN_PROGRESS, null, null, List.of());
        when(quizAttemptService.startAttempt(QUIZ_ID)).thenReturn(inProgressResponse);

        mockMvc.perform(post("/api/v1/quizzes/{quizId}/attempts", QUIZ_ID)
                        .with(jwtWithRole("STUDENT")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/quizzes/" + QUIZ_ID + "/attempts/" + ATTEMPT_ID))
                .andExpect(jsonPath("$.id").value(ATTEMPT_ID))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void submitAttempt_Authenticated_ReturnsOk() throws Exception {
        QuizAttemptSubmitRequest submitRequest = new QuizAttemptSubmitRequest(List.of(
                new QuestionAnswerSubmission(301L, List.of(201L))
        ));
        when(quizAttemptService.submitAttempt(eq(QUIZ_ID), eq(ATTEMPT_ID), any(QuizAttemptSubmitRequest.class)))
                .thenReturn(quizAttemptResponse);

        mockMvc.perform(post("/api/v1/quizzes/{quizId}/attempts/{attemptId}/submit", QUIZ_ID, ATTEMPT_ID)
                        .with(jwtWithRole("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ATTEMPT_ID))
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getStudentAttempts_Authenticated_ReturnsOk() throws Exception {
        when(quizAttemptService.getStudentAttempts(QUIZ_ID)).thenReturn(List.of(quizAttemptResponse));

        mockMvc.perform(get("/api/v1/quizzes/{quizId}/attempts", QUIZ_ID)
                        .with(jwtWithRole("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ATTEMPT_ID));
    }

    @Test
    void getAttempt_Authenticated_ReturnsOk() throws Exception {
        when(quizAttemptService.getAttemptById(QUIZ_ID, ATTEMPT_ID)).thenReturn(quizAttemptResponse);

        mockMvc.perform(get("/api/v1/quizzes/{quizId}/attempts/{attemptId}", QUIZ_ID, ATTEMPT_ID)
                        .with(jwtWithRole("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ATTEMPT_ID));
    }

    @Test
    void getAllAttempts_Owner_ReturnsOk() throws Exception {
        when(quizSecurityService.isQuizOwner(QUIZ_ID)).thenReturn(true);
        when(quizAttemptService.getAllAttemptsForQuiz(QUIZ_ID)).thenReturn(List.of(quizAttemptResponse));

        mockMvc.perform(get("/api/v1/quizzes/{quizId}/attempts/all", QUIZ_ID)
                        .with(jwtWithRole("INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllAttempts_NonOwner_ReturnsForbidden() throws Exception {
        when(quizSecurityService.isQuizOwner(QUIZ_ID)).thenReturn(false);

        mockMvc.perform(get("/api/v1/quizzes/{quizId}/attempts/all", QUIZ_ID)
                        .with(jwtWithRole("STUDENT")))
                .andExpect(status().isForbidden());
    }
}
