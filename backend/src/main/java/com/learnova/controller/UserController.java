
package com.learnova.controller;

import com.learnova.dto.error.ApiError;
import com.learnova.dto.user.UserProfileUpdateRequest;
import com.learnova.dto.user.UserRequest;
import com.learnova.dto.user.UserResponse;
import com.learnova.dto.user.UserStatusRequest;
import com.learnova.model.UserStatus;
import com.learnova.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "User management operations")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create a user",
        description = "Creates a new user and returns the created resource.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload or validation failure",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
        @Valid @RequestBody UserRequest request) {

        UserResponse user = userService.createUser(request);
        URI location = URI.create("/api/v1/users/" + user.id());

        return ResponseEntity.created(location).body(user);
    }

    @Operation(summary = "Get current user profile",
        description = "Returns the profile of the currently authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Current user profile retrieved successfully")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(
        @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
            userService.getMyProfile(jwt.getSubject())
        );
    }

    @Operation(summary = "Update current user profile",
        description = "Updates the profile of the currently authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UserProfileUpdateRequest request) {

        UserResponse currentUser = userService.getMyProfile(jwt.getSubject());

        return ResponseEntity.ok(
            userService.updateUserProfile(currentUser.id(), request)
        );
    }

    @Operation(summary = "List users",
        description = "Returns a paginated list of all users, including disabled and deleted accounts.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Paginated user list")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PagedModel<UserResponse>> getUsers(
        @ParameterObject
        @PageableDefault(size = 20, sort = "createdAt",
            direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(
            new PagedModel<>(userService.getUsers(pageable))
        );
    }

    @Operation(summary = "Get user by ID",
        description = "Returns the profile of a user, including disabled and deleted accounts.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(
        @Parameter(description = "Unique ID of the user", example = "1")
        @PathVariable Long id) {

        return ResponseEntity.ok(userService.getUser(id));
    }

    @Operation(summary = "Update a user's profile",
        description = "Updates a user's profile in Learnova and Keycloak.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUserProfile(
        @PathVariable Long id,
        @Valid @RequestBody UserProfileUpdateRequest request) {

        return ResponseEntity.ok(
            userService.updateUserProfile(id, request)
        );
    }

    @Operation(summary = "Update a user's status",
        description = "Activates or disables an existing user in Learnova and Keycloak.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
        @PathVariable Long id,
        @Valid @RequestBody UserStatusRequest request) {

        UserStatus status = switch (request.status()) {
            case ACTIVE -> UserStatus.ACTIVE;
            case DISABLED -> UserStatus.DISABLED;
        };

        return ResponseEntity.ok(
            userService.updateUserStatus(id, status)
        );
    }

    @Operation(summary = "Delete a user",
        description = "Soft-deletes a user in Learnova and disables their Keycloak account.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {

        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }
}
