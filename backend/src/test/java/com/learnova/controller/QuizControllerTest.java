package com.learnova.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnova.config.ratelimit.RateLimitInterceptor;
import com.learnova.config.security.SecurityConfig;
import com.learnova.dto.quiz.AnswerOptionRequest;
import com.learnova.dto.quiz.QuestionRequest;
import com.learnova.dto.quiz.QuestionResponse;
import com.learnova.dto.quiz.QuizPatchRequest;
import com.learnova.dto.quiz.QuizRequest;
import com.learnova.dto.quiz.QuizResponse;
import com.learnova.model.QuestionType;
import com.learnova.service.QuizService;
import com.learnova.service.security.QuizSecurityService;
import java.time.LocalDateTime;
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

@WebMvcTest(controllers = QuizController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private QuizService quizService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockitoBean(name = "quizSecurityService")
    private QuizSecurityService quizSecurityService;

    private static final Long QUIZ_ID = 10L;
    private static final Long QUESTION_ID = 100L;

    private QuizRequest quizRequest;
    private QuizResponse quizResponse;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        quizRequest = new QuizRequest("Basics Quiz", "Description", 70, 3, 30, true);
        quizResponse = new QuizResponse(QUIZ_ID, 1L, "Basics Quiz", "Description", 70, 3, 30, true, List.of(), LocalDateTime.now(), LocalDateTime.now());
    }

    private RequestPostProcessor jwtWithRole(String role) {
        return jwt().authorities(createAuthorityList("ROLE_" + role));
    }

    @Test
    void getQuiz_Owner_ReturnsOk() throws Exception {
        when(quizSecurityService.isQuizOwner(QUIZ_ID)).thenReturn(true);
        when(quizService.getQuizById(QUIZ_ID)).thenReturn(quizResponse);

        mockMvc.perform(get("/api/v1/quizzes/{quizId}", QUIZ_ID)
                        .with(jwtWithRole("INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(QUIZ_ID))
                .andExpect(jsonPath("$.title").value("Basics Quiz"));
    }

    @Test
    void getQuiz_NonOwner_ReturnsForbidden() throws Exception {
        when(quizSecurityService.isQuizOwner(QUIZ_ID)).thenReturn(false);

        mockMvc.perform(get("/api/v1/quizzes/{quizId}", QUIZ_ID)
                        .with(jwtWithRole("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateQuiz_Owner_ReturnsOk() throws Exception {
        when(quizSecurityService.isQuizOwner(QUIZ_ID)).thenReturn(true);
        when(quizService.updateQuiz(eq(QUIZ_ID), any(QuizRequest.class))).thenReturn(quizResponse);

        mockMvc.perform(put("/api/v1/quizzes/{quizId}", QUIZ_ID)
                        .with(jwtWithRole("INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Basics Quiz"));
    }

    @Test
    void patchQuiz_Owner_ReturnsOk() throws Exception {
        when(quizSecurityService.isQuizOwner(QUIZ_ID)).thenReturn(true);
        QuizPatchRequest patchRequest = new QuizPatchRequest("Patched Title", null, null, null, null, null);
        when(quizService.patchQuiz(eq(QUIZ_ID), any(QuizPatchRequest.class))).thenReturn(quizResponse);

        mockMvc.perform(patch("/api/v1/quizzes/{quizId}", QUIZ_ID)
                        .with(jwtWithRole("INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteQuiz_Owner_ReturnsNoContent() throws Exception {
        when(quizSecurityService.isQuizOwner(QUIZ_ID)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/quizzes/{quizId}", QUIZ_ID)
                        .with(jwtWithRole("INSTRUCTOR")))
                .andExpect(status().isNoContent());

        verify(quizService).deleteQuiz(QUIZ_ID);
    }

    @Test
    void addQuestion_Owner_ReturnsCreated() throws Exception {
        when(quizSecurityService.isQuizOwner(QUIZ_ID)).thenReturn(true);

        List<AnswerOptionRequest> options = List.of(
                new AnswerOptionRequest("Option 1", true, 1),
                new AnswerOptionRequest("Option 2", false, 2)
        );
        QuestionRequest questionRequest = new QuestionRequest("What is OOP?", QuestionType.SINGLE_CHOICE, 1, 1, options);
        QuestionResponse questionResponse = new QuestionResponse(QUESTION_ID, QUIZ_ID, "What is OOP?", QuestionType.SINGLE_CHOICE, 1, 1, List.of());

        when(quizService.addQuestion(eq(QUIZ_ID), any(QuestionRequest.class))).thenReturn(questionResponse);

        mockMvc.perform(post("/api/v1/quizzes/{quizId}/questions", QUIZ_ID)
                        .with(jwtWithRole("INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/quizzes/" + QUIZ_ID + "/questions/" + QUESTION_ID))
                .andExpect(jsonPath("$.id").value(QUESTION_ID));
    }

    @Test
    void updateQuestion_Owner_ReturnsOk() throws Exception {
        when(quizSecurityService.isQuizOwner(QUIZ_ID)).thenReturn(true);

        List<AnswerOptionRequest> options = List.of(
                new AnswerOptionRequest("Option 1", true, 1),
                new AnswerOptionRequest("Option 2", false, 2)
        );
        QuestionRequest questionRequest = new QuestionRequest("What is OOP?", QuestionType.SINGLE_CHOICE, 1, 1, options);
        QuestionResponse questionResponse = new QuestionResponse(QUESTION_ID, QUIZ_ID, "What is OOP?", QuestionType.SINGLE_CHOICE, 1, 1, List.of());

        when(quizService.updateQuestion(eq(QUIZ_ID), eq(QUESTION_ID), any(QuestionRequest.class))).thenReturn(questionResponse);

        mockMvc.perform(put("/api/v1/quizzes/{quizId}/questions/{questionId}", QUIZ_ID, QUESTION_ID)
                        .with(jwtWithRole("INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(QUESTION_ID));
    }

    @Test
    void deleteQuestion_Owner_ReturnsNoContent() throws Exception {
        when(quizSecurityService.isQuizOwner(QUIZ_ID)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/quizzes/{quizId}/questions/{questionId}", QUIZ_ID, QUESTION_ID)
                        .with(jwtWithRole("INSTRUCTOR")))
                .andExpect(status().isNoContent());

        verify(quizService).deleteQuestion(QUIZ_ID, QUESTION_ID);
    }
}
