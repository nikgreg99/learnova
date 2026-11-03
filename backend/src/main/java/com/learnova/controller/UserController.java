package com.learnova.controller;

import com.learnova.dto.user.UserRequest;
import com.learnova.dto.user.UserResponse;
import com.learnova.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("/{id}/disable")
    public ResponseEntity<UserResponse> disableUser(@PathVariable Long id){
        return ResponseEntity.ok(userService.disableUser(id));
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<UserResponse> enableUser(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(userService.enableUser(id));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
