package com.jobpulse.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "password_reset_tokens",
    indexes = {
      @Index(name = "idx_reset_tokens_token", columnList = "token"),
      @Index(name = "idx_reset_tokens_user_id", columnList = "user_id"),
      @Index(name = "idx_reset_tokens_expires_at", columnList = "expires_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  private Instant expiresAt;

  private Instant usedAt;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  public boolean isUsed() {
    return usedAt != null;
  }

  public boolean isValid() {
    return !isExpired() && !isUsed();
  }
}
