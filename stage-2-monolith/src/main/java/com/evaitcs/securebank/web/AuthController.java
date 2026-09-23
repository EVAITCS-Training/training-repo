package com.evaitcs.securebank.web;

import com.evaitcs.securebank.security.AuthService;
import com.evaitcs.securebank.security.JwtService;
import com.evaitcs.securebank.web.dto.LoginRequest;
import com.evaitcs.securebank.web.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AUTH CONTROLLER — the only PUBLIC endpoint (see SecurityConfig permitAll).
 *
 * CONTROLLER LAYER RESPONSIBILITIES (and nothing more):
 *   1. bind JSON -> DTO            (@RequestBody)
 *   2. validate input              (@Valid -> Bean Validation -> 400 on failure)
 *   3. delegate to a service       (authService.login)
 *   4. map result -> HTTP response (200 + LoginResponse JSON)
 *
 * No business logic here. If you're tempted to write an if-statement about
 * balances or roles in a controller, it belongs in a service. This keeps
 * rules testable WITHOUT a web server (see TransferServiceTest).
 *
 * The springdoc @Tag/@Operation annotations feed the auto-generated OpenAPI
 * docs at /swagger-ui.html (Week 7: "docs that can't drift, because they're
 * generated from the code").
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "auth", description = "Login and token issuance")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange username+password for a Bearer token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result;
        try {
            result = authService.login(request.username(), request.password());
        } catch (UsernameNotFoundException e) {
            // Generic 401 — same status and message whether the username is
            // unknown or the password wrong (no enumeration oracle).
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new LoginResponse(
                result.token(), "Bearer", jwtService.expiresAfter(),
                result.username(), result.role(), result.customerId()));
    }
}
