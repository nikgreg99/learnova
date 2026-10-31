package com.learnova.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.learnova.dto.course.CourseRequest;
import com.learnova.dto.course.CourseResponse;
import com.learnova.service.CourseService;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(CourseController.class)
public class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JsonMapper objectMapper;
    @MockitoBean
    private CourseService courseService;

    private CourseRequest courseRequest;
    private CourseResponse courseResponse;

    @BeforeEach
    void setUp() {
        courseRequest = new CourseRequest(
                "Java Fundamentals",
                "Introduction to Java",
                new BigDecimal("49.90"));

        courseResponse = new CourseResponse(
                1L,
                courseRequest.name(),
                courseRequest.description(),
                courseRequest.price(),
                null,
                null);
    }

    @Test
    void shouldReturnCoursesWhenCoursesExist() throws Exception {
        when(courseService.getCourses()).thenReturn(List.of(courseResponse));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value(courseResponse.name()));
    }

    @Test
    void shouldReturnEmptyListWhenNoCoursesExist() throws Exception {
        when(courseService.getCourses()).thenReturn(List.of());

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnCourseWhenCourseExists() throws Exception {
        when(courseService.getCourseById(courseResponse.id())).thenReturn(Optional.of(courseResponse));

        mockMvc.perform(get("/api/courses/{id}", courseResponse.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseResponse.id()))
                .andExpect(jsonPath("$.name").value(courseResponse.name()));
    }

    @Test
    void shouldReturnNotFoundWhenCourseDoesNotExist() throws Exception {
        Long nonExistingId = 999L;
        when(courseService.getCourseById(nonExistingId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/courses/{id}", nonExistingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateCourseWhenRequestIsValid() throws Exception {
        when(courseService.addCourse(courseRequest)).thenReturn(courseResponse);

        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(courseResponse.id()))
                .andExpect(jsonPath("$.name").value(courseRequest.name()));
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        CourseRequest invalidRequest = new CourseRequest(
                "",
                "Introduction to Java",
                new BigDecimal("49.90"));

        mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    void shouldUpdateCourseWhenCourseExists() throws Exception {
        when(courseService.updateCourse(courseResponse.id(), courseRequest)).thenReturn(Optional.of(courseResponse));

        mockMvc.perform(put("/api/courses/{id}", courseResponse.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseResponse.id()))
                .andExpect(jsonPath("$.name").value(courseResponse.name()));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistingCourse() throws Exception {
        when(courseService.updateCourse(courseResponse.id(), courseRequest)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/courses/{id}", courseResponse.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteCourseWhenCourseExists() throws Exception {
        mockMvc.perform(delete("/api/courses/{id}", courseResponse.id()))
                .andExpect(status().isNoContent());

        verify(courseService).deleteCourseById(courseResponse.id());
    }
}