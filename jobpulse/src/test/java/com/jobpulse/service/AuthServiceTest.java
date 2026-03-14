package com.jobpulse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.jobpulse.config.JwtService;
import com.jobpulse.dto.request.LoginRequest;
import com.jobpulse.dto.request.RegisterRequest;
import com.jobpulse.dto.response.AuthResponse;
import com.jobpulse.exception.BadRequestException;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.model.Role;
import com.jobpulse.model.User;
import com.jobpulse.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpirationMs", 3600000L);
    }

    @Test
    void registerThrowsWhenEmailOrUsernameExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("amine@example.com")
                .username("amine")
                .password("secret")
                .build();

        when(userRepository.existsByEmail("amine@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void registerReturnsAuthResponseWhenValid() {
        RegisterRequest request = RegisterRequest.builder()
                .email("amine@example.com")
                .username("amine")
                .password("secret")
                .build();

        User savedUser = User.builder()
                .id(1L)
                .email("amine@example.com")
                .username("amine")
                .password("encoded")
                .role(Role.USER)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateToken("amine")).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        assertEquals("amine", response.getUser().getUsername());
    }

    @Test
    void loginReturnsAuthResponseWhenCredentialsValid() {
        LoginRequest request = LoginRequest.builder()
                .username("amine")
                .password("secret")
                .build();

        User principal = User.builder()
                .id(2L)
                .username("amine")
                .email("amine@example.com")
                .role(Role.USER)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(tokenProvider.generateToken("amine")).thenReturn("login-jwt");

        AuthResponse response = authService.login(request);

        assertEquals("login-jwt", response.getAccessToken());
        assertEquals("amine@example.com", response.getUser().getEmail());
    }

    @Test
    void getCurrentUserThrowsWhenUserNotFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.getCurrentUser("missing"));
    }

    @Test
    void getCurrentUserReturnsMappedResponse() {
        User user = User.builder()
                .id(3L)
                .username("amine")
                .email("amine@example.com")
                .role(Role.USER)
                .build();

        when(userRepository.findByUsername(eq("amine"))).thenReturn(Optional.of(user));

        AuthResponse.UserResponse response = authService.getCurrentUser("amine");

        assertEquals(3L, response.getId());
        assertEquals("amine@example.com", response.getEmail());
        assertEquals("USER", response.getRole());
    }
}
