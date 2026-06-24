package com.jobpulse.repository;

import com.jobpulse.model.AuthProvider;
import com.jobpulse.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByUsername(String username);

  Optional<User> findByIdentities_ProviderAndIdentities_ProviderId(
      AuthProvider provider, String providerId);
}
