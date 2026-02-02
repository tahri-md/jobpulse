package com.jobpulse.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobpulse.config.JwtService;
import com.jobpulse.dto.request.GitHubOAuthRequest;
import com.jobpulse.dto.request.GoogleOAuthRequest;
import com.jobpulse.dto.response.AuthResponse;
import com.jobpulse.model.User;
import com.jobpulse.service.OAuthService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth/oauth")
@Validated
@Slf4j
public class OAuthController {

    @Autowired
    private OAuthService oauthService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleOAuth(@Valid @RequestBody GoogleOAuthRequest request) {
        log.info("Google OAuth request received");
        
        try {
            User user = oauthService.processGoogleOAuth(request.getToken());
            
            String accessToken = jwtService.generateToken(user.getUsername());
            
            AuthResponse response = AuthResponse.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .expiresIn((long) 3600)
                    .user(AuthResponse.UserResponse.builder()
                            .id(user.getId())
                            .email(user.getEmail())
                            .username(user.getUsername())
                            .role(user.getRole().name())
                            .build())
                    .build();
            
            log.info("Google OAuth successful for user: {}", user.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Google OAuth failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

 
    @PostMapping("/github")
    public ResponseEntity<AuthResponse> githubOAuth(@Valid @RequestBody GitHubOAuthRequest request) {
        log.info("GitHub OAuth request received");
        
        try {
            User user = oauthService.processGitHubOAuth(request.getCode());
            
            String accessToken = jwtService.generateToken(user.getUsername());
            
            AuthResponse response = AuthResponse.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .expiresIn((long)3600)
                    .user(AuthResponse.UserResponse.builder()
                            .id(user.getId())
                            .email(user.getEmail())
                            .username(user.getUsername())
                            .role(user.getRole().name())
                            .build())
                    .build();
            
            log.info("GitHub OAuth successful for user: {}", user.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("GitHub OAuth failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
