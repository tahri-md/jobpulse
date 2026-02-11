package com.jobpulse.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobpulse.model.DeadLetterJob;
import com.jobpulse.model.User;

@Repository
public interface DeadLetterJobRepository extends JpaRepository<DeadLetterJob, Long> {

    List<DeadLetterJob> findByJob_Owner(User owner);
}
