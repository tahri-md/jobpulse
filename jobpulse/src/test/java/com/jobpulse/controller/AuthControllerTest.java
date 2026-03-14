package com.jobpulse.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jobpulse.config.CustomDetailsService;
import com.jobpulse.config.JwtAuthFilter;
import com.jobpulse.dto.response.AuthResponse;
import com.jobpulse.service.AuthService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

        @MockitoBean
    private AuthService authService;

        @MockitoBean
        private JwtAuthFilter jwtAuthFilter;

        @MockitoBean
        private CustomDetailsService customDetailsService;

    @Test
    void registerReturnsCreated() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(AuthResponse.UserResponse.builder()
                        .id(1L)
                        .email("amine@example.com")
                        .username("amine")
                        .role("USER")
                        .build())
                .build();

        given(authService.register(any())).willReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "amine@example.com",
                                  "username": "amine",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.user.username").value("amine"));
    }

    @Test
    void loginReturnsOk() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(AuthResponse.UserResponse.builder()
                        .id(2L)
                        .email("user@example.com")
                        .username("user")
                        .role("USER")
                        .build())
                .build();

        given(authService.login(any())).willReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "user",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

}
