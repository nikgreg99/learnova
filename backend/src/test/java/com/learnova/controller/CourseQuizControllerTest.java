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
import com.learnova.dto.quiz.QuizRequest;
import com.learnova.dto.quiz.QuizResponse;
import com.learnova.service.QuizService;
import com.learnova.service.security.CourseSecurityService;
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

@WebMvcTest(controllers = CourseQuizController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class CourseQuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private QuizService quizService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockitoBean(name = "courseSecurityService")
    private CourseSecurityService courseSecurityService;

    private static final Long COURSE_ID = 1L;
    private static final Long QUIZ_ID = 10L;

    private QuizRequest quizRequest;
    private QuizResponse quizResponse;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        quizRequest = new QuizRequest("Basics Quiz", "Description", 70, 3, 30, true);
        quizResponse = new QuizResponse(QUIZ_ID, COURSE_ID, "Basics Quiz", "Description", 70, 3, 30, true, List.of(), LocalDateTime.now(), LocalDateTime.now());
    }

    private RequestPostProcessor jwtWithRole(String role) {
        return jwt().authorities(createAuthorityList("ROLE_" + role));
    }

    @Test
    void createQuiz_InstructorOwner_ReturnsCreated() throws Exception {
        when(courseSecurityService.isOwner(COURSE_ID)).thenReturn(true);
        when(quizService.createQuiz(eq(COURSE_ID), any(QuizRequest.class))).thenReturn(quizResponse);

        mockMvc.perform(post("/api/v1/courses/{courseId}/quizzes", COURSE_ID)
                        .with(jwtWithRole("INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/quizzes/" + QUIZ_ID))
                .andExpect(jsonPath("$.id").value(QUIZ_ID))
                .andExpect(jsonPath("$.title").value("Basics Quiz"));
    }

    @Test
    void createQuiz_NonOwner_ReturnsForbidden() throws Exception {
        when(courseSecurityService.isOwner(COURSE_ID)).thenReturn(false);

        mockMvc.perform(post("/api/v1/courses/{courseId}/quizzes", COURSE_ID)
                        .with(jwtWithRole("INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createQuiz_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/courses/{courseId}/quizzes", COURSE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createQuiz_InvalidPayload_ReturnsBadRequest() throws Exception {
        when(courseSecurityService.isOwner(COURSE_ID)).thenReturn(true);
        QuizRequest invalidRequest = new QuizRequest("", "Desc", -5, 0, 0, true);

        mockMvc.perform(post("/api/v1/courses/{courseId}/quizzes", COURSE_ID)
                        .with(jwtWithRole("INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getQuizzes_CanView_ReturnsOk() throws Exception {
        when(courseSecurityService.canView(COURSE_ID)).thenReturn(true);
        when(quizService.getCourseQuizzes(COURSE_ID)).thenReturn(List.of(quizResponse));

        mockMvc.perform(get("/api/v1/courses/{courseId}/quizzes", COURSE_ID)
                        .with(jwtWithRole("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(QUIZ_ID));
    }

    @Test
    void getQuizzes_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{courseId}/quizzes", COURSE_ID))
                .andExpect(status().isUnauthorized());
    }
}
