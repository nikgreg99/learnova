package com.learnova.controller;


import com.learnova.dto.user.UserRequest;
import com.learnova.dto.user.UserProfileUpdateRequest;
import com.learnova.dto.user.UserResponse;
import com.learnova.config.security.SecurityConfig;
import com.learnova.exception.GlobalMessageHandler;
import com.learnova.exception.UserDeletedException;
import com.learnova.exception.UserNotFoundException;
import com.learnova.exception.UserProfileConflictException;
import com.learnova.model.UserStatus;
import com.learnova.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({GlobalMessageHandler.class, SecurityConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private MessageSource messageSource;

    private UserResponse createUserResponse(UserStatus status) {
        return new UserResponse(
                1L,
                "Mario",
                "Rossi",
                "mario.rossi@example.com",
                "mrossi",
                Set.of("STUDENT"),
                status,
                null,
                null
        );
    }

    // CREATE USER

    @Test
    void createUser_shouldReturn201Created() throws Exception {
        UserRequest request = new UserRequest(
                "Mario",
                "Rossi",
                "mario.rossi@example.com",
                "mrossi",
                Set.of("STUDENT"),
                "SecurePassword123!"
        );

        UserResponse response = createUserResponse(UserStatus.ACTIVE);

        when(userService.createUser(any(UserRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/users/1"
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Mario"))
                .andExpect(jsonPath("$.lastName").value("Rossi"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService).createUser(any(UserRequest.class));
    }

    @Test
    void getUsers_shouldReturnPagedUsers() throws Exception {
        UserResponse response = createUserResponse(UserStatus.ACTIVE);
        when(userService.getUsers(any()))
                .thenReturn(new PageImpl<>(java.util.List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "20")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.number").value(0));

        verify(userService).getUsers(any());
    }

    @Test
    void getUser_shouldReturnUserById() throws Exception {
        when(userService.getUser(1L)).thenReturn(createUserResponse(UserStatus.DISABLED));

        mockMvc.perform(get("/api/v1/users/1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DISABLED"));

        verify(userService).getUser(1L);
    }

    @Test
    void getUser_shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.getUser(99L)).thenThrow(new UserNotFoundException(99L));
        when(messageSource.getMessage(
                eq("user.not-found"),
                any(Object[].class),
                any(java.util.Locale.class)
        )).thenReturn("User with id 99 was not found.");

        mockMvc.perform(get("/api/v1/users/99").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User with id 99 was not found."));
    }

    @Test
    void updateUserProfile_shouldReturnUpdatedUser() throws Exception {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "Mario",
                "Bianchi",
                "mario.bianchi@example.com",
                "mbianchi"
        );
        UserResponse response = new UserResponse(
                1L,
                "Mario",
                "Bianchi",
                "mario.bianchi@example.com",
                "mbianchi",
                Set.of("STUDENT"),
                UserStatus.ACTIVE,
                null,
                null
        );

        when(userService.updateUserProfile(eq(1L), any(UserProfileUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request))
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Mario"))
                .andExpect(jsonPath("$.lastName").value("Bianchi"))
                .andExpect(jsonPath("$.email").value("mario.bianchi@example.com"))
                .andExpect(jsonPath("$.username").value("mbianchi"));

        verify(userService).updateUserProfile(eq(1L), any(UserProfileUpdateRequest.class));
    }

    @Test
    void updateUserProfile_shouldReturn409WhenEmailOrUsernameIsTaken() throws Exception {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "Mario",
                "Rossi",
                "taken@example.com",
                "mrossi"
        );
        when(userService.updateUserProfile(eq(1L), any(UserProfileUpdateRequest.class)))
                .thenThrow(new UserProfileConflictException("email"));
        when(messageSource.getMessage(
                eq("user.profile-conflict"),
                any(Object[].class),
                any(java.util.Locale.class)
        )).thenReturn("The email is already in use.");

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request))
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("The email is already in use."));
    }

    // UPDATE USER STATUS

    @Test
    void updateUserStatus_shouldDisableUser() throws Exception {
        UserResponse response = createUserResponse(UserStatus.DISABLED);

        when(userService.updateUserStatus(1L, UserStatus.DISABLED))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DISABLED"));

        verify(userService).updateUserStatus(1L, UserStatus.DISABLED);
    }

    @Test
    void updateUserStatus_shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.updateUserStatus(99L, UserStatus.DISABLED))
                .thenThrow(new UserNotFoundException(99L));

        when(messageSource.getMessage(
                eq("user.not-found"),
                any(Object[].class),
                any(java.util.Locale.class)
        )).thenReturn("User with id 99 was not found.");

        mockMvc.perform(patch("/api/v1/users/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}")
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User with id 99 was not found."));

        verify(userService).updateUserStatus(99L, UserStatus.DISABLED);
    }

    @Test
    void updateUserStatus_shouldEnableUser() throws Exception {
        UserResponse response = createUserResponse(UserStatus.ACTIVE);

        when(userService.updateUserStatus(1L, UserStatus.ACTIVE))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService).updateUserStatus(1L, UserStatus.ACTIVE);
    }

    @Test
    void updateUserStatus_shouldReturn409WhenUserIsDeleted() throws Exception {
        when(userService.updateUserStatus(1L, UserStatus.ACTIVE))
                .thenThrow(new UserDeletedException(1L));

        when(messageSource.getMessage(
                eq("user.deleted"),
                any(Object[].class),
                any(java.util.Locale.class)
        )).thenReturn("User with id 1 has been deleted.");

        mockMvc.perform(patch("/api/v1/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}")
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("User with id 1 has been deleted."));

        verify(userService).updateUserStatus(1L, UserStatus.ACTIVE);
    }

    @Test
    void updateUserStatus_shouldRejectUnsupportedStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELETED\"}")
                        .with(jwt()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    // DELETE USER

    @Test
    void deleteUser_shouldReturn204NoContent() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/v1/users/1")
                        .with(jwt()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userService).deleteUser(1L);
    }

    @Test
    void deleteUser_shouldReturn404WhenUserNotFound() throws Exception {
        doThrow(new UserNotFoundException(99L))
                .when(userService).deleteUser(99L);

        when(messageSource.getMessage(
                eq("user.not-found"),
                any(Object[].class),
                any(java.util.Locale.class)
        )).thenReturn("User with id 99 was not found.");

        mockMvc.perform(delete("/api/v1/users/99")
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User with id 99 was not found."));

        verify(userService).deleteUser(99L);
    }
}
