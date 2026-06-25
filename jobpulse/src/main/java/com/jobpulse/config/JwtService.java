package com.jobpulse.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtService {

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${jwt.access-token-expiration}")
  private long jwtAccessTokenExpiration;

  @Value("${jwt.refresh-token-expiration}")
  private long jwtRefreshTokenExpiration;

  @Value("${jwt.issuer}")
  private String issuer;

  @Value("${jwt.audience}")
  private String audience;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(Authentication authentication) {
    return generateToken(authentication, jwtAccessTokenExpiration, false);
  }

  public String generateAccessTokenFromUserId(
      UUID userId, Collection<? extends GrantedAuthority> authorities) {
    return generateTokenFromUserId(userId, jwtAccessTokenExpiration, authorities, false);
  }

  public String generateRefreshToken(Authentication authentication) {
    return generateToken(authentication, jwtRefreshTokenExpiration, true);
  }

  public String generateRefreshTokenFromUserId(UUID userId) {
    return generateTokenFromUserId(
        userId, jwtRefreshTokenExpiration, Collections.emptyList(), true);
  }

  private String generateToken(
      Authentication authentication, long expirationTime, boolean isRefreshToken) {
    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    List<String> roles =
        authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());

    Map<String, Object> claims = new HashMap<>();
    claims.put("roles", roles);
    if (!isRefreshToken) {
      claims.put("username", authentication.getName());
    }

    return generateTokenWithClaims(claims, authentication.getName(), expirationTime);
  }

  private String generateTokenFromUserId(
      UUID userId,
      long expirationTime,
      Collection<? extends GrantedAuthority> authorities,
      boolean isRefreshToken) {
    Map<String, Object> claims = new HashMap<>();

    if (!isRefreshToken) {
      List<String> roles =
          authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
      claims.put("roles", roles);
      claims.put("userId", userId);
    }

    return generateTokenWithClaims(claims, userId.toString(), expirationTime);
  }

  private String generateTokenWithClaims(
      Map<String, Object> claims, String subject, long expirationTime) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expirationTime);

    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuer(issuer)
        .audience()
        .add(audience)
        .and()
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  public UUID getUserIdFromToken(String token) {
    Claims claims =
        Jwts.parser().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();

    Object userIdObj = claims.get("userId");

    if (userIdObj != null) {
      return UUID.fromString(userIdObj.toString());
    }

    try {
      return UUID.fromString(claims.getSubject());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public String getUsernameFromToken(String token) {
    Claims claims =
        Jwts.parser().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();

    return (String) claims.get("username");
  }

  @SuppressWarnings("unchecked")
  public List<String> getRolesFromToken(String token) {
    Claims claims =
        Jwts.parser().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();

    return (List<String>) claims.get("roles");
  }

 public boolean validateToken(String token) {
    try {
        Jwts.parser()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token);
        return true;
    } catch (JwtException | IllegalArgumentException ex) {
        log.warn("Invalid JWT token: {}", ex.getMessage());
        return false;
    }
}

  public Claims getClaimsFromToken(String token) {
    return Jwts.parser().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
  }
}
