package com.learnova.controller;


import com.learnova.dto.user.UserRequest;
import com.learnova.dto.user.UserResponse;
import com.learnova.exception.GlobalMessageHandler;
import com.learnova.exception.UserDeletedException;
import com.learnova.exception.UserNotFoundException;
import com.learnova.model.UserStatus;
import com.learnova.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(GlobalMessageHandler.class)
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

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/users/1"
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Mario"))
                .andExpect(jsonPath("$.lastName").value("Rossi"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService).createUser(any(UserRequest.class));
    }

    // DISABLE USER

    @Test
    void disableUser_shouldReturn200Ok() throws Exception {
        UserResponse response = createUserResponse(UserStatus.DISABLED);

        when(userService.disableUser(1L))
                .thenReturn(response);

        mockMvc.perform(patch("/api/users/1/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DISABLED"));

        verify(userService).disableUser(1L);
    }

    @Test
    void disableUser_shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.disableUser(99L))
                .thenThrow(new UserNotFoundException(99L));

        when(messageSource.getMessage(
                eq("user.not-found"),
                any(Object[].class),
                any(java.util.Locale.class)
        )).thenReturn("User with id 99 was not found.");

        mockMvc.perform(patch("/api/users/99/disable"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User with id 99 was not found."));

        verify(userService).disableUser(99L);
    }

    // ENABLE USER

    @Test
    void enableUser_shouldReturn200Ok() throws Exception {
        UserResponse response = createUserResponse(UserStatus.ACTIVE);

        when(userService.enableUser(1L))
                .thenReturn(response);

        mockMvc.perform(patch("/api/users/1/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService).enableUser(1L);
    }

    @Test
    void enableUser_shouldReturn409WhenUserIsDeleted() throws Exception {
        when(userService.enableUser(1L))
                .thenThrow(new UserDeletedException(1L));

        when(messageSource.getMessage(
                eq("user.deleted"),
                any(Object[].class),
                any(java.util.Locale.class)
        )).thenReturn("User with id 1 has been deleted.");

        mockMvc.perform(patch("/api/users/1/enable"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("User with id 1 has been deleted."));

        verify(userService).enableUser(1L);
    }

    // DELETE USER

    @Test
    void deleteUser_shouldReturn204NoContent() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
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

        mockMvc.perform(delete("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User with id 99 was not found."));

        verify(userService).deleteUser(99L);
    }
}

