package com.jobpulse.config;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

  private JwtService jwtService;

  // Minimum 64 chars for HS512
  private static final String SECRET =
      "thisIsAVeryStrongJwtSecretKeyForTestingPurposesAtLeastSixtyFourCharsLong!!";

  @BeforeEach
  void setUp() {
    jwtService = new JwtService();
    ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
    ReflectionTestUtils.setField(jwtService, "jwtAccessTokenExpiration", 3600000L);
    ReflectionTestUtils.setField(jwtService, "jwtRefreshTokenExpiration", 86400000L);
    ReflectionTestUtils.setField(jwtService, "issuer", "jobpulse");
    ReflectionTestUtils.setField(jwtService, "audience", "jobpulse-client");
  }

  @Test
  void generateAccessToken_extractUserIdRoundTrip() {
    UUID userId = UUID.randomUUID();
    List<SimpleGrantedAuthority> roles = List.of(new SimpleGrantedAuthority("USER"));

    String token = jwtService.generateAccessTokenFromUserId(userId, roles);

    assertThat(token).isNotBlank();
    assertThat(jwtService.getUserIdFromToken(token)).isEqualTo(userId);
  }

  @Test
  void generateRefreshToken_subjectIsUserId() {
    UUID userId = UUID.randomUUID();

    String token = jwtService.generateRefreshTokenFromUserId(userId);

    assertThat(token).isNotBlank();
    UUID extractedId = jwtService.getUserIdFromToken(token);
    assertThat(extractedId).isEqualTo(userId);
  }

  @Test
  void getRolesFromToken_returnsCorrectRoles() {
    UUID userId = UUID.randomUUID();
    List<SimpleGrantedAuthority> roles = List.of(new SimpleGrantedAuthority("ADMIN"));

    String token = jwtService.generateAccessTokenFromUserId(userId, roles);
    List<String> extracted = jwtService.getRolesFromToken(token);

    assertThat(extracted).containsExactly("ADMIN");
  }

  @Test
  void validateToken_validToken_returnsTrue() {
    String token =
        jwtService.generateAccessTokenFromUserId(
            UUID.randomUUID(), List.of(new SimpleGrantedAuthority("USER")));

    assertThat(jwtService.validateToken(token)).isTrue();
  }

  @Test
  void validateToken_tamperedToken_returnsFalse() {
    String token =
        jwtService.generateAccessTokenFromUserId(
            UUID.randomUUID(), List.of(new SimpleGrantedAuthority("USER")));

    String tampered = token.substring(0, token.length() - 5) + "XXXXX";
    assertThat(jwtService.validateToken(tampered)).isFalse();
  }

  @Test
  void validateToken_randomString_returnsFalse() {
    assertThat(jwtService.validateToken("not.a.jwt")).isFalse();
  }

  @Test
  void validateToken_emptyString_returnsFalse() {
    assertThat(jwtService.validateToken("")).isFalse();
  }

  @Test
  void expiredToken_validateReturnsFalse() {
    // set expiration to -1ms (already expired)
    ReflectionTestUtils.setField(jwtService, "jwtAccessTokenExpiration", -1000L);

    String token =
        jwtService.generateAccessTokenFromUserId(
            UUID.randomUUID(), List.of(new SimpleGrantedAuthority("USER")));

    assertThat(jwtService.validateToken(token)).isFalse();
  }

  @Test
  void getClaimsFromToken_returnsNonNullClaims() {
    UUID userId = UUID.randomUUID();
    String token =
        jwtService.generateAccessTokenFromUserId(
            userId, List.of(new SimpleGrantedAuthority("USER")));

    var claims = jwtService.getClaimsFromToken(token);

    assertThat(claims).isNotNull();
    assertThat(claims.getSubject()).isEqualTo(userId.toString());
  }
}
