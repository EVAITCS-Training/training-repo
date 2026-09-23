package com.evaitcs.securebank.auth.web;

import com.evaitcs.securebank.auth.security.AuthService;
import com.evaitcs.securebank.auth.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * POST /auth/login — reached publicly as POST /api/auth/login through the
 * gateway (StripPrefix removes /api). Path shape is IDENTICAL to the
 * monolith's /api/auth/login after stripping, which is why the stage-3
 * frontend works against both architectures untouched.
 *
 * DTOs are nested records in the controller: at this service's size,
 * one-file-per-record ceremony adds no clarity. (Stage 2 kept them separate —
 * both styles are defensible; consistency within a codebase is the real rule.)
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    public record LoginRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 8, max = 100) String password) {
    }

    public record LoginResponse(String token, String tokenType, Instant expiresAt,
                                String username, String role, Long customerId) {
    }

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result;
        try {
            result = authService.login(request.username(), request.password());
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(401).build(); // generic — no oracle
        }
        return ResponseEntity.ok(new LoginResponse(result.token(), "Bearer",
                jwtService.expiresAfter(), result.username(), result.role(), result.customerId()));
    }
}
