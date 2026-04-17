package com.jobpulse.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jobpulse.model.Job;
import com.jobpulse.model.Status;
import com.jobpulse.model.User;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByOwner(User owner);

    Optional<Job> findByIdAndOwner(long id, User owner);
    
    // Search and filtering methods
    List<Job> findByOwnerAndNameContainingIgnoreCase(User owner, String name);
    
    List<Job> findByOwnerAndStatus(User owner, Status status);
    
    @Query("SELECT j FROM Job j WHERE j.owner = :owner AND LOWER(j.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Job> searchByOwnerAndQuery(@Param("owner") User owner, @Param("query") String query);
    
    @Query("SELECT j FROM Job j WHERE j.owner = :owner AND j.createdAt >= :startDate AND j.createdAt <= :endDate")
    List<Job> findByOwnerAndDateRange(@Param("owner") User owner, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
