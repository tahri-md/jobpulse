package com.jobpulse.controller;

import com.jobpulse.dto.request.ChangePasswordRequest;
import com.jobpulse.dto.request.ForgotPasswordRequest;
import com.jobpulse.dto.request.ResetPasswordRequest;
import com.jobpulse.dto.response.ApiResponse;
import com.jobpulse.service.PasswordService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/password")
@Slf4j
@RequiredArgsConstructor
public class PasswordController {

  private final PasswordService passwordService;

  @PostMapping("/forgot-password")
  public ResponseEntity<ApiResponse<Object>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {
    log.info("Forgot password request for: {}", request.getEmail());
    passwordService.forgotPassword(request);
    return ResponseEntity.ok(
        ApiResponse.ok("If email exists, you will receive password reset instructions"));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<ApiResponse<Object>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request) {
    log.info("Reset password request");
    passwordService.resetPassword(request);
    return ResponseEntity.ok(ApiResponse.ok("Password reset successfully"));
  }

  @PostMapping("/change-password")
  public ResponseEntity<ApiResponse<Object>> changePassword(
      @Valid @RequestBody ChangePasswordRequest request) {
    UUID userId = getCurrentUserId();
    log.info("Change password request for user: {}", userId);
    passwordService.changePassword(userId, request);
    return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
  }

  private UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("User not authenticated");
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof UUID) {
      return (UUID) principal;
    }
    if (principal instanceof String) {
      return UUID.fromString((String) principal);
    }
    throw new IllegalStateException(
        "Cannot extract user ID from principal: " + principal.getClass());
  }
}
