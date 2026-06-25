package com.jobpulse.service;

import com.jobpulse.dto.others.JobFailureReason;
import com.jobpulse.dto.request.JobRequestDTO;
import com.jobpulse.dto.request.ScheduleDTO;
import com.jobpulse.dto.request.ScheduleDTO.ScheduleType;
import com.jobpulse.dto.response.DeadLetterJobResponse;
import com.jobpulse.dto.response.JobHistoryResponse;
import com.jobpulse.dto.response.JobResponse;
import com.jobpulse.dto.response.JobStatsResponse;
import com.jobpulse.exception.JobExecutionException;
import com.jobpulse.exception.NonRetryableJobException;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.model.DeadLetterJob;
import com.jobpulse.model.Job;
import com.jobpulse.model.JobHistory;
import com.jobpulse.model.Status;
import com.jobpulse.model.User;
import com.jobpulse.repository.DeadLetterJobRepository;
import com.jobpulse.repository.JobHistoryRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.UserRepository;
import com.jobpulse.util.CronExpressionUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobService {

  private final JobRepository jobRepository;
  private final UserRepository userRepository;
  private final RedisTemplate<String, String> redisTemplate;
  private final JobExecutorFactory executorFactory;
  private final DeadLetterJobRepository deadLetterJobRepository;
  private final JobHistoryRepository jobHistoryRepository;
  private final RetryPolicy retryPolicy;

  private User resolveUser(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
  }

  public void createJobFull(JobRequestDTO dto) {
    if (dto.getOwnerId() == null) throw new RuntimeException("ID cant be null");
    log.info("Owner Id: {}", dto.getOwnerId());

    log.info("Creating job: {}", dto.getName());
    Job job =
        Job.builder()
            .name(dto.getName())
            .jobType(dto.getJobType())
            .owner(resolveUser(dto.getOwnerId()))
            .maxRetries(dto.getMaxRetries())
            .retryCount(0)
            .status(Status.PENDING)
            .lastError(null)
            .payload(dto.getPayload())
            .build();

    ScheduleDTO scheduleDTO = dto.getSchedule();

    if (scheduleDTO.getType() == ScheduleType.ONE_TIME) {
      LocalDateTime runAt = scheduleDTO.getRunAt();
      if (runAt == null || runAt.isBefore(LocalDateTime.now())) {
        throw new IllegalArgumentException(
            "Invalid runAt time for ONE_TIME job. Must be in the future.");
      }
      job.setRecurring(false);
      job.setNextRunTime(runAt);
      job.setCronExpression(null);
      log.debug("Created ONE_TIME job scheduled for: {}", runAt);

    } else if (scheduleDTO.getType() == ScheduleType.RECURRING) {
      if (scheduleDTO.getFrequency() == null
          || scheduleDTO.getInterval() == null
          || scheduleDTO.getInterval() <= 0) {
        throw new IllegalArgumentException(
            "Invalid recurrence configuration. Frequency and interval are required and must be positive.");
      }
      job.setRecurring(true);
      String cronExpression =
          CronExpressionUtil.generateCronExpression(
              scheduleDTO.getFrequency().name(), scheduleDTO.getInterval());
      job.setCronExpression(cronExpression);
      job.setNextRunTime(CronExpressionUtil.getNextRunTime(cronExpression, LocalDateTime.now()));
      log.debug("Created RECUjoRRING job with cron: {}", cronExpression);

    } else if (scheduleDTO.getType() == ScheduleType.CRON) {
      if (scheduleDTO.getCronExpression() == null || scheduleDTO.getCronExpression().isBlank()) {
        throw new IllegalArgumentException("Cron expression required for CRON schedule type.");
      }
      CronExpressionUtil.validateCronExpression(scheduleDTO.getCronExpression());
      job.setRecurring(true);
      job.setCronExpression(scheduleDTO.getCronExpression());
      job.setNextRunTime(
          CronExpressionUtil.getNextRunTime(scheduleDTO.getCronExpression(), LocalDateTime.now()));
      log.debug("Created CRON job with expression: {}", scheduleDTO.getCronExpression());

    } else {
      throw new IllegalArgumentException("Unsupported schedule type: " + scheduleDTO.getType());
    }

    jobRepository.save(job);
    log.info("Job created successfully with ID: {}", job.getId());
  }

  @Scheduled(fixedRate = 10000)
  public void runDueJobs() throws Exception {
    log.debug("Checking for due jobs...");
    LocalDateTime now = LocalDateTime.now();
    List<Job> dueJobs = jobRepository.findDueJobs(now);
    log.info("Found {} due jobs to execute", dueJobs.size());

    for (Job job : dueJobs) {
      if (acquireLock(job)) {
        try {
          log.info("Executing job: {} (ID: {})", job.getName(), job.getId());
          executeJob(job);
        } catch (Exception e) {
          log.error(
              "Error executing job {} (ID: {}): {}", job.getName(), job.getId(), e.getMessage(), e);
        } finally {
          releaseLock(job);
          jobRepository.save(job);
        }
      } else {
        log.debug("Could not acquire lock for job: {} (ID: {})", job.getName(), job.getId());
      }
    }
  }

  private void executeJob(Job job) throws Exception {
    try {
      job.setStatus(Status.RUNNING);
      log.info("Starting execution of job: {} (ID: {})", job.getName(), job.getId());

      JobExecutor executor = executorFactory.get(job.getJobType());
      if (executor == null) {
        throw new NonRetryableJobException(
            JobFailureReason.INVALID_CONFIG, "No executor found for job type: " + job.getJobType());
      }

      executor.execute(job);

      job.setStatus(Status.SUCCESS);
      job.setRetryCount(0);
      job.setLastError(null);

      jobHistoryRepository.save(
          JobHistory.builder()
              .job(job)
              .status(Status.SUCCESS)
              .retryAttempt(0)
              .runTime(LocalDateTime.now())
              .build());

      job.setNextRunTime(calculateNextRun(job));
      log.info("Job executed successfully: {} (ID: {})", job.getName(), job.getId());

    } catch (JobExecutionException e) {
      log.error(
          "Job execution failed: {} - Reason: {}, Message: {}",
          job.getName(),
          e.getReason(),
          e.getMessage());

      JobFailureReason reason = e.getReason();
      boolean retryable = retryPolicy.isRetryable(reason);
      int nextRetry = job.getRetryCount() + 1;
      job.setRetryCount(nextRetry);

      if (retryable && nextRetry < job.getMaxRetries()) {
        long delayMinutes = (long) Math.pow(2, nextRetry);
        job.setStatus(Status.RETRYING);
        job.setNextRunTime(LocalDateTime.now().plusMinutes(delayMinutes));
        log.info(
            "Job scheduled for retry {} of {} with {} minute delay: {}",
            nextRetry,
            job.getMaxRetries(),
            delayMinutes,
            job.getName());
      } else {
        job.setStatus(Status.FAILED);
        job.setNextRunTime(null);
        log.warn(
            "Job failed and will not be retried: {} (Retryable: {}, Attempt: {} of {})",
            job.getName(),
            retryable,
            nextRetry,
            job.getMaxRetries());
        moveToDeadLetter(job);
      }

      job.setLastError(e.getMessage());
      jobHistoryRepository.save(
          JobHistory.builder()
              .job(job)
              .status(job.getStatus())
              .retryAttempt(nextRetry)
              .errorMessage(e.getMessage())
              .runTime(LocalDateTime.now())
              .build());
    }
  }

  private boolean acquireLock(Job job) {
    String key = "job-lock:" + job.getId();
    Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, "LOCKED", 5, TimeUnit.MINUTES);
    return locked != null && locked;
  }

  private void releaseLock(Job job) {
    redisTemplate.delete("job-lock:" + job.getId());
  }

  private void moveToDeadLetter(Job job) {
    jobRepository.save(job);
    deadLetterJobRepository.save(
        DeadLetterJob.builder()
            .job(job)
            .lastError(job.getLastError())
            .failedAt(LocalDateTime.now())
            .build());
    log.info("Job moved to dead letter queue: {} (ID: {})", job.getName(), job.getId());
  }

  private LocalDateTime calculateNextRun(Job job) {
    if (!job.isRecurring() || job.getCronExpression() == null) return null;
    try {
      return CronExpressionUtil.getNextRunTime(job.getCronExpression(), LocalDateTime.now());
    } catch (Exception e) {
      log.error("Error calculating next run time for job {}: {}", job.getId(), e.getMessage());
      return LocalDateTime.now().plusMinutes(5);
    }
  }

  public List<JobResponse> getJobs(UUID userId) {
    User user = resolveUser(userId);
    return jobRepository.findByOwner(user).stream()
        .filter(job -> job.getStatus() != Status.FAILED)
        .map(JobService::mapToJobResponse)
        .toList();
  }

  public JobResponse getJob(long id, UUID userId) {
    User user = resolveUser(userId);
    return mapToJobResponse(
        jobRepository
            .findByIdAndOwner(id, user)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found")));
  }

  public void deleteJob(long id, UUID userId) {
    User user = resolveUser(userId);
    Job job =
        jobRepository
            .findByIdAndOwner(id, user)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    deadLetterJobRepository.findByJob_Owner(user).stream()
        .filter(dl -> dl.getJob().getId() == id)
        .forEach(deadLetterJobRepository::delete);
    jobRepository.delete(job);
  }

  public JobResponse replayDeadJob(Long deadJobId, UUID userId) {
    log.info("Replaying dead letter job: {}", deadJobId);

    DeadLetterJob dlJob =
        deadLetterJobRepository
            .findById(deadJobId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Dead letter job not found with ID: " + deadJobId));

    if (dlJob.getJob().getOwner() == null || !dlJob.getJob().getOwner().getId().equals(userId)) {
      throw new ResourceNotFoundException("Dead letter job not found with ID: " + deadJobId);
    }

    Job originalJob = dlJob.getJob();
    originalJob.setRetryCount(0);
    originalJob.setStatus(Status.PENDING);
    originalJob.setLastError(null);

    if (originalJob.isRecurring() && originalJob.getCronExpression() != null) {
      try {
        originalJob.setNextRunTime(
            CronExpressionUtil.getNextRunTime(
                originalJob.getCronExpression(), LocalDateTime.now()));
      } catch (Exception e) {
        log.warn("Failed to parse cron expression, scheduling for immediate execution");
        originalJob.setNextRunTime(LocalDateTime.now().plusSeconds(1));
      }
    } else {
      originalJob.setNextRunTime(LocalDateTime.now().plusSeconds(1));
    }

    Job savedJob = jobRepository.save(originalJob);
    deadLetterJobRepository.delete(dlJob);
    log.info(
        "Dead letter job replayed successfully: {} (ID: {})",
        originalJob.getName(),
        savedJob.getId());
    return mapToJobResponse(savedJob);
  }

  public List<DeadLetterJobResponse> getDeadLetterJobs(UUID userId) {
    User user = resolveUser(userId);
    return deadLetterJobRepository.findByJob_Owner(user).stream()
        .map(
            dlj ->
                DeadLetterJobResponse.builder()
                    .id(dlj.getId())
                    .jobName(dlj.getJob().getName())
                    .jobType(
                        dlj.getJob().getJobType() != null ? dlj.getJob().getJobType().name() : null)
                    .lastError(dlj.getLastError())
                    .failedAt(dlj.getFailedAt())
                    .retryCount(dlj.getJob().getRetryCount())
                    .maxRetries(dlj.getJob().getMaxRetries())
                    .build())
        .toList();
  }

  public List<JobHistoryResponse> getJobHistory(long jobId, UUID userId) {
    User user = resolveUser(userId);
    Job job =
        jobRepository
            .findByIdAndOwner(jobId, user)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    return jobHistoryRepository.findByJobOrderByRunTimeDesc(job).stream()
        .map(
            jh ->
                JobHistoryResponse.builder()
                    .id(jh.getId())
                    .runTime(jh.getRunTime())
                    .status(jh.getStatus())
                    .errorMessage(jh.getErrorMessage())
                    .retryAttempt(jh.getRetryAttempt())
                    .build())
        .toList();
  }

  public JobStatsResponse getJobStats(UUID userId) {
    User user = resolveUser(userId);
    List<Job> allJobs = jobRepository.findByOwner(user);
    List<DeadLetterJob> deadLetterJobs = deadLetterJobRepository.findByJob_Owner(user);

    long pending = allJobs.stream().filter(j -> j.getStatus() == Status.PENDING).count();
    long running = allJobs.stream().filter(j -> j.getStatus() == Status.RUNNING).count();
    long success = allJobs.stream().filter(j -> j.getStatus() == Status.SUCCESS).count();
    long retrying = allJobs.stream().filter(j -> j.getStatus() == Status.RETRYING).count();
    long failed = allJobs.stream().filter(j -> j.getStatus() == Status.FAILED).count();

    return JobStatsResponse.builder()
        .totalJobs(allJobs.size())
        .pendingJobs(pending)
        .runningJobs(running)
        .successfulJobs(success)
        .retryingJobs(retrying)
        .failedJobs(failed)
        .deadLetterJobs(deadLetterJobs.size())
        .build();
  }

  public JobResponse pauseJob(long id, UUID userId) {
    User user = resolveUser(userId);
    Job job =
        jobRepository
            .findByIdAndOwner(id, user)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    job.setStatus(Status.PAUSED);
    return mapToJobResponse(jobRepository.save(job));
  }

  public JobResponse resumeJob(long id, UUID userId) {
    User user = resolveUser(userId);
    Job job =
        jobRepository
            .findByIdAndOwner(id, user)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    if (job.getStatus() == Status.FAILED) {
      throw new IllegalStateException(
          "Cannot resume a failed job. Use replay from the dead letter queue.");
    }
    job.setStatus(Status.PENDING);
    if (job.getNextRunTime() == null || job.getNextRunTime().isBefore(LocalDateTime.now())) {
      job.setNextRunTime(LocalDateTime.now().plusSeconds(10));
    }
    return mapToJobResponse(jobRepository.save(job));
  }

  public void bulkOperation(List<Long> jobIds, String operation, UUID userId) {
    User user = resolveUser(userId);
    List<Job> jobs =
        jobRepository.findByOwner(user).stream().filter(j -> jobIds.contains(j.getId())).toList();

    if (jobs.isEmpty()) throw new ResourceNotFoundException("No jobs found");

    switch (operation.toLowerCase()) {
      case "pause" -> {
        jobs.forEach(j -> j.setStatus(Status.PAUSED));
        log.info("Paused {} jobs for user: {}", jobs.size(), userId);
      }
      case "resume" -> {
        jobs.forEach(
            j -> {
              if (j.getStatus() != Status.FAILED) j.setStatus(Status.PENDING);
            });
        log.info("Resumed {} jobs for user: {}", jobs.size(), userId);
      }
      case "delete" -> {
        jobs.forEach(j -> deleteJob(j.getId(), userId));
        log.info("Deleted {} jobs for user: {}", jobs.size(), userId);
        return;
      }
      default -> throw new IllegalArgumentException("Invalid operation: " + operation);
    }

    jobRepository.saveAll(jobs);
  }

  public List<JobResponse> searchJobs(String query, UUID userId) {
    User user = resolveUser(userId);
    return jobRepository.searchByOwnerAndQuery(user, query).stream()
        .filter(job -> job.getStatus() != Status.FAILED)
        .map(JobService::mapToJobResponse)
        .toList();
  }

  public List<JobResponse> filterByStatus(Status status, UUID userId) {
    User user = resolveUser(userId);
    return jobRepository.findByOwnerAndStatus(user, status).stream()
        .map(JobService::mapToJobResponse)
        .toList();
  }

  public List<JobResponse> filterByDateRange(
      LocalDateTime startDate, LocalDateTime endDate, UUID userId) {
    User user = resolveUser(userId);
    return jobRepository.findByOwnerAndDateRange(user, startDate, endDate).stream()
        .filter(job -> job.getStatus() != Status.FAILED)
        .map(JobService::mapToJobResponse)
        .toList();
  }

  public static JobResponse mapToJobResponse(Job job) {
    return JobResponse.builder()
        .id(job.getId())
        .name(job.getName())
        .cronExpression(job.getCronExpression())
        .maxRetries(job.getMaxRetries())
        .status(job.getStatus())
        .lastError(job.getLastError())
        .recurring(job.isRecurring())
        .retryCount(job.getRetryCount())
        .nextRunTime(job.getNextRunTime())
        .jobType(job.getJobType() != null ? job.getJobType().name() : null)
        .payload(job.getPayload())
        .build();
  }
}
