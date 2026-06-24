package com.jobpulse.service;

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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final EmailService emailService;
  private final EmailVerificationTokenRepository emailVerificationTokenRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService tokenProvider;

  @Value("${jwt.access-token-expiration}")
  private long accessTokenExpirationMs;

  @Value("${auth.email.verification-expiry-hours}")
  private long emailVerificationExpiryHours;

  @Transactional
  public UserDto register(RegisterRequest request) {
    if (!request.getPassword().equals(request.getPasswordConfirmation()))
      throw new BadRequestException("Password should be identical");

    if (userRepository.existsByEmail(request.getEmail()))
      throw new BadRequestException("Email already in use");

    if (userRepository.existsByUsername(request.getUsername()))
      throw new BadRequestException("Username already in use");

    User user =
        User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .isEnabled(true)
            .role(Role.USER)
            .build();

    user = userRepository.save(user);
    log.info("User registered: {}", user.getEmail());
    sendEmailVerification(user);
    return mapToUserDto(user);
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("Username not Found"));
    if (!user.isEmailVerified()) throw new RuntimeException("Email not verified");
    if (!user.isEnabled()) throw new RuntimeException("User not Enabled");
    if (user.isAccountLocked()) throw new RuntimeException("Account is Locked");

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new RuntimeException("Invalid email or password");
    }

    user.setLastLoginAt(Instant.now());

    user = userRepository.save(user);
    Set<String> roles = Set.of(user.getRole().name());
    String accessToken =
        tokenProvider.generateAccessTokenFromUserId(
            user.getId(),
            roles.stream()
                .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority(r))
                .collect(Collectors.toList()));
    String refreshToken = tokenProvider.generateRefreshTokenFromUserId(user.getId());

    return createAuthResponse(user, accessToken, refreshToken);
  }

  @Transactional
  public AuthResponse refreshToken(RefreshTokenRequest request) {
    log.info("Token refresh attempt");

    if (!tokenProvider.validateToken(request.getRefreshToken())) {
      throw new RuntimeException("Invalid or expired refresh token");
    }

    UUID userId = tokenProvider.getUserIdFromToken(request.getRefreshToken());
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    String newAccessToken =
        tokenProvider.generateAccessTokenFromUserId(
            user.getId(),
            Set.of(user.getRole().name()).stream()
                .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority(r))
                .collect(Collectors.toList()));

    return AuthResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(request.getRefreshToken())
        .expiresIn(900L)
        .build();
  }

  @Transactional
  public void verifyEmail(String token) {
    log.info("Email verification attempt");

    EmailVerificationToken verificationToken =
        emailVerificationTokenRepository
            .findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid verification token"));

    if (verificationToken.isExpired()) {
      throw new RuntimeException("Verification token has expired");
    }

    User user = verificationToken.getUser();
    user.setEmailVerified(true);
    user.setEmailVerifiedAt(Instant.now());
    userRepository.save(user);

    emailVerificationTokenRepository.delete(verificationToken);

    log.info("Email verified for user: {}", user.getId());
  }

  // @Transactional
  // public void logout(RefreshTokenRequest request) {
  // RefreshToken refreshToken =
  // refreshTokenRepository.findByToken(request.getRefreshToken())
  // .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

  // refreshTokenRepository.delete(refreshToken);
  // log.info("User logged out");
  // }

  @Transactional
  public void sendEmailVerification(User user) {
    String token = UUID.randomUUID().toString();
    LocalDateTime expiryTime = LocalDateTime.now().plusHours(emailVerificationExpiryHours);

    emailVerificationTokenRepository.deleteByUserId(user.getId());

    EmailVerificationToken verificationToken =
        EmailVerificationToken.builder().user(user).token(token).expiresAt(expiryTime).build();

    emailVerificationTokenRepository.save(verificationToken);

    emailService.sendEmailVerification(user.getEmail(), user.getUsername(), token);
  }

  @Transactional
  public UserDto getCurrentUser(UUID userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    return mapToUserDto(user);
  }

  private UserDto mapToUserDto(User user) {
    List<String> providers =
        user.getIdentities() == null || user.getIdentities().isEmpty()
            ? List.of("NATIVE")
            : user.getIdentities().stream().map(i -> i.getProvider().toString()).toList();

    return UserDto.builder()
        .username(user.getUsername())
        .email(user.getEmail())
        .authProviders(providers)
        .avatar(user.getAvatar())
        .lastLoginAt(user.getLastLoginAt())
        .role(user.getRole().toString())
        .build();
  }

  private AuthResponse createAuthResponse(User user, String accessToken, String refreshToken) {

    return AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(accessTokenExpirationMs / 1000)
        .user(mapToUserDto(user))
        .build();
  }
}
