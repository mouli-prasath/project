package com.tripplanner.service;

import com.tripplanner.dto.auth.AuthResponse;
import com.tripplanner.dto.auth.LoginRequest;
import com.tripplanner.dto.auth.RegisterRequest;
import com.tripplanner.dto.auth.UserResponse;
import com.tripplanner.entity.User;
import com.tripplanner.exception.DuplicateResourceException;
import com.tripplanner.repository.UserRepository;
import com.tripplanner.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        User saved = userRepository.save(user);

        String token = jwtUtil.generateToken(saved.getEmail(), saved.getId());
        return AuthResponse.of(token, UserResponse.from(saved));
    }

    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException (handled globally) on failure.
        // Deliberately does not distinguish "no such user" from "wrong password".
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid credentials"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
        return AuthResponse.of(token, UserResponse.from(user));
    }

    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.tripplanner.exception.ResourceNotFoundException("User not found"));
        return UserResponse.from(user);
    }
}
