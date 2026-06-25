package com.jobpulse.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jobpulse.dto.request.ChangePasswordRequest;
import com.jobpulse.dto.request.ForgotPasswordRequest;
import com.jobpulse.dto.request.ResetPasswordRequest;
import com.jobpulse.model.PasswordResetToken;
import com.jobpulse.model.User;
import com.jobpulse.repository.PasswordResetTokenRepository;
import com.jobpulse.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

// ─────────────────────────────────────────────────────────────────
// PasswordValidationService
// ─────────────────────────────────────────────────────────────────

class PasswordValidationServiceTest {

  private final PasswordValidationService service = new PasswordValidationService();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "minLength", 12);
    ReflectionTestUtils.setField(service, "requireSpecialChars", true);
    ReflectionTestUtils.setField(service, "requireNumbers", true);
    ReflectionTestUtils.setField(service, "requireUppercase", true);
  }

  @Test
  void validPassword_returnsNull() {
    assertThat(service.validatePassword("StrongPass1!")).isNull();
  }

  @Test
  void nullPassword_returnsError() {
    assertThat(service.validatePassword(null)).isNotNull();
  }

  @Test
  void emptyPassword_returnsError() {
    assertThat(service.validatePassword("")).isNotNull();
  }

  @Test
  void tooShort_returnsError() {
    assertThat(service.validatePassword("Short1!")).contains("12");
  }

  @Test
  void noUppercase_returnsError() {
    assertThat(service.validatePassword("alllowercase1!")).contains("uppercase");
  }

  @Test
  void noNumber_returnsError() {
    assertThat(service.validatePassword("NoNumbers!!!!!")).contains("number");
  }

  @Test
  void noSpecialChar_returnsError() {
    assertThat(service.validatePassword("NoSpecialChar1")).contains("special");
  }

  @Test
  void isPasswordStrong_validPassword_returnsTrue() {
    assertThat(service.isPasswordStrong("StrongPass1!")).isTrue();
  }

  @Test
  void isPasswordStrong_weakPassword_returnsFalse() {
    assertThat(service.isPasswordStrong("weak")).isFalse();
  }
}

