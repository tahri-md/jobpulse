package com.jobpulse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobpulse.model.Job;
import com.jobpulse.model.User;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByOwner(User owner);

    Optional<Job> findByIdAndOwner(long id, User owner);
}
