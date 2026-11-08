
package com.learnova.controller;

import com.learnova.config.ratelimit.RateLimitInterceptor;
import com.learnova.config.security.SecurityConfig;
import com.learnova.dto.course.CoursePatchRequest;
import com.learnova.dto.course.CourseRequest;
import com.learnova.dto.course.CourseResponse;
import com.learnova.dto.course.CourseStatusUpdateRequest;
import com.learnova.model.CourseStatus;
import com.learnova.service.CourseService;

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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    private CourseRequest courseRequest;
    private CourseResponse courseResponse;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);
        courseRequest = new CourseRequest("Java Fundamentals", "Introduction to Java", new BigDecimal("49.90"));

        courseResponse = new CourseResponse(1L, courseRequest.name(), null, null, courseRequest.description(),
                courseRequest.price(), null, null);
    }

    @Test
    void getCourses_shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(get("/api/v1/courses")).andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }

    @Test
    void getCourses_shouldReturnCoursesWhenCoursesExist() throws Exception {
        when(courseService.getCourses()).thenReturn(List.of(courseResponse));

        mockMvc.perform(get("/api/v1/courses").with(jwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value(courseResponse.name()));
    }

    @Test
    void getCourses_shouldReturnEmptyListWhenNoCoursesExist() throws Exception {
        when(courseService.getCourses()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/courses").with(jwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getCourse_shouldReturnCourseWhenCourseExists() throws Exception {
        when(courseService.getCourseById(courseResponse.id())).thenReturn(Optional.of(courseResponse));

        mockMvc.perform(get("/api/v1/courses/{id}", courseResponse.id()).with(jwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseResponse.id()))
                .andExpect(jsonPath("$.name").value(courseResponse.name()));
    }

    @Test
    void getCourse_shouldReturnNotFoundWhenCourseDoesNotExist() throws Exception {
        Long nonExistingId = 999L;

        when(courseService.getCourseById(nonExistingId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/courses/{id}", nonExistingId).with(jwt())).andExpect(status().isNotFound());
    }

    @Test
    void createCourse_shouldCreateCourseWhenRequestIsValid() throws Exception {
        when(courseService.addCourse(courseRequest)).thenReturn(courseResponse);

        mockMvc.perform(post("/api/v1/courses").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courseRequest))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(courseResponse.id()))
                .andExpect(jsonPath("$.name").value(courseRequest.name()));
    }

    @Test
    void createCourse_shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        CourseRequest invalidRequest = new CourseRequest("", "Introduction to Java", new BigDecimal("49.90"));

        mockMvc.perform(post("/api/v1/courses").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))).andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    void updateCourse_shouldUpdateCourseWhenCourseExists() throws Exception {
        when(courseService.updateCourse(courseResponse.id(), courseRequest)).thenReturn(Optional.of(courseResponse));

        mockMvc.perform(put("/api/v1/courses/{id}", courseResponse.id()).with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(courseResponse.id()))
                .andExpect(jsonPath("$.name").value(courseResponse.name()));
    }

    @Test
    void updateCourse_shouldReturnNotFoundWhenUpdatingNonExistingCourse() throws Exception {

        when(courseService.updateCourse(courseResponse.id(), courseRequest)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/courses/{id}", courseResponse.id()).with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCourse_shouldDeleteCourseWhenCourseExists() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/{id}", courseResponse.id()).with(jwt()))
                .andExpect(status().isNoContent());

        verify(courseService).deleteCourseById(courseResponse.id());
    }

    @Test
    void patchCourse_shouldPartiallyUpdateCourseWhenCourseExists() throws Exception {
        CoursePatchRequest patchRequest = new CoursePatchRequest("Patched Java", null, null, null);

        when(courseService.patchCourse(courseResponse.id(), patchRequest)).thenReturn(Optional.of(courseResponse));

        mockMvc.perform(patch("/api/v1/courses/{id}", courseResponse.id()).with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(courseResponse.id()));
    }

    @Test
    void updateCourseStatus_shouldUpdateCourseStatusWhenCourseExists() throws Exception {
        CourseStatusUpdateRequest statusRequest = new CourseStatusUpdateRequest(CourseStatus.PUBLISHED);

        when(courseService.updateCourseStatus(courseResponse.id(), CourseStatus.PUBLISHED))
                .thenReturn(Optional.of(courseResponse));

        mockMvc.perform(patch("/api/v1/courses/{id}/status", courseResponse.id()).with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(courseResponse.id()));
    }
}