// ─────────────────────────────────────────────────────────────────
// PasswordService
// ─────────────────────────────────────────────────────────────────

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private EmailService emailService;
  @Mock private PasswordValidationService passwordValidationService;

  @InjectMocks private PasswordService passwordService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(passwordService, "passwordResetExpiryHours", 1);
  }

  @Nested
  class ForgotPassword {

    @Test
    void knownEmail_savesTokenAndSendsEmail() {
      User user =
          User.builder().id(UUID.randomUUID()).email("tahri@example.com").username("tahri").build();
      when(userRepository.findByEmail("tahri@example.com")).thenReturn(Optional.of(user));

      passwordService.forgotPassword(
          ForgotPasswordRequest.builder().email("tahri@example.com").build());

      verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
      verify(emailService)
          .sendPasswordResetEmail(eq("tahri@example.com"), eq("tahri"), anyString());
    }

    @Test
    void unknownEmail_throwsRuntimeException() {
      when(userRepository.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  passwordService.forgotPassword(
                      ForgotPasswordRequest.builder().email("ghost@x.com").build()))
          .isInstanceOf(RuntimeException.class);
    }
  }

  @Nested
  class ResetPassword {

    private User user;
    private PasswordResetToken validToken;

    @BeforeEach
    void setUp() {
      user = User.builder().id(UUID.randomUUID()).passwordHash("old-hash").build();
      validToken =
          PasswordResetToken.builder()
              .token("valid-token")
              .user(user)
              .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
              .build();
    }

    @Test
    void validRequest_updatesPasswordHash() {
      when(passwordResetTokenRepository.findByToken("valid-token"))
          .thenReturn(Optional.of(validToken));
      when(passwordValidationService.validatePassword("NewPass123!")).thenReturn(null);
      when(passwordEncoder.encode("NewPass123!")).thenReturn("new-hash");
      when(userRepository.save(any())).thenReturn(user);

      passwordService.resetPassword(
          ResetPasswordRequest.builder()
              .token("valid-token")
              .newPassword("NewPass123!")
              .passwordConfirmation("NewPass123!")
              .build());

      assertThat(user.getPasswordHash()).isEqualTo("new-hash");
      assertThat(user.getPasswordChangedAt()).isNotNull();
      verify(passwordResetTokenRepository).save(validToken);
      assertThat(validToken.getUsedAt()).isNotNull();
    }

    @Test
    void passwordMismatch_throwsRuntimeException() {
      assertThatThrownBy(
              () ->
                  passwordService.resetPassword(
                      ResetPasswordRequest.builder()
                          .token("t")
                          .newPassword("A")
                          .passwordConfirmation("B")
                          .build()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("match");
    }

    @Test
    void tokenNotFound_throwsRuntimeException() {
      when(passwordResetTokenRepository.findByToken("bad")).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  passwordService.resetPassword(
                      ResetPasswordRequest.builder()
                          .token("bad")
                          .newPassword("P")
                          .passwordConfirmation("P")
                          .build()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Invalid");
    }

    @Test
    void expiredToken_throwsRuntimeException() {
      validToken.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
      when(passwordResetTokenRepository.findByToken("valid-token"))
          .thenReturn(Optional.of(validToken));

      assertThatThrownBy(
              () ->
                  passwordService.resetPassword(
                      ResetPasswordRequest.builder()
                          .token("valid-token")
                          .newPassword("P")
                          .passwordConfirmation("P")
                          .build()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("expired");
    }

    @Test
    void alreadyUsedToken_throwsRuntimeException() {
      validToken.setUsedAt(Instant.now().minus(1, ChronoUnit.HOURS));
      when(passwordResetTokenRepository.findByToken("valid-token"))
          .thenReturn(Optional.of(validToken));

      assertThatThrownBy(
              () ->
                  passwordService.resetPassword(
                      ResetPasswordRequest.builder()
                          .token("valid-token")
                          .newPassword("P")
                          .passwordConfirmation("P")
                          .build()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("already been used");
    }

    @Test
    void weakPassword_throwsRuntimeException() {
      when(passwordResetTokenRepository.findByToken("valid-token"))
          .thenReturn(Optional.of(validToken));
      when(passwordValidationService.validatePassword("weak"))
          .thenReturn("Password must be at least 12 characters long");

      assertThatThrownBy(
              () ->
                  passwordService.resetPassword(
                      ResetPasswordRequest.builder()
                          .token("valid-token")
                          .newPassword("weak")
                          .passwordConfirmation("weak")
                          .build()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("12");
    }
  }

  @Nested
  class ChangePassword {

    private User user;

    @BeforeEach
    void setUp() {
      user = User.builder().id(UUID.randomUUID()).passwordHash("current-hash").build();
    }

    @Test
    void validRequest_updatesPassword() {
      when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
      when(passwordEncoder.matches("CurrentPass1!", "current-hash")).thenReturn(true);
      when(passwordValidationService.validatePassword("NewPass123!")).thenReturn(null);
      when(passwordEncoder.encode("NewPass123!")).thenReturn("new-hash");
      when(userRepository.save(any())).thenReturn(user);

      passwordService.changePassword(
          user.getId(),
          ChangePasswordRequest.builder()
              .currentPassword("CurrentPass1!")
              .newPassword("NewPass123!")
              .passwordConfirmation("NewPass123!")
              .build());

      assertThat(user.getPasswordHash()).isEqualTo("new-hash");
      assertThat(user.getPasswordChangedAt()).isNotNull();
    }

    @Test
    void passwordMismatch_throwsRuntimeException() {
      assertThatThrownBy(
              () ->
                  passwordService.changePassword(
                      user.getId(),
                      ChangePasswordRequest.builder()
                          .currentPassword("c")
                          .newPassword("A")
                          .passwordConfirmation("B")
                          .build()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("match");
    }

    @Test
    void wrongCurrentPassword_throwsRuntimeException() {
      when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
      when(passwordEncoder.matches("wrong", "current-hash")).thenReturn(false);

      assertThatThrownBy(
              () ->
                  passwordService.changePassword(
                      user.getId(),
                      ChangePasswordRequest.builder()
                          .currentPassword("wrong")
                          .newPassword("P")
                          .passwordConfirmation("P")
                          .build()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("incorrect");
    }

    @Test
    void userNotFound_throwsRuntimeException() {
      UUID id = UUID.randomUUID();
      when(userRepository.findById(id)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  passwordService.changePassword(
                      id,
                      ChangePasswordRequest.builder()
                          .currentPassword("c")
                          .newPassword("P")
                          .passwordConfirmation("P")
                          .build()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("not found");
    }
  }
}
