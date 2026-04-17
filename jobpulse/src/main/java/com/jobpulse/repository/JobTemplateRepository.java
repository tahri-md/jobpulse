package com.jobpulse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobpulse.model.JobTemplate;
import com.jobpulse.model.User;

@Repository
public interface JobTemplateRepository extends JpaRepository<JobTemplate, Long> {
    List<JobTemplate> findByOwner(User owner);
    
    List<JobTemplate> findByOwnerOrIsPublicTrue(User owner, boolean isPublic);
    
    Optional<JobTemplate> findByIdAndOwner(long id, User owner);
}
