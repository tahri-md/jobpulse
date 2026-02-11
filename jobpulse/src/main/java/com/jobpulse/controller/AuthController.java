package com.jobpulse.controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.jobpulse.dto.request.LoginRequest;
import com.jobpulse.dto.request.RegisterRequest;
import com.jobpulse.dto.response.AuthResponse;
import com.jobpulse.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private  AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

 

    // @PostMapping("/logout")
    // public ResponseEntity<Map<String, String>> logout(@RequestBody RefreshTokenRequest request) {
    //     authService.logout(request);
    //     return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    // }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getCurrentUser(userDetails.getUsername()));
    }
}
