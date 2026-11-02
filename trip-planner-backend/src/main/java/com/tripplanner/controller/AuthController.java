package com.tripplanner.controller;

import com.tripplanner.dto.auth.AuthResponse;
import com.tripplanner.dto.auth.LoginRequest;
import com.tripplanner.dto.auth.RegisterRequest;
import com.tripplanner.dto.auth.UserResponse;
import com.tripplanner.security.UserPrincipal;
import com.tripplanner.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, and current-user info")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal.getId()));
    }

    // Logout is intentionally not implemented server-side: with stateless JWTs
    // there is no server session to destroy. The frontend simply discards the
    // token. A token-blocklist (e.g. in Redis) is the standard upgrade if you
    // need immediate server-side revocation later.
}
