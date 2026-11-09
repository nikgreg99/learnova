
package com.learnova.controller;

import com.learnova.config.ratelimit.RateLimitInterceptor;
import com.learnova.config.security.SecurityConfig;
import com.learnova.dto.user.UserProfileUpdateRequest;
import com.learnova.dto.user.UserRequest;
import com.learnova.dto.user.UserResponse;
import com.learnova.model.UserStatus;
import com.learnova.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(controllers = UserController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    private static final Long USER_ID = 1L;

    private static final String STUDENT_SUB = "student-keycloak-id";
    private static final String INSTRUCTOR_SUB = "instructor-keycloak-id";
    private static final String ADMIN_SUB = "admin-keycloak-id";

    private UserResponse userResponse;

    private static final String USER_REQUEST_JSON = """
        {
            "firstName": "John",
            "lastName": "Doe",
            "email": "john.doe@example.com",
            "username": "johndoe",
            "roles":  ["STUDENT"],
        }
        """;

    private static final String PROFILE_UPDATE_JSON = """
        {
            "firstName": "John",
            "lastName": "Updated",
            "email": "john.updated@example.com",
            "username": "johnupdated"
        }
        """;

    private static final String ACTIVE_STATUS_JSON = """
        {
            "status": "ACTIVE"
        }
        """;

    private static final String DISABLED_STATUS_JSON = """
        {
            "status": "DISABLED"
        }
        """;

    @BeforeEach
    void setUp() throws Exception {

        when(rateLimitInterceptor.preHandle(any(), any(), any()))
            .thenReturn(true);

        userResponse = new UserResponse(
            USER_ID,
            "John",
            "Doe",
            "john.doe@example.com",
            "johndoe",
            Set.of("STUDENT"),
            UserStatus.ACTIVE,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtWithRole(
        String subject,
        String role
    ) {
        return jwt()
            .jwt(token -> token.subject(subject))
            .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                "ROLE_" + role
            ));
    }

    private String userUrl() {
        return "/api/v1/users";
    }

    private String userUrl(Long id) {
        return "/api/v1/users/" + id;
    }


    @Test
    void createUser_shouldReturn400WhenRequestIsInvalid() throws Exception {

        String invalidRequest = """
            {
                "firstName": "",
                "lastName": "",
                "email": "invalid-email",
                "username": "",
                "role": "STUDENT",
                "password": ""
            }
            """;

        mockMvc.perform(post(userUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    // GET MY PROFILE

    @Test
    void getMyProfile_shouldReturn200WhenStudentIsAuthenticated() throws Exception {

        when(userService.getMyProfile(STUDENT_SUB))
            .thenReturn(userResponse);

        mockMvc.perform(get(userUrl() + "/me")
                .with(jwtWithRole(STUDENT_SUB, "STUDENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(USER_ID))
            .andExpect(jsonPath("$.username").value("johndoe"));

        verify(userService).getMyProfile(STUDENT_SUB);
    }

    @Test
    void getMyProfile_shouldReturn200WhenInstructorIsAuthenticated() throws Exception {

        when(userService.getMyProfile(INSTRUCTOR_SUB))
            .thenReturn(userResponse);

        mockMvc.perform(get(userUrl() + "/me")
                .with(jwtWithRole(INSTRUCTOR_SUB, "INSTRUCTOR")))
            .andExpect(status().isOk());

        verify(userService).getMyProfile(INSTRUCTOR_SUB);
    }

    @Test
    void getMyProfile_shouldReturn200WhenAdminIsAuthenticated() throws Exception {

        when(userService.getMyProfile(ADMIN_SUB))
            .thenReturn(userResponse);

        mockMvc.perform(get(userUrl() + "/me")
                .with(jwtWithRole(ADMIN_SUB, "ADMIN")))
            .andExpect(status().isOk());

        verify(userService).getMyProfile(ADMIN_SUB);
    }

    @Test
    void getMyProfile_shouldReturn401WhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(get(userUrl() + "/me"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    // UPDATE MY PROFILE

    @Test
    void updateMyProfile_shouldReturn200WhenStudentIsAuthenticated() throws Exception {

        when(userService.getMyProfile(STUDENT_SUB))
            .thenReturn(userResponse);

        when(userService.updateUserProfile(
            eq(USER_ID),
            any(UserProfileUpdateRequest.class)
        )).thenReturn(userResponse);

        mockMvc.perform(put(userUrl() + "/me")
                .with(jwtWithRole(STUDENT_SUB, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PROFILE_UPDATE_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(USER_ID));

        verify(userService).getMyProfile(STUDENT_SUB);
        verify(userService).updateUserProfile(
            eq(USER_ID),
            any(UserProfileUpdateRequest.class)
        );
    }

    @Test
    void updateMyProfile_shouldReturn200WhenInstructorIsAuthenticated() throws Exception {

        when(userService.getMyProfile(INSTRUCTOR_SUB))
            .thenReturn(userResponse);

        when(userService.updateUserProfile(
            eq(USER_ID),
            any(UserProfileUpdateRequest.class)
        )).thenReturn(userResponse);

        mockMvc.perform(put(userUrl() + "/me")
                .with(jwtWithRole(INSTRUCTOR_SUB, "INSTRUCTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PROFILE_UPDATE_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void updateMyProfile_shouldReturn200WhenAdminIsAuthenticated() throws Exception {

        when(userService.getMyProfile(ADMIN_SUB))
            .thenReturn(userResponse);

        when(userService.updateUserProfile(
            eq(USER_ID),
            any(UserProfileUpdateRequest.class)
        )).thenReturn(userResponse);

        mockMvc.perform(put(userUrl() + "/me")
                .with(jwtWithRole(ADMIN_SUB, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PROFILE_UPDATE_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void updateMyProfile_shouldReturn400WhenRequestIsInvalid() throws Exception {

        String invalidRequest = """
            {
                "firstName": "",
                "lastName": "",
                "email": "invalid-email",
                "username": ""
            }
            """;

        mockMvc.perform(put(userUrl() + "/me")
                .with(jwtWithRole(STUDENT_SUB, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void updateMyProfile_shouldReturn401WhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(put(userUrl() + "/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(PROFILE_UPDATE_JSON))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    // GET USERS

    @Test
    void getUsers_shouldReturn200WhenAdminIsAuthenticated() throws Exception {

        when(userService.getUsers(any()))
            .thenReturn(Page.empty());

        mockMvc.perform(get(userUrl())
                .with(jwtWithRole(ADMIN_SUB, "ADMIN")))
            .andExpect(status().isOk());

        verify(userService).getUsers(any());
    }

    @Test
    void getUsers_shouldReturn403WhenStudentIsAuthenticated() throws Exception {

        mockMvc.perform(get(userUrl())
                .with(jwtWithRole(STUDENT_SUB, "STUDENT")))
            .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void getUsers_shouldReturn403WhenInstructorIsAuthenticated() throws Exception {

        mockMvc.perform(get(userUrl())
                .with(jwtWithRole(INSTRUCTOR_SUB, "INSTRUCTOR")))
            .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void getUsers_shouldReturn401WhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(get(userUrl()))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void getUsers_shouldSupportPaginationAndSorting() throws Exception {

        when(userService.getUsers(any()))
            .thenReturn(Page.empty());

        mockMvc.perform(get(userUrl())
                .param("page", "1")
                .param("size", "10")
                .param("sort", "username,asc")
                .with(jwtWithRole(ADMIN_SUB, "ADMIN")))
            .andExpect(status().isOk());

        verify(userService).getUsers(any());
    }

    // GET USER BY ID

    @Test
    void getUser_shouldReturn200WhenAdminIsAuthenticated() throws Exception {

        when(userService.getUser(USER_ID))
            .thenReturn(userResponse);

        mockMvc.perform(get(userUrl(USER_ID))
                .with(jwtWithRole(ADMIN_SUB, "ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(USER_ID));

        verify(userService).getUser(USER_ID);
    }

    @Test
    void getUser_shouldReturn403WhenStudentIsAuthenticated() throws Exception {

        mockMvc.perform(get(userUrl(USER_ID))
                .with(jwtWithRole(STUDENT_SUB, "STUDENT")))
            .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void getUser_shouldReturn403WhenInstructorIsAuthenticated() throws Exception {

        mockMvc.perform(get(userUrl(USER_ID))
                .with(jwtWithRole(INSTRUCTOR_SUB, "INSTRUCTOR")))
            .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void getUser_shouldReturn401WhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(get(userUrl(USER_ID)))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    // UPDATE USER PROFILE

    @Test
    void updateUserProfile_shouldReturn200WhenAdminIsAuthenticated() throws Exception {

        when(userService.updateUserProfile(
            eq(USER_ID),
            any(UserProfileUpdateRequest.class)
        )).thenReturn(userResponse);

        mockMvc.perform(put(userUrl(USER_ID))
                .with(jwtWithRole(ADMIN_SUB, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PROFILE_UPDATE_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(USER_ID));

        verify(userService).updateUserProfile(
            eq(USER_ID),
            any(UserProfileUpdateRequest.class)
        );
    }

    @Test
    void updateUserProfile_shouldReturn403WhenStudentIsAuthenticated() throws Exception {

        mockMvc.perform(put(userUrl(USER_ID))
                .with(jwtWithRole(STUDENT_SUB, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PROFILE_UPDATE_JSON))
            .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void updateUserProfile_shouldReturn403WhenInstructorIsAuthenticated() throws Exception {

        mockMvc.perform(put(userUrl(USER_ID))
                .with(jwtWithRole(INSTRUCTOR_SUB, "INSTRUCTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PROFILE_UPDATE_JSON))
            .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void updateUserProfile_shouldReturn401WhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(put(userUrl(USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PROFILE_UPDATE_JSON))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    // UPDATE USER STATUS

    @Test
    void updateUserStatus_shouldReturn200WhenAdminActivatesUser() throws Exception {

        when(userService.updateUserStatus(USER_ID, UserStatus.ACTIVE))
            .thenReturn(userResponse);

        mockMvc.perform(patch(userUrl(USER_ID) + "/status")
                .with(jwtWithRole(ADMIN_SUB, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ACTIVE_STATUS_JSON))
            .andExpect(status().isOk());

        verify(userService).updateUserStatus(USER_ID, UserStatus.ACTIVE);
    }

    @Test
    void updateUserStatus_shouldReturn200WhenAdminDisablesUser() throws Exception {

        when(userService.updateUserStatus(USER_ID, UserStatus.DISABLED))
            .thenReturn(userResponse);

        mockMvc.perform(patch(userUrl(USER_ID) + "/status")
                .with(jwtWithRole(ADMIN_SUB, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(DISABLED_STATUS_JSON))
            .andExpect(status().isOk());

        verify(userService).updateUserStatus(USER_ID, UserStatus.DISABLED);
    }

    @Test
    void updateUserStatus_shouldReturn400WhenStatusIsInvalid() throws Exception {

        String invalidRequest = """
            {
                "status": "DELETED"
            }
            """;

        mockMvc.perform(patch(userUrl(USER_ID) + "/status")
                .with(jwtWithRole(ADMIN_SUB, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void updateUserStatus_shouldReturn403WhenStudentIsAuthenticated() throws Exception {

        mockMvc.perform(patch(userUrl(USER_ID) + "/status")
                .with(jwtWithRole(STUDENT_SUB, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ACTIVE_STATUS_JSON))
            .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void updateUserStatus_shouldReturn403WhenInstructorIsAuthenticated() throws Exception {

        mockMvc.perform(patch(userUrl(USER_ID) + "/status")
                .with(jwtWithRole(INSTRUCTOR_SUB, "INSTRUCTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ACTIVE_STATUS_JSON))
            .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void updateUserStatus_shouldReturn401WhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(patch(userUrl(USER_ID) + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ACTIVE_STATUS_JSON))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    // DELETE USER

    @Test
    void deleteUser_shouldReturn204WhenAdminIsAuthenticated() throws Exception {

        mockMvc.perform(delete(userUrl(USER_ID))
                .with(jwtWithRole(ADMIN_SUB, "ADMIN")))
            .andExpect(status().isNoContent());

        verify(userService).deleteUser(USER_ID);
    }

    @Test
    void deleteUser_shouldReturn403WhenStudentIsAuthenticated() throws Exception {

        mockMvc.perform(delete(userUrl(USER_ID))
                .with(jwtWithRole(STUDENT_SUB, "STUDENT")))
            .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void deleteUser_shouldReturn403WhenInstructorIsAuthenticated() throws Exception {

        mockMvc.perform(delete(userUrl(USER_ID))
                .with(jwtWithRole(INSTRUCTOR_SUB, "INSTRUCTOR")))
            .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void deleteUser_shouldReturn401WhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(delete(userUrl(USER_ID)))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }
}
