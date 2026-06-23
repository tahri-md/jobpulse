package com.jobpulse.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobpulse.model.EmailVerificationToken;

@Repository
public interface EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationToken, UUID> {
  Optional<EmailVerificationToken> findByToken(String token);

  Optional<EmailVerificationToken> findByUserId(UUID userId);

  void deleteByUserId(UUID userId);
}
