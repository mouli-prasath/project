package com.tripplanner.service;

import com.tripplanner.dto.auth.AuthResponse;
import com.tripplanner.dto.auth.LoginRequest;
import com.tripplanner.dto.auth.RegisterRequest;
import com.tripplanner.entity.User;
import com.tripplanner.exception.DuplicateResourceException;
import com.tripplanner.repository.UserRepository;
import com.tripplanner.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtUtil);
    }

    @Test
    void register_emailAlreadyExists_throwsDuplicateResourceException() {
        RegisterRequest request = new RegisterRequest("Alex", "alex@example.com", "password123");
        when(userRepository.existsByEmail("alex@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_newEmail_hashesPasswordAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("Alex", "alex@example.com", "password123");

        when(userRepository.existsByEmail("alex@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L); // simulate DB assigning an id on save
            return u;
        });
        when(jwtUtil.generateToken("alex@example.com", 1L)).thenReturn("mock-jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("mock-jwt-token", response.token());
        assertEquals("alex@example.com", response.user().email());
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void login_wrongPassword_propagatesBadCredentials() {
        LoginRequest request = new LoginRequest("alex@example.com", "wrong-password");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_validCredentials_returnsToken() {
        LoginRequest request = new LoginRequest("alex@example.com", "password123");
        User user = User.builder().id(1L).name("Alex").email("alex@example.com").build();

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("alex@example.com", 1L)).thenReturn("mock-jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("mock-jwt-token", response.token());
        assertEquals(1L, response.user().id());
    }
}
