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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.learnova.dto.CourseRequest;
import com.learnova.dto.CourseResponse;
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

    @Test
    void shouldReturnCoursesWhenCoursesExist() throws Exception {
        CourseResponse response = new CourseResponse(
                1L,
                "Java Fundamentals",
                "Introduction to Java",
                new BigDecimal("49.90"),
                null,
                null);

        when(courseService.getCourses()).thenReturn(List.of(response));

        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Java Fundamentals"));
    }

    @Test
    void shouldReturnEmptyListWhenNoCoursesExist() throws Exception {
        when(courseService.getCourses()).thenReturn(List.of());

        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnCourseWhenCourseExists() throws Exception {
        CourseResponse response = new CourseResponse(
                1L,
                "Java Fundamentals",
                "Introduction to Java",
                new BigDecimal("49.90"),
                null,
                null);

        when(courseService.getCourseById(response.id())).thenReturn(Optional.of(response));

        mockMvc.perform(get("/courses/{id}", response.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id()))
                .andExpect(jsonPath("$.name").value("Java Fundamentals"));
    }

    @Test
    void shouldReturnNotFoundWhenCourseDoesNotExist() throws Exception {
        Long nonExistingId = 1L;

        when(courseService.getCourseById(nonExistingId)).thenReturn(Optional.empty());
        mockMvc.perform(get("/courses/{id}", nonExistingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateCourseWhenRequestIsValid() throws Exception {

        CourseRequest request = new CourseRequest(
                "Java Fundamentals",
                "Introduction to Java",
                new BigDecimal("49.90"));

        CourseResponse response = new CourseResponse(
                1L,
                "Java Fundamentals",
                "Introduction to Java",
                new BigDecimal("49.90"),
                null,
                null);

        when(courseService.addCourse(request)).thenReturn(response);

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value(request.name()));
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {

        CourseRequest request = new CourseRequest(
                "",
                "Introduction to Java",
                new BigDecimal("49.90"));

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    void shouldUpdateCourseWhenCourseExists() throws Exception {

        CourseRequest request = new CourseRequest(
                "Java Fundamentals",
                "Introduction to Java",
                new BigDecimal("49.90"));

        CourseResponse response = new CourseResponse(
                1L,
                "Java Fundamentals",
                "Introduction to Java",
                new BigDecimal("49.90"),
                null,
                null);

        when(courseService.updateCourse(response.id(), request)).thenReturn(Optional.of(response));

        mockMvc.perform(put("/courses/{id}", response.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id()))
                .andExpect(jsonPath("$.name").value(response.name()));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistingCourse() throws Exception {

        CourseRequest request = new CourseRequest(
                "Java Fundamentals",
                "Introduction to Java",
                new BigDecimal("49.90"));

        CourseResponse response = new CourseResponse(
                1L,
                "Java Fundamentals",
                "Introduction to Java",
                new BigDecimal("49.90"),
                null,
                null);

        when(courseService.updateCourse(response.id(), request)).thenReturn(Optional.empty());

        mockMvc.perform(put("/courses/{id}", response.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteCourseWhenCourseExists() throws Exception {

        Long courseId = 1L;

        mockMvc.perform(delete("/courses/{id}", courseId))
                .andExpect(status().isNoContent());

        verify(courseService).deleteCourseById(courseId);
    }
}