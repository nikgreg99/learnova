package com.learnova.controller;

import com.learnova.dto.user.UserRequest;
import com.learnova.dto.user.UserResponse;
import com.learnova.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserRequest request) {

        UserResponse user = userService.createUser(request);

        URI location = URI.create("/api/users/" + user.id());

        return ResponseEntity
                .created(location)
                .body(user);
    }


}
