package com.learnova.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnova.config.ratelimit.RateLimitInterceptor;
import com.learnova.config.security.SecurityConfig;
import com.learnova.dto.rating.RatingRequest;
import com.learnova.dto.rating.RatingResponse;
import com.learnova.dto.rating.RatingSummaryResponse;
import com.learnova.service.RatingService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(controllers = LessonRatingController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class LessonRatingControllerTest {

    private static final Long LESSON_ID = 5L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private RatingService ratingService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    private RatingRequest ratingRequest;
    private RatingResponse ratingResponse;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        ratingRequest = new RatingRequest(4, "Clear explanation!");
        ratingResponse = new RatingResponse(20L, 2L, "test-user", null, LESSON_ID, 4, "Clear explanation!",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void createRating_shouldReturn201WhenAuthenticated() throws Exception {
        when(ratingService.createLessonRating(eq(LESSON_ID), any(RatingRequest.class))).thenReturn(ratingResponse);

        mockMvc.perform(post("/api/v1/lessons/{lessonId}/ratings", LESSON_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ratingRequest)))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/lessons/5/ratings/20"))
                .andExpect(jsonPath("$.id").value(20)).andExpect(jsonPath("$.lessonId").value(5))
                .andExpect(jsonPath("$.rating").value(4)).andExpect(jsonPath("$.review").value("Clear explanation!"));

        verify(ratingService).createLessonRating(eq(LESSON_ID), any(RatingRequest.class));
    }

    @Test
    void createRating_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/lessons/{lessonId}/ratings", LESSON_ID).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ratingRequest))).andExpect(status().isUnauthorized());
    }

    @Test
    void createRating_shouldReturn400WhenInvalidRating() throws Exception {
        RatingRequest invalid = new RatingRequest(0, "Score too low");

        mockMvc.perform(post("/api/v1/lessons/{lessonId}/ratings", LESSON_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRatings_shouldReturn200() throws Exception {
        when(ratingService.getLessonRatings(LESSON_ID)).thenReturn(List.of(ratingResponse));

        mockMvc.perform(get("/api/v1/lessons/{lessonId}/ratings", LESSON_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(20));
    }

    @Test
    void getRatingSummary_shouldReturn200() throws Exception {
        RatingSummaryResponse summary = new RatingSummaryResponse(4.2, 8L);
        when(ratingService.getLessonRatingSummary(LESSON_ID)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/lessons/{lessonId}/ratings/summary", LESSON_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))).andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.2)).andExpect(jsonPath("$.totalRatings").value(8));
    }

    @Test
    void getMyRating_shouldReturn200() throws Exception {
        when(ratingService.getMyLessonRating(LESSON_ID)).thenReturn(ratingResponse);

        mockMvc.perform(get("/api/v1/lessons/{lessonId}/ratings/me", LESSON_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20));
    }

    @Test
    void updateMyRating_shouldReturn200() throws Exception {
        when(ratingService.updateMyLessonRating(eq(LESSON_ID), any(RatingRequest.class))).thenReturn(ratingResponse);

        mockMvc.perform(put("/api/v1/lessons/{lessonId}/ratings/me", LESSON_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ratingRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(20));
    }

    @Test
    void deleteMyRating_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/lessons/{lessonId}/ratings/me", LESSON_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))).andExpect(status().isNoContent());

        verify(ratingService).deleteMyLessonRating(LESSON_ID);
    }
}
