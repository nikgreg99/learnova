
package com.learnova.controller;

import com.learnova.config.ratelimit.RateLimitInterceptor;
import com.learnova.service.security.CourseSecurityService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @MockitoBean(name = "courseSecurityService")
    private CourseSecurityService courseSecurityService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    private CourseRequest courseRequest;
    private CourseResponse courseResponse;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any()))
            .thenReturn(true);

        when(courseSecurityService.isOwner(anyLong()))
            .thenReturn(true);

        courseRequest = new CourseRequest(
            "Java Fundamentals",
            "Introduction to Java",
            new BigDecimal("49.90")
        );

        courseResponse = new CourseResponse(
            1L,
            courseRequest.name(),
            null,
            null,
            courseRequest.description(),
            courseRequest.price(),
            null,
            null
        );
    }

    @Test
    @WithAnonymousUser
    void getCourses_shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/courses"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }

    @Test
    void getCourses_shouldReturnCoursesWhenCoursesExist() throws Exception {
        when(courseService.getCourses()).thenReturn(List.of(courseResponse));

        mockMvc.perform(get("/api/v1/courses")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value(courseResponse.name()));
    }

    @Test
    void getCourses_shouldReturnEmptyListWhenNoCoursesExist() throws Exception {
        when(courseService.getCourses()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/courses")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getCourses_shouldReturnCoursesWhenUserIsInstructor() throws Exception {
        when(courseService.getCourses()).thenReturn(List.of(courseResponse));

        mockMvc.perform(get("/api/v1/courses")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))))
            .andExpect(status().isOk());
    }

    @Test
    void getCourses_shouldReturnCoursesWhenUserIsAdmin() throws Exception {
        when(courseService.getCourses()).thenReturn(List.of(courseResponse));

        mockMvc.perform(get("/api/v1/courses")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk());
    }

    @Test
    void getCourse_shouldReturnCourseWhenCourseExists() throws Exception {
        when(courseService.getCourseById(courseResponse.id()))
            .thenReturn(Optional.of(courseResponse));

        mockMvc.perform(get("/api/v1/courses/{id}", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(courseResponse.id()))
            .andExpect(jsonPath("$.name").value(courseResponse.name()));
    }

    @Test
    void getCourse_shouldReturnNotFoundWhenCourseDoesNotExist() throws Exception {
        Long nonExistingId = 999L;

        when(courseService.getCourseById(nonExistingId))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/courses/{id}", nonExistingId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void getCourse_shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{id}", 1L))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithAnonymousUser
    void createCourse_shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courseRequest)))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }

    @Test
    void createCourse_shouldCreateCourseWhenUserIsInstructor() throws Exception {
        when(courseService.addCourse(courseRequest)).thenReturn(courseResponse);

        mockMvc.perform(post("/api/v1/courses")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courseRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(courseResponse.id()))
            .andExpect(jsonPath("$.name").value(courseRequest.name()));

        verify(courseService).addCourse(courseRequest);
    }

    @Test
    void createCourse_shouldCreateCourseWhenUserIsAdmin() throws Exception {
        when(courseService.addCourse(courseRequest)).thenReturn(courseResponse);

        mockMvc.perform(post("/api/v1/courses")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courseRequest)))
            .andExpect(status().isCreated());

        verify(courseService).addCourse(courseRequest);
    }

    @Test
    void createCourse_shouldReturnForbiddenWhenUserIsStudent() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courseRequest)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    void createCourse_shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        CourseRequest invalidRequest = new CourseRequest(
            "",
            "Introduction to Java",
            new BigDecimal("49.90")
        );

        mockMvc.perform(post("/api/v1/courses")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    void updateCourse_shouldUpdateCourseWhenUserIsOwner() throws Exception {
        when(courseService.updateCourse(courseResponse.id(), courseRequest))
            .thenReturn(Optional.of(courseResponse));

        mockMvc.perform(put("/api/v1/courses/{id}", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courseRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(courseResponse.id()))
            .andExpect(jsonPath("$.name").value(courseResponse.name()));

        verify(courseSecurityService).isOwner(courseResponse.id());
        verify(courseService).updateCourse(courseResponse.id(), courseRequest);
    }

    @Test
    void updateCourse_shouldReturnForbiddenWhenUserIsNotOwner() throws Exception {
        when(courseSecurityService.isOwner(courseResponse.id()))
            .thenReturn(false);

        mockMvc.perform(put("/api/v1/courses/{id}", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courseRequest)))
            .andExpect(status().isForbidden());

        verify(courseSecurityService).isOwner(courseResponse.id());
        verifyNoInteractions(courseService);
    }

    @Test
    void updateCourse_shouldUpdateCourseWhenUserIsAdmin() throws Exception {
        when(courseService.updateCourse(courseResponse.id(), courseRequest))
            .thenReturn(Optional.of(courseResponse));

        mockMvc.perform(put("/api/v1/courses/{id}", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courseRequest)))
            .andExpect(status().isOk());

        verifyNoInteractions(courseSecurityService);
        verify(courseService).updateCourse(courseResponse.id(), courseRequest);
    }

    @Test
    void updateCourse_shouldReturnNotFoundWhenCourseDoesNotExist() throws Exception {
        when(courseService.updateCourse(courseResponse.id(), courseRequest))
            .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/courses/{id}", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courseRequest)))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateCourse_shouldReturnForbiddenWhenUserIsStudent() throws Exception {
        mockMvc.perform(put("/api/v1/courses/{id}", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courseRequest)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithAnonymousUser
    void updateCourse_shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/v1/courses/{id}", courseResponse.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(courseRequest)))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }

    @Test
    void patchCourse_shouldPartiallyUpdateCourseWhenUserIsOwner() throws Exception {
        CoursePatchRequest patchRequest =
            new CoursePatchRequest("Patched Java", null, null, null);

        when(courseService.patchCourse(courseResponse.id(), patchRequest))
            .thenReturn(Optional.of(courseResponse));

        mockMvc.perform(patch("/api/v1/courses/{id}", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(courseResponse.id()));

        verify(courseSecurityService).isOwner(courseResponse.id());
        verify(courseService).patchCourse(courseResponse.id(), patchRequest);
    }

    @Test
    void patchCourse_shouldReturnForbiddenWhenUserIsNotOwner() throws Exception {
        CoursePatchRequest patchRequest =
            new CoursePatchRequest("Patched Java", null, null, null);

        when(courseSecurityService.isOwner(courseResponse.id()))
            .thenReturn(false);

        mockMvc.perform(patch("/api/v1/courses/{id}", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchRequest)))
            .andExpect(status().isForbidden());

        verify(courseSecurityService).isOwner(courseResponse.id());
        verifyNoInteractions(courseService);
    }

    @Test
    void patchCourse_shouldPartiallyUpdateCourseWhenUserIsAdmin() throws Exception {
        CoursePatchRequest patchRequest =
            new CoursePatchRequest("Patched Java", null, null, null);

        when(courseService.patchCourse(courseResponse.id(), patchRequest))
            .thenReturn(Optional.of(courseResponse));

        mockMvc.perform(patch("/api/v1/courses/{id}", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchRequest)))
            .andExpect(status().isOk());

        verifyNoInteractions(courseSecurityService);
        verify(courseService).patchCourse(courseResponse.id(), patchRequest);
    }

    @Test
    void patchCourse_shouldReturnForbiddenWhenUserIsStudent() throws Exception {
        CoursePatchRequest patchRequest =
            new CoursePatchRequest("Patched Java", null, null, null);

        mockMvc.perform(patch("/api/v1/courses/{id}", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchRequest)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithAnonymousUser
    void patchCourse_shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        CoursePatchRequest patchRequest =
            new CoursePatchRequest("Patched Java", null, null, null);

        mockMvc.perform(patch("/api/v1/courses/{id}", courseResponse.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchRequest)))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }

    @Test
    void updateCourseStatus_shouldUpdateCourseStatusWhenUserIsOwner() throws Exception {
        CourseStatusUpdateRequest statusRequest =
            new CourseStatusUpdateRequest(CourseStatus.PUBLISHED);

        when(courseService.updateCourseStatus(
            courseResponse.id(),
            CourseStatus.PUBLISHED
        )).thenReturn(Optional.of(courseResponse));

        mockMvc.perform(patch("/api/v1/courses/{id}/status", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(courseResponse.id()));

        verify(courseSecurityService).isOwner(courseResponse.id());
        verify(courseService).updateCourseStatus(
            courseResponse.id(),
            CourseStatus.PUBLISHED
        );
    }

    @Test
    void updateCourseStatus_shouldReturnForbiddenWhenUserIsNotOwner() throws Exception {
        CourseStatusUpdateRequest statusRequest =
            new CourseStatusUpdateRequest(CourseStatus.PUBLISHED);

        when(courseSecurityService.isOwner(courseResponse.id()))
            .thenReturn(false);

        mockMvc.perform(patch("/api/v1/courses/{id}/status", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
            .andExpect(status().isForbidden());

        verify(courseSecurityService).isOwner(courseResponse.id());
        verifyNoInteractions(courseService);
    }

    @Test
    void updateCourseStatus_shouldUpdateCourseStatusWhenUserIsAdmin() throws Exception {
        CourseStatusUpdateRequest statusRequest =
            new CourseStatusUpdateRequest(CourseStatus.PUBLISHED);

        when(courseService.updateCourseStatus(
            courseResponse.id(),
            CourseStatus.PUBLISHED
        )).thenReturn(Optional.of(courseResponse));

        mockMvc.perform(patch("/api/v1/courses/{id}/status", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
            .andExpect(status().isOk());

        verifyNoInteractions(courseSecurityService);
        verify(courseService).updateCourseStatus(
            courseResponse.id(),
            CourseStatus.PUBLISHED
        );
    }

    @Test
    void updateCourseStatus_shouldReturnForbiddenWhenUserIsStudent() throws Exception {
        CourseStatusUpdateRequest statusRequest =
            new CourseStatusUpdateRequest(CourseStatus.PUBLISHED);

        mockMvc.perform(patch("/api/v1/courses/{id}/status", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithAnonymousUser
    void updateCourseStatus_shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        CourseStatusUpdateRequest statusRequest =
            new CourseStatusUpdateRequest(CourseStatus.PUBLISHED);

        mockMvc.perform(patch("/api/v1/courses/{id}/status", courseResponse.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }

    @Test
    void deleteCourse_shouldDeleteCourseWhenUserIsOwner() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/{id}", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))))
            .andExpect(status().isNoContent());

        verify(courseSecurityService).isOwner(courseResponse.id());
        verify(courseService).deleteCourseById(courseResponse.id());
    }

    @Test
    void deleteCourse_shouldReturnForbiddenWhenUserIsNotOwner() throws Exception {
        when(courseSecurityService.isOwner(courseResponse.id()))
            .thenReturn(false);

        mockMvc.perform(delete("/api/v1/courses/{id}", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))))
            .andExpect(status().isForbidden());

        verify(courseSecurityService).isOwner(courseResponse.id());
        verifyNoInteractions(courseService);
    }

    @Test
    void deleteCourse_shouldDeleteCourseWhenUserIsAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/{id}", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isNoContent());

        verifyNoInteractions(courseSecurityService);
        verify(courseService).deleteCourseById(courseResponse.id());
    }

    @Test
    void deleteCourse_shouldReturnForbiddenWhenUserIsStudent() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/{id}", courseResponse.id())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
            .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @WithAnonymousUser
    void deleteCourse_shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/{id}", courseResponse.id()))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }
}
