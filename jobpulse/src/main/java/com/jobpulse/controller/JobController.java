package com.jobpulse.controller;

import com.jobpulse.dto.request.BulkJobOperationDTO;
import com.jobpulse.dto.request.JobRequestDTO;
import com.jobpulse.dto.response.DeadLetterJobResponse;
import com.jobpulse.dto.response.JobHistoryResponse;
import com.jobpulse.dto.response.JobResponse;
import com.jobpulse.dto.response.JobStatsResponse;
import com.jobpulse.model.Status;
import com.jobpulse.service.JobService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jobs")
@Slf4j
@RequiredArgsConstructor
public class JobController {

  private final JobService jobService;

  @PostMapping("/full")
  @PreAuthorize("hasRole('ADMIN') or #request.jobType != T(com.jobpulse.dto.request.JobRequestDTO.JobType).SCRIPT")
  public ResponseEntity<JobResponse> createJobFull(
      @RequestBody JobRequestDTO request, @AuthenticationPrincipal UUID userId) {
    log.info("Creating full job: {} for user: {}", request.getName(), userId);
    request.setOwnerId(userId);
    jobService.createJobFull(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @GetMapping
  public ResponseEntity<List<JobResponse>> getAllJobs(@AuthenticationPrincipal UUID userId) {
    log.debug("Fetching jobs for user: {}", userId);
    return ResponseEntity.ok(jobService.getJobs(userId));
  }

  @GetMapping("/{id}")
  public ResponseEntity<JobResponse> getJob(
      @PathVariable long id, @AuthenticationPrincipal UUID userId) {
    log.debug("Fetching job with ID: {} for user: {}", id, userId);
    return ResponseEntity.ok(jobService.getJob(id, userId));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteJob(
      @PathVariable long id, @AuthenticationPrincipal UUID userId) {
    log.info("Deleting job with ID: {} for user: {}", id, userId);
    jobService.deleteJob(id, userId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/dead-letter/{id}/replay")
  public ResponseEntity<JobResponse> replayDeadJob(
      @PathVariable Long id, @AuthenticationPrincipal UUID userId) {
    log.info("Replaying dead letter job with ID: {} for user: {}", id, userId);
    return ResponseEntity.ok(jobService.replayDeadJob(id, userId));
  }

  @GetMapping("/dead-letter")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<DeadLetterJobResponse>> getDeadLetterJobs(
      @AuthenticationPrincipal UUID userId) {
    log.debug("Fetching dead letter jobs for user: {}", userId);
    return ResponseEntity.ok(jobService.getDeadLetterJobs(userId));
  }

  @GetMapping("/{id}/history")
  public ResponseEntity<List<JobHistoryResponse>> getJobHistory(
      @PathVariable long id, @AuthenticationPrincipal UUID userId) {
    log.debug("Fetching history for job with ID: {} for user: {}", id, userId);
    return ResponseEntity.ok(jobService.getJobHistory(id, userId));
  }

  @GetMapping("/stats")
  public ResponseEntity<JobStatsResponse> getJobStats(@AuthenticationPrincipal UUID userId) {
    log.debug("Fetching job statistics for user: {}", userId);
    return ResponseEntity.ok(jobService.getJobStats(userId));
  }

  @PutMapping("/{id}/pause")
  public ResponseEntity<JobResponse> pauseJob(
      @PathVariable long id, @AuthenticationPrincipal UUID userId) {
    log.info("Pausing job with ID: {} for user: {}", id, userId);
    return ResponseEntity.ok(jobService.pauseJob(id, userId));
  }

  @PutMapping("/{id}/resume")
  public ResponseEntity<JobResponse> resumeJob(
      @PathVariable long id, @AuthenticationPrincipal UUID userId) {
    log.info("Resuming job with ID: {} for user: {}", id, userId);
    return ResponseEntity.ok(jobService.resumeJob(id, userId));
  }

  @PostMapping("/bulk")
  public ResponseEntity<Void> bulkOperation(
      @RequestBody BulkJobOperationDTO request, @AuthenticationPrincipal UUID userId) {
    log.info(
        "Performing bulk operation: {} on {} jobs for user: {}",
        request.getOperation(),
        request.getJobIds().size(),
        userId);
    jobService.bulkOperation(request.getJobIds(), request.getOperation(), userId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/search")
  public ResponseEntity<List<JobResponse>> searchJobs(
      @RequestParam String query, @AuthenticationPrincipal UUID userId) {
    log.debug("Searching jobs with query: {} for user: {}", query, userId);
    return ResponseEntity.ok(jobService.searchJobs(query, userId));
  }

  @GetMapping("/filter/status")
  public ResponseEntity<List<JobResponse>> filterByStatus(
      @RequestParam Status status, @AuthenticationPrincipal UUID userId) {
    log.debug("Filtering jobs by status: {} for user: {}", status, userId);
    return ResponseEntity.ok(jobService.filterByStatus(status, userId));
  }

  @GetMapping("/filter/date-range")
  public ResponseEntity<List<JobResponse>> filterByDateRange(
      @RequestParam LocalDateTime startDate,
      @RequestParam LocalDateTime endDate,
      @AuthenticationPrincipal UUID userId) {
    log.debug("Filtering jobs by date range: {} to {} for user: {}", startDate, endDate, userId);
    return ResponseEntity.ok(jobService.filterByDateRange(startDate, endDate, userId));
  }
}