package com.jobpulse.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobpulse.config.JwtService;
import com.jobpulse.dto.request.LoginRequest;
import com.jobpulse.dto.request.RegisterRequest;
import com.jobpulse.dto.response.AuthResponse;
import com.jobpulse.exception.BadRequestException;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.model.Role;
import com.jobpulse.model.User;
import com.jobpulse.repository.UserRepository;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()) || userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Email already in use");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .role(Role.USER)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();
        log.info("User logged in: {}", user.getEmail());

        return createAuthResponse(user);
    }



    // @Transactional
    // public void logout(RefreshTokenRequest request) {
    //     RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
    //             .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));
        
    //     refreshTokenRepository.delete(refreshToken);
    //     log.info("User logged out");
    // }

    public AuthResponse.UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return AuthResponse.UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = tokenProvider.generateToken(user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationMs / 1000)
                .user(AuthResponse.UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}
