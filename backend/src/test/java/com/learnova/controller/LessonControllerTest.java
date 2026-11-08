package com.learnova.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import com.learnova.dto.lesson.LessonPatchRequest;
import com.learnova.dto.lesson.LessonRequest;
import com.learnova.dto.lesson.LessonResponse;
import com.learnova.exception.LessonNotFoundException;
import com.learnova.exception.LessonPositionConflictException;
import com.learnova.service.LessonService;
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
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(controllers = LessonController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class LessonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private LessonService lessonService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    private LessonRequest lessonRequest;
    private LessonResponse lessonResponse;
    private final Long courseId = 1L;
    private final Long lessonId = 10L;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);
        lessonRequest = new LessonRequest("Introduction", "Course Intro", "Welcome to the course", 1, 10);
        lessonResponse = new LessonResponse(lessonId, courseId, "Introduction", "Course Intro", "Welcome to the course",
                1, 10, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void getLessons_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{courseId}/lessons", courseId)).andExpect(status().isUnauthorized());

        verifyNoInteractions(lessonService);
    }

    @Test
    void getLessons_shouldReturnLessonsList() throws Exception {
        when(lessonService.getLessons(courseId)).thenReturn(List.of(lessonResponse));

        mockMvc.perform(get("/api/v1/courses/{courseId}/lessons", courseId).with(jwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1)).andExpect(jsonPath("$[0].id").value(lessonId))
                .andExpect(jsonPath("$[0].title").value("Introduction")).andExpect(jsonPath("$[0].position").value(1));

        verify(lessonService).getLessons(courseId);
    }

    @Test
    void getLesson_shouldReturnLesson() throws Exception {
        when(lessonService.getLessonById(courseId, lessonId)).thenReturn(lessonResponse);

        mockMvc.perform(get("/api/v1/courses/{courseId}/lessons/{lessonId}", courseId, lessonId).with(jwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(lessonId))
                .andExpect(jsonPath("$.title").value("Introduction"));

        verify(lessonService).getLessonById(courseId, lessonId);
    }

    @Test
    void getLesson_shouldReturnNotFound_whenLessonNotFound() throws Exception {
        when(lessonService.getLessonById(courseId, lessonId)).thenThrow(new LessonNotFoundException(lessonId));

        mockMvc.perform(get("/api/v1/courses/{courseId}/lessons/{lessonId}", courseId, lessonId).with(jwt()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));

        verify(lessonService).getLessonById(courseId, lessonId);
    }

    @Test
    void createLesson_shouldReturnCreated() throws Exception {
        when(lessonService.createLesson(courseId, lessonRequest)).thenReturn(lessonResponse);

        mockMvc.perform(post("/api/v1/courses/{courseId}/lessons", courseId).with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(lessonRequest)))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/courses/1/lessons/10"))
                .andExpect(jsonPath("$.id").value(lessonId)).andExpect(jsonPath("$.title").value("Introduction"));

        verify(lessonService).createLesson(courseId, lessonRequest);
    }

    @Test
    void createLesson_shouldReturnBadRequest_whenPayloadIsInvalid() throws Exception {
        LessonRequest invalidRequest = new LessonRequest("", "", "", -1, -5);

        mockMvc.perform(post("/api/v1/courses/{courseId}/lessons", courseId).with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(lessonService);
    }

    @Test
    void createLesson_shouldReturnConflict_whenPositionConflict() throws Exception {
        when(lessonService.createLesson(courseId, lessonRequest))
                .thenThrow(new LessonPositionConflictException(lessonRequest.position()));

        mockMvc.perform(post("/api/v1/courses/{courseId}/lessons", courseId).with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(lessonRequest)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));

        verify(lessonService).createLesson(courseId, lessonRequest);
    }

    @Test
    void updateLesson_shouldReturnUpdatedLesson() throws Exception {
        when(lessonService.updateLesson(courseId, lessonId, lessonRequest)).thenReturn(lessonResponse);

        mockMvc.perform(put("/api/v1/courses/{courseId}/lessons/{lessonId}", courseId, lessonId).with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(lessonRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(lessonId))
                .andExpect(jsonPath("$.title").value("Introduction"));

        verify(lessonService).updateLesson(courseId, lessonId, lessonRequest);
    }

    @Test
    void patchLesson_shouldReturnPatchedLesson() throws Exception {
        LessonPatchRequest patchRequest = new LessonPatchRequest("New Title", null, null, null, null);

        when(lessonService.patchLesson(courseId, lessonId, patchRequest)).thenReturn(lessonResponse);

        mockMvc.perform(patch("/api/v1/courses/{courseId}/lessons/{lessonId}", courseId, lessonId).with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(lessonId))
                .andExpect(jsonPath("$.title").value("Introduction"));

        verify(lessonService).patchLesson(courseId, lessonId, patchRequest);
    }

    @Test
    void deleteLesson_shouldReturnNoContent() throws Exception {
        doNothing().when(lessonService).deleteLesson(courseId, lessonId);

        mockMvc.perform(delete("/api/v1/courses/{courseId}/lessons/{lessonId}", courseId, lessonId).with(jwt()))
                .andExpect(status().isNoContent());

        verify(lessonService).deleteLesson(courseId, lessonId);
    }
}
