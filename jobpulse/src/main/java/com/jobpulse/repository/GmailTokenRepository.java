package com.jobpulse.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobpulse.model.GmailToken;
import com.jobpulse.model.User;

@Repository
public interface GmailTokenRepository extends JpaRepository<GmailToken, Long> {

    Optional<GmailToken> findByUser(User user);

    Optional<GmailToken> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);
}
