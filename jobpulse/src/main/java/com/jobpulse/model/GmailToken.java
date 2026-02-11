package com.jobpulse.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gmail_tokens", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class GmailToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** AES-encrypted access token */
    @Column(name = "access_token", nullable = false, length = 2048)
    private String accessToken;

    /** AES-encrypted refresh token */
    @Column(name = "refresh_token", nullable = false, length = 2048)
    private String refreshToken;

    /** Gmail address associated with the token */
    @Column(name = "gmail_address", nullable = false)
    private String gmailAddress;

    @Column(name = "token_expiry", nullable = false)
    private LocalDateTime tokenExpiry;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(tokenExpiry);
    }
}
