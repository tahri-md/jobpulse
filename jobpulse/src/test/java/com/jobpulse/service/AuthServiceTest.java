package com.jobpulse.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jobpulse.config.JwtService;
import com.jobpulse.dto.request.LoginRequest;
import com.jobpulse.dto.request.RefreshTokenRequest;
import com.jobpulse.dto.request.RegisterRequest;
import com.jobpulse.dto.response.AuthResponse;
import com.jobpulse.dto.response.UserDto;
import com.jobpulse.exception.BadRequestException;
import com.jobpulse.model.EmailVerificationToken;
import com.jobpulse.model.Role;
import com.jobpulse.model.User;
import com.jobpulse.repository.EmailVerificationTokenRepository;
import com.jobpulse.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService tokenProvider;
  @Mock private EmailService emailService;
  @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;

  @InjectMocks private AuthService authService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(authService, "accessTokenExpirationMs", 3600000L);
    ReflectionTestUtils.setField(authService, "emailVerificationExpiryHours", 24L);
  }

  // ─────────────────────────────────────────────────────────────────
  // register
  // ─────────────────────────────────────────────────────────────────

  @Nested
  class Register {

    private RegisterRequest validRequest() {
      return RegisterRequest.builder()
          .email("tahri@example.com")
          .username("tahri")
          .password("Secret123!")
          .passwordConfirmation("Secret123!")
          .build();
    }

    @Test
    void validRequest_savesUserAndReturnsDto() {
      RegisterRequest req = validRequest();
      User saved =
          User.builder()
              .id(UUID.randomUUID())
              .username("tahri")
              .email("tahri@example.com")
              .role(Role.USER)
              .identities(List.of())
              .build();

      when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
      when(userRepository.existsByUsername(req.getUsername())).thenReturn(false);
      when(passwordEncoder.encode(req.getPassword())).thenReturn("encoded");
      when(userRepository.save(any(User.class))).thenReturn(saved);

      UserDto result = authService.register(req);

      assertThat(result.getUsername()).isEqualTo("tahri");
      assertThat(result.getEmail()).isEqualTo("tahri@example.com");
      verify(emailService).sendEmailVerification(eq("tahri@example.com"), eq("tahri"), anyString());
    }

    @Test
    void passwordMismatch_throwsBadRequest() {
      RegisterRequest req =
          RegisterRequest.builder()
              .email("a@b.com")
              .username("a")
              .password("Secret123!")
              .passwordConfirmation("different")
              .build();

      assertThatThrownBy(() -> authService.register(req))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("identical");
    }

    @Test
    void emailAlreadyExists_throwsBadRequest() {
      RegisterRequest req = validRequest();
      when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

      assertThatThrownBy(() -> authService.register(req))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Email");
    }

    @Test
    void usernameAlreadyExists_throwsBadRequest() {
      RegisterRequest req = validRequest();
      when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
      when(userRepository.existsByUsername(req.getUsername())).thenReturn(true);

      assertThatThrownBy(() -> authService.register(req))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Username");
    }

    @Test
    void validRequest_userSavedWithCorrectFields() {
      RegisterRequest req = validRequest();
      User saved =
          User.builder()
              .id(UUID.randomUUID())
              .username("tahri")
              .email("tahri@example.com")
              .role(Role.USER)
              .identities(List.of())
              .build();

      when(userRepository.existsByEmail(any())).thenReturn(false);
      when(userRepository.existsByUsername(any())).thenReturn(false);
      when(passwordEncoder.encode(any())).thenReturn("encoded");
      when(userRepository.save(any())).thenReturn(saved);

      authService.register(req);

      ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(captor.capture());
      assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded");
      assertThat(captor.getValue().getRole()).isEqualTo(Role.USER);
      assertThat(captor.getValue().isEnabled()).isTrue();
    }
  }

  // ─────────────────────────────────────────────────────────────────
  // login
  // ─────────────────────────────────────────────────────────────────

  @Nested
  class Login {

    private User verifiedUser() {
      return User.builder()
          .id(UUID.randomUUID())
          .username("tahri")
          .email("tahri@example.com")
          .passwordHash("encoded")
          .role(Role.USER)
          .isEnabled(true)
          .isEmailVerified(true)
          .isAccountLocked(false)
          .identities(List.of())
          .build();
    }

    @Test
    void validCredentials_returnsAuthResponse() {
      User user = verifiedUser();
      LoginRequest req = LoginRequest.builder().username("tahri").password("Secret123!").build();

      when(userRepository.findByUsername("tahri")).thenReturn(Optional.of(user));
      when(passwordEncoder.matches("Secret123!", "encoded")).thenReturn(true);
      when(userRepository.save(any())).thenReturn(user);
      when(tokenProvider.generateAccessTokenFromUserId(any(), any())).thenReturn("access-token");
      when(tokenProvider.generateRefreshTokenFromUserId(any())).thenReturn("refresh-token");

      AuthResponse result = authService.login(req);

      assertThat(result.getAccessToken()).isEqualTo("access-token");
      assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
      assertThat(result.getTokenType()).isEqualTo("Bearer");
      assertThat(result.getUser().getUsername()).isEqualTo("tahri");
    }

    @Test
    void wrongPassword_throwsRuntimeException() {
      User user = verifiedUser();
      when(userRepository.findByUsername("tahri")).thenReturn(Optional.of(user));
      when(passwordEncoder.matches(any(), any())).thenReturn(false);

      assertThatThrownBy(
              () ->
                  authService.login(
                      LoginRequest.builder().username("tahri").password("wrong").build()))
          .isInstanceOf(RuntimeException.class);
    }

    @Test
    void userNotFound_throwsRuntimeException() {
      when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  authService.login(LoginRequest.builder().username("ghost").password("x").build()))
          .isInstanceOf(RuntimeException.class);
    }

    @Test
    void emailNotVerified_throwsRuntimeException() {
      User user = verifiedUser();
      user.setEmailVerified(false);
      when(userRepository.findByUsername("tahri")).thenReturn(Optional.of(user));

      assertThatThrownBy(
              () ->
                  authService.login(LoginRequest.builder().username("tahri").password("x").build()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("verified");
    }

    @Test
    void accountLocked_throwsRuntimeException() {
      User user = verifiedUser();
      user.setAccountLocked(true);
      when(userRepository.findByUsername("tahri")).thenReturn(Optional.of(user));

      assertThatThrownBy(
              () ->
                  authService.login(LoginRequest.builder().username("tahri").password("x").build()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Locked");
    }

    @Test
    void accountDisabled_throwsRuntimeException() {
      User user = verifiedUser();
      user.setEnabled(false);
      when(userRepository.findByUsername("tahri")).thenReturn(Optional.of(user));

      assertThatThrownBy(
              () ->
                  authService.login(LoginRequest.builder().username("tahri").password("x").build()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Enabled");
    }

    @Test
    void successfulLogin_updatesLastLoginAt() {
      User user = verifiedUser();
      when(userRepository.findByUsername("tahri")).thenReturn(Optional.of(user));
      when(passwordEncoder.matches(any(), any())).thenReturn(true);
      when(userRepository.save(any())).thenReturn(user);
      when(tokenProvider.generateAccessTokenFromUserId(any(), any())).thenReturn("t");
      when(tokenProvider.generateRefreshTokenFromUserId(any())).thenReturn("r");

      authService.login(LoginRequest.builder().username("tahri").password("p").build());

      assertThat(user.getLastLoginAt()).isNotNull();
    }
  }

  // ─────────────────────────────────────────────────────────────────
  // refreshToken
  // ─────────────────────────────────────────────────────────────────

  @Nested
  class RefreshToken {

    @Test
    void validRefreshToken_returnsNewAccessToken() {
      UUID userId = UUID.randomUUID();
      User user = User.builder().id(userId).role(Role.USER).identities(List.of()).build();

      when(tokenProvider.validateToken("valid-refresh")).thenReturn(true);
      when(tokenProvider.getUserIdFromToken("valid-refresh")).thenReturn(userId);
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(tokenProvider.generateAccessTokenFromUserId(any(), any())).thenReturn("new-access");

      AuthResponse result =
          authService.refreshToken(
              RefreshTokenRequest.builder().refreshToken("valid-refresh").build());

      assertThat(result.getAccessToken()).isEqualTo("new-access");
      assertThat(result.getRefreshToken()).isEqualTo("valid-refresh");
    }

    @Test
    void invalidRefreshToken_throwsRuntimeException() {
      when(tokenProvider.validateToken("bad-token")).thenReturn(false);

      assertThatThrownBy(
              () ->
                  authService.refreshToken(
                      RefreshTokenRequest.builder().refreshToken("bad-token").build()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Invalid");
    }
  }

  // ─────────────────────────────────────────────────────────────────
  // verifyEmail
  // ─────────────────────────────────────────────────────────────────

  @Nested
  class VerifyEmail {

    @Test
    void validToken_setsEmailVerified() {
      User user = User.builder().id(UUID.randomUUID()).build();
      EmailVerificationToken token =
          EmailVerificationToken.builder()
              .token("abc123")
              .user(user)
              .expiresAt(java.time.LocalDateTime.now().plusHours(1))
              .build();

      when(emailVerificationTokenRepository.findByToken("abc123")).thenReturn(Optional.of(token));
      when(userRepository.save(any())).thenReturn(user);

      authService.verifyEmail("abc123");

      assertThat(user.isEmailVerified()).isTrue();
      assertThat(user.getEmailVerifiedAt()).isNotNull();
      verify(emailVerificationTokenRepository).delete(token);
    }

    @Test
    void tokenNotFound_throwsRuntimeException() {
      when(emailVerificationTokenRepository.findByToken("bad")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> authService.verifyEmail("bad"))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Invalid");
    }

    @Test
    void expiredToken_throwsRuntimeException() {
      User user = User.builder().id(UUID.randomUUID()).build();
      EmailVerificationToken token =
          EmailVerificationToken.builder()
              .token("expired")
              .user(user)
              .expiresAt(java.time.LocalDateTime.now().minusHours(1)) // already expired
              .build();

      when(emailVerificationTokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

      assertThatThrownBy(() -> authService.verifyEmail("expired"))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("expired");
    }
  }

  // ─────────────────────────────────────────────────────────────────
  // getCurrentUser
  // ─────────────────────────────────────────────────────────────────

  @Nested
  class GetCurrentUser {

    @Test
    void foundUser_returnsMappedDto() {
      UUID userId = UUID.randomUUID();
      User user =
          User.builder()
              .id(userId)
              .username("tahri")
              .email("tahri@example.com")
              .role(Role.USER)
              .identities(List.of())
              .build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      UserDto result = authService.getCurrentUser(userId);

      assertThat(result.getUsername()).isEqualTo("tahri");
      assertThat(result.getEmail()).isEqualTo("tahri@example.com");
      assertThat(result.getRole()).isEqualTo("USER");
    }

    @Test
    void userNotFound_throwsRuntimeException() {
      UUID id = UUID.randomUUID();
      when(userRepository.findById(id)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> authService.getCurrentUser(id))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("not found");
    }
  }
}
