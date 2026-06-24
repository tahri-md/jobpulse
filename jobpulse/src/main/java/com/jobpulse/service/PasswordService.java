package com.jobpulse.service;

import com.jobpulse.dto.request.ChangePasswordRequest;
import com.jobpulse.dto.request.ForgotPasswordRequest;
import com.jobpulse.dto.request.ResetPasswordRequest;
import com.jobpulse.model.PasswordResetToken;
import com.jobpulse.model.User;
import com.jobpulse.repository.PasswordResetTokenRepository;
import com.jobpulse.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordService {

  private final UserRepository userRepository;

  private final PasswordResetTokenRepository passwordResetTokenRepository;

  private final PasswordEncoder passwordEncoder;

  private final EmailService emailService;

  private final PasswordValidationService passwordValidationService;

  @Value("${auth.email.password-reset-expiry-hours:1}")
  private int passwordResetExpiryHours;

  @Transactional
  public void forgotPassword(ForgotPasswordRequest request) {

    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(
                () -> {
                  return new RuntimeException(
                      "If email exists, you will receive password reset instructions");
                });

    String token = UUID.randomUUID().toString();
    Instant expiryTime = Instant.now().plus(passwordResetExpiryHours, ChronoUnit.HOURS);
    PasswordResetToken resetToken =
        PasswordResetToken.builder().user(user).token(token).expiresAt(expiryTime).build();

    passwordResetTokenRepository.save(resetToken);

    emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), token);
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {

    if (!request.getNewPassword().equals(request.getPasswordConfirmation())) {
      throw new RuntimeException("Passwords do not match");
    }

    PasswordResetToken resetToken =
        passwordResetTokenRepository
            .findByToken(request.getToken())
            .orElseThrow(() -> new RuntimeException("Invalid password reset token"));

    if (resetToken.isExpired()) {
      throw new RuntimeException("Password reset token has expired");
    }

    if (resetToken.isUsed()) {
      throw new RuntimeException("Password reset token has already been used");
    }

    User user = resetToken.getUser();

    String validationError = passwordValidationService.validatePassword(request.getNewPassword());
    if (validationError != null) {
      throw new RuntimeException(validationError);
    }

    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    user.setPasswordChangedAt(Instant.now());
    userRepository.save(user);

    resetToken.setUsedAt(Instant.now());
    passwordResetTokenRepository.save(resetToken);
  }

  @Transactional
  public void changePassword(UUID userId, ChangePasswordRequest request) {

    if (!request.getNewPassword().equals(request.getPasswordConfirmation())) {
      throw new RuntimeException("Passwords do not match");
    }

    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
      throw new RuntimeException("Current password is incorrect");
    }

    String validationError = passwordValidationService.validatePassword(request.getNewPassword());
    if (validationError != null) {
      throw new RuntimeException(validationError);
    }

    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    user.setPasswordChangedAt(Instant.now());
    userRepository.save(user);
  }
}
