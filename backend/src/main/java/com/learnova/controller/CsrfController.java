package com.learnova.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CSRF", description = "Cross-Site Request Forgery protection operations")
@RestController
@RequestMapping("/api/auth")
public class CsrfController {

    @Operation(summary = "Get CSRF token", description = """
            Retrieves the CSRF token required for protected requests.
            Clients should include the token in the appropriate request
            header when performing state-changing operations.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSRF token retrieved successfully", content = @Content(schema = @Schema(implementation = CsrfToken.class)))
    })
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }
}
