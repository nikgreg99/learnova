
package com.learnova.controller;

import com.learnova.config.ratelimit.RateLimitInterceptor;
import com.learnova.service.security.CourseSecurityService;
import com.learnova.config.security.SecurityConfig;
import com.learnova.dto.lesson.LessonPatchRequest;
import com.learnova.dto.lesson.LessonRequest;
import com.learnova.dto.lesson.LessonResponse;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @MockitoBean(name = "courseSecurityService")
    private CourseSecurityService courseSecurityService;

    private static final Long COURSE_ID = 1L;
    private static final Long LESSON_ID = 10L;

    private LessonRequest lessonRequest;
    private LessonResponse lessonResponse;
    private LessonPatchRequest patchRequest;

    @BeforeEach
    void setUp() throws Exception {

        when(rateLimitInterceptor.preHandle(any(), any(), any()))
            .thenReturn(true);

        when(courseSecurityService.isOwner(COURSE_ID))
            .thenReturn(false);

        lessonRequest = new LessonRequest(
            "Introduction",
            "Course Intro",
            "Welcome to the course",
            1,
            10
        );

        lessonResponse = new LessonResponse(
            LESSON_ID,
            COURSE_ID,
            "Introduction",
            "Course Intro",
            "Welcome to the course",
            1,
            10,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        patchRequest = new LessonPatchRequest(
            "Patched Introduction",
            null,
            null,
            null,
            null
        );
    }

    private RequestPostProcessor jwtWithRole(String role) {
        return jwt().authorities(createAuthorityList("ROLE_" + role));
    }

    private String lessonsUrl() {
        return "/api/v1/courses/" + COURSE_ID + "/lessons";
    }

    private String lessonUrl() {
        return lessonsUrl() + "/" + LESSON_ID;
    }

    private void mockCourseOwner() {
        when(courseSecurityService.isOwner(COURSE_ID))
            .thenReturn(true);
    }

    private String serialize(Object request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    @Test
    void getLessons_shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(get(lessonsUrl()))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(lessonService);
    }

    @Test
    void getLessons_shouldReturnLessonsWhenUserIsStudent() throws Exception {

        when(lessonService.getLessons(COURSE_ID))
            .thenReturn(List.of(lessonResponse));

        mockMvc.perform(get(lessonsUrl())
                .with(jwtWithRole("STUDENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(LESSON_ID));

        verify(lessonService).getLessons(COURSE_ID);
    }

    @Test
    void getLessons_shouldReturnLessonsWhenUserIsInstructor() throws Exception {

        when(lessonService.getLessons(COURSE_ID))
            .thenReturn(List.of(lessonResponse));

        mockMvc.perform(get(lessonsUrl())
                .with(jwtWithRole("INSTRUCTOR")))
            .andExpect(status().isOk());

        verify(lessonService).getLessons(COURSE_ID);
    }

    @Test
    void getLessons_shouldReturnLessonsWhenUserIsAdmin() throws Exception {

        when(lessonService.getLessons(COURSE_ID))
            .thenReturn(List.of(lessonResponse));

        mockMvc.perform(get(lessonsUrl())
                .with(jwtWithRole("ADMIN")))
            .andExpect(status().isOk());

        verify(lessonService).getLessons(COURSE_ID);
    }

    @Test
    void getLessons_shouldReturnEmptyListWhenNoLessonsExist() throws Exception {

        when(lessonService.getLessons(COURSE_ID))
            .thenReturn(List.of());

        mockMvc.perform(get(lessonsUrl())
                .with(jwtWithRole("STUDENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));

        verify(lessonService).getLessons(COURSE_ID);
    }

    @Test
    void getLesson_shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(get(lessonUrl()))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(lessonService);
    }

    @Test
    void getLesson_shouldReturnLessonWhenUserIsStudent() throws Exception {

        when(lessonService.getLessonById(COURSE_ID, LESSON_ID))
            .thenReturn(lessonResponse);

        mockMvc.perform(get(lessonUrl())
                .with(jwtWithRole("STUDENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(LESSON_ID))
            .andExpect(jsonPath("$.title").value(lessonResponse.title()));

        verify(lessonService).getLessonById(COURSE_ID, LESSON_ID);
    }

    @Test
    void getLesson_shouldReturnLessonWhenUserIsInstructor() throws Exception {

        when(lessonService.getLessonById(COURSE_ID, LESSON_ID))
            .thenReturn(lessonResponse);

        mockMvc.perform(get(lessonUrl())
                .with(jwtWithRole("INSTRUCTOR")))
            .andExpect(status().isOk());

        verify(lessonService).getLessonById(COURSE_ID, LESSON_ID);
    }

    @Test
    void getLesson_shouldReturnLessonWhenUserIsAdmin() throws Exception {

        when(lessonService.getLessonById(COURSE_ID, LESSON_ID))
            .thenReturn(lessonResponse);

        mockMvc.perform(get(lessonUrl())
                .with(jwtWithRole("ADMIN")))
            .andExpect(status().isOk());

        verify(lessonService).getLessonById(COURSE_ID, LESSON_ID);
    }

    @Test
    void createLesson_shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(post(lessonsUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(lessonRequest)))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(lessonService);
    }

    @Test
    void createLesson_shouldCreateLessonWhenUserIsInstructorAndOwner() throws Exception {

        mockCourseOwner();

        when(lessonService.createLesson(eq(COURSE_ID), any(LessonRequest.class)))
            .thenReturn(lessonResponse);

        mockMvc.perform(post(lessonsUrl())
                .with(jwtWithRole("INSTRUCTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(lessonRequest)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(header().string(
                "Location",
                lessonsUrl() + "/" + LESSON_ID
            ))
            .andExpect(jsonPath("$.id").value(LESSON_ID));

        verify(lessonService).createLesson(eq(COURSE_ID), any(LessonRequest.class));
    }

    @Test
    void createLesson_shouldCreateLessonWhenUserIsAdmin() throws Exception {

        mockCourseOwner();

        when(lessonService.createLesson(eq(COURSE_ID), any(LessonRequest.class)))
            .thenReturn(lessonResponse);

        mockMvc.perform(post(lessonsUrl())
                .with(jwtWithRole("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(lessonRequest)))
            .andExpect(status().isCreated());

        verify(lessonService).createLesson(eq(COURSE_ID), any(LessonRequest.class));
    }

    @Test
    void createLesson_shouldReturnForbiddenWhenUserIsStudent() throws Exception {

        mockMvc.perform(post(lessonsUrl())
                .with(jwtWithRole("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(lessonRequest)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService);
    }

    @Test
    void createLesson_shouldReturnForbiddenWhenInstructorIsNotOwner() throws Exception {

        mockMvc.perform(post(lessonsUrl())
                .with(jwtWithRole("INSTRUCTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(lessonRequest)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService);
    }

    @Test
    void updateLesson_shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(put(lessonUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(lessonRequest)))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(lessonService);
    }

    @Test
    void updateLesson_shouldUpdateLessonWhenUserIsInstructorAndOwner() throws Exception {

        mockCourseOwner();

        when(lessonService.updateLesson(
            eq(COURSE_ID),
            eq(LESSON_ID),
            any(LessonRequest.class)
        )).thenReturn(lessonResponse);

        mockMvc.perform(put(lessonUrl())
                .with(jwtWithRole("INSTRUCTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(lessonRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(LESSON_ID));

        verify(lessonService).updateLesson(
            eq(COURSE_ID),
            eq(LESSON_ID),
            any(LessonRequest.class)
        );
    }

    @Test
    void updateLesson_shouldUpdateLessonWhenUserIsAdmin() throws Exception {

        mockCourseOwner();

        when(lessonService.updateLesson(
            eq(COURSE_ID),
            eq(LESSON_ID),
            any(LessonRequest.class)
        )).thenReturn(lessonResponse);

        mockMvc.perform(put(lessonUrl())
                .with(jwtWithRole("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(lessonRequest)))
            .andExpect(status().isOk());

        verify(lessonService).updateLesson(
            eq(COURSE_ID),
            eq(LESSON_ID),
            any(LessonRequest.class)
        );
    }

    @Test
    void updateLesson_shouldReturnForbiddenWhenUserIsStudent() throws Exception {

        mockMvc.perform(put(lessonUrl())
                .with(jwtWithRole("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(lessonRequest)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService);
    }

    @Test
    void updateLesson_shouldReturnForbiddenWhenInstructorIsNotOwner() throws Exception {

        mockMvc.perform(put(lessonUrl())
                .with(jwtWithRole("INSTRUCTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(lessonRequest)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService);
    }

    @Test
    void patchLesson_shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(patch(lessonUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(patchRequest)))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(lessonService);
    }

    @Test
    void patchLesson_shouldPartiallyUpdateLessonWhenUserIsInstructorAndOwner() throws Exception {

        mockCourseOwner();

        when(lessonService.patchLesson(
            eq(COURSE_ID),
            eq(LESSON_ID),
            any(LessonPatchRequest.class)
        )).thenReturn(lessonResponse);

        mockMvc.perform(patch(lessonUrl())
                .with(jwtWithRole("INSTRUCTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(patchRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(LESSON_ID));

        verify(lessonService).patchLesson(
            eq(COURSE_ID),
            eq(LESSON_ID),
            any(LessonPatchRequest.class)
        );
    }

    @Test
    void patchLesson_shouldPartiallyUpdateLessonWhenUserIsAdmin() throws Exception {

        mockCourseOwner();

        when(lessonService.patchLesson(
            eq(COURSE_ID),
            eq(LESSON_ID),
            any(LessonPatchRequest.class)
        )).thenReturn(lessonResponse);

        mockMvc.perform(patch(lessonUrl())
                .with(jwtWithRole("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(patchRequest)))
            .andExpect(status().isOk());

        verify(lessonService).patchLesson(
            eq(COURSE_ID),
            eq(LESSON_ID),
            any(LessonPatchRequest.class)
        );
    }

    @Test
    void patchLesson_shouldReturnForbiddenWhenUserIsStudent() throws Exception {

        mockMvc.perform(patch(lessonUrl())
                .with(jwtWithRole("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(patchRequest)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService);
    }

    @Test
    void patchLesson_shouldReturnForbiddenWhenInstructorIsNotOwner() throws Exception {

        mockMvc.perform(patch(lessonUrl())
                .with(jwtWithRole("INSTRUCTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(patchRequest)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService);
    }

    @Test
    void deleteLesson_shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(delete(lessonUrl()))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(lessonService);
    }

    @Test
    void deleteLesson_shouldDeleteLessonWhenUserIsInstructorAndOwner() throws Exception {

        mockCourseOwner();

        mockMvc.perform(delete(lessonUrl())
                .with(jwtWithRole("INSTRUCTOR")))
            .andExpect(status().isNoContent());

        verify(lessonService).deleteLesson(COURSE_ID, LESSON_ID);
    }

    @Test
    void deleteLesson_shouldDeleteLessonWhenUserIsAdmin() throws Exception {

        mockCourseOwner();

        mockMvc.perform(delete(lessonUrl())
                .with(jwtWithRole("ADMIN")))
            .andExpect(status().isNoContent());

        verify(lessonService).deleteLesson(COURSE_ID, LESSON_ID);
    }

    @Test
    void deleteLesson_shouldReturnForbiddenWhenUserIsStudent() throws Exception {

        mockMvc.perform(delete(lessonUrl())
                .with(jwtWithRole("STUDENT")))
            .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService);
    }

    @Test
    void deleteLesson_shouldReturnForbiddenWhenInstructorIsNotOwner() throws Exception {

        mockMvc.perform(delete(lessonUrl())
                .with(jwtWithRole("INSTRUCTOR")))
            .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService);
    }
}
