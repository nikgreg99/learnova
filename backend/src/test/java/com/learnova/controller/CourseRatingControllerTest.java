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

@WebMvcTest(controllers = CourseRatingController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class CourseRatingControllerTest {

    private static final Long COURSE_ID = 1L;

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

        ratingRequest = new RatingRequest(5, "Brilliant course!");
        ratingResponse = new RatingResponse(10L, 2L, "test-user", COURSE_ID, null, 5, "Brilliant course!",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void createRating_shouldReturn201WhenAuthenticated() throws Exception {
        when(ratingService.createCourseRating(eq(COURSE_ID), any(RatingRequest.class))).thenReturn(ratingResponse);

        mockMvc.perform(post("/api/v1/courses/{courseId}/ratings", COURSE_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ratingRequest)))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/courses/1/ratings/10"))
                .andExpect(jsonPath("$.id").value(10)).andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.review").value("Brilliant course!"));

        verify(ratingService).createCourseRating(eq(COURSE_ID), any(RatingRequest.class));
    }

    @Test
    void createRating_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/courses/{courseId}/ratings", COURSE_ID).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ratingRequest))).andExpect(status().isUnauthorized());
    }

    @Test
    void createRating_shouldReturn400WhenInvalidRating() throws Exception {
        RatingRequest invalid = new RatingRequest(6, "Invalid rating score");

        mockMvc.perform(post("/api/v1/courses/{courseId}/ratings", COURSE_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRatings_shouldReturn200() throws Exception {
        when(ratingService.getCourseRatings(COURSE_ID)).thenReturn(List.of(ratingResponse));

        mockMvc.perform(get("/api/v1/courses/{courseId}/ratings", COURSE_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void getRatingSummary_shouldReturn200() throws Exception {
        RatingSummaryResponse summary = new RatingSummaryResponse(4.8, 15L);
        when(ratingService.getCourseRatingSummary(COURSE_ID)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/courses/{courseId}/ratings/summary", COURSE_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))).andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.8)).andExpect(jsonPath("$.totalRatings").value(15));
    }

    @Test
    void getMyRating_shouldReturn200() throws Exception {
        when(ratingService.getMyCourseRating(COURSE_ID)).thenReturn(ratingResponse);

        mockMvc.perform(get("/api/v1/courses/{courseId}/ratings/me", COURSE_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void updateMyRating_shouldReturn200() throws Exception {
        when(ratingService.updateMyCourseRating(eq(COURSE_ID), any(RatingRequest.class))).thenReturn(ratingResponse);

        mockMvc.perform(put("/api/v1/courses/{courseId}/ratings/me", COURSE_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ratingRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void deleteMyRating_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/{courseId}/ratings/me", COURSE_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))).andExpect(status().isNoContent());

        verify(ratingService).deleteMyCourseRating(COURSE_ID);
    }
}
