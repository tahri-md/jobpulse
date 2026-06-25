package com.jobpulse.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jobpulse.dto.others.JobFailureReason;
import com.jobpulse.dto.request.JobRequestDTO;
import com.jobpulse.dto.request.JobRequestDTO.JobType;
import com.jobpulse.dto.request.ScheduleDTO;
import com.jobpulse.dto.request.ScheduleDTO.Frequency;
import com.jobpulse.dto.request.ScheduleDTO.ScheduleType;
import com.jobpulse.dto.response.JobHistoryResponse;
import com.jobpulse.dto.response.JobResponse;
import com.jobpulse.dto.response.JobStatsResponse;
import com.jobpulse.exception.NonRetryableJobException;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.exception.RetryableJobException;
import com.jobpulse.model.DeadLetterJob;
import com.jobpulse.model.Job;
import com.jobpulse.model.JobHistory;
import com.jobpulse.model.Status;
import com.jobpulse.model.User;
import com.jobpulse.repository.DeadLetterJobRepository;
import com.jobpulse.repository.JobHistoryRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

  @Mock private JobRepository jobRepository;
  @Mock private UserRepository userRepository;
  @Mock private RedisTemplate<String, String> redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;
  @Mock private JobExecutorFactory executorFactory;
  @Mock private DeadLetterJobRepository deadLetterJobRepository;
  @Mock private JobHistoryRepository jobHistoryRepository;
  @Mock private RetryPolicy retryPolicy;
  @Mock private JobExecutor jobExecutor;

  @InjectMocks private JobService jobService;

  private User user;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user =
        User.builder()
            .id(userId)
            .username("tahri")
            .email("tahri@test.com")
            .passwordHash("hash")
            .build();
  }

  // createJobFull

  @Nested
  class CreateJobFull {

    @Test
    void oneTime_validFutureDate_savesJob() {
      ScheduleDTO schedule = new ScheduleDTO();
      schedule.setType(ScheduleType.ONE_TIME);
      schedule.setRunAt(LocalDateTime.now().plusHours(1));

      JobRequestDTO dto =
          JobRequestDTO.builder()
              .name("My Job")
              .ownerId(userId)
              .jobType(JobType.LOG)
              .schedule(schedule)
              .maxRetries(3)
              .build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      jobService.createJobFull(dto);

      ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
      verify(jobRepository).save(captor.capture());

      Job saved = captor.getValue();
      assertThat(saved.getName()).isEqualTo("My Job");
      assertThat(saved.isRecurring()).isFalse();
      assertThat(saved.getStatus()).isEqualTo(Status.PENDING);
      assertThat(saved.getNextRunTime()).isAfter(LocalDateTime.now());
    }

    @Test
    void oneTime_pastDate_throwsIllegalArgument() {
      ScheduleDTO schedule = new ScheduleDTO();
      schedule.setType(ScheduleType.ONE_TIME);
      schedule.setRunAt(LocalDateTime.now().minusHours(1));

      JobRequestDTO dto =
          JobRequestDTO.builder()
              .name("Old Job")
              .ownerId(userId)
              .jobType(JobType.LOG)
              .schedule(schedule)
              .build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> jobService.createJobFull(dto))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("runAt");
    }

    @Test
    void recurring_validConfig_savesJobWithCron() {
      ScheduleDTO schedule = new ScheduleDTO();
      schedule.setType(ScheduleType.RECURRING);
      schedule.setFrequency(Frequency.HOURS);
      schedule.setInterval(2);

      JobRequestDTO dto =
          JobRequestDTO.builder()
              .name("Recurring Job")
              .ownerId(userId)
              .jobType(JobType.LOG)
              .schedule(schedule)
              .build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      jobService.createJobFull(dto);

      ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
      verify(jobRepository).save(captor.capture());

      Job saved = captor.getValue();
      assertThat(saved.isRecurring()).isTrue();
      assertThat(saved.getCronExpression()).isNotBlank();
      assertThat(saved.getNextRunTime()).isNotNull();
    }

    @Test
    void recurring_missingFrequency_throwsIllegalArgument() {
      ScheduleDTO schedule = new ScheduleDTO();
      schedule.setType(ScheduleType.RECURRING);
      schedule.setFrequency(null);
      schedule.setInterval(1);

      JobRequestDTO dto =
          JobRequestDTO.builder()
              .name("Bad Job")
              .ownerId(userId)
              .jobType(JobType.LOG)
              .schedule(schedule)
              .build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> jobService.createJobFull(dto))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cron_validExpression_savesJob() {
      ScheduleDTO schedule = new ScheduleDTO();
      schedule.setType(ScheduleType.CRON);
      schedule.setCronExpression("0 0 * * * ? *"); // every hour

      JobRequestDTO dto =
          JobRequestDTO.builder()
              .name("Cron Job")
              .ownerId(userId)
              .jobType(JobType.LOG)
              .schedule(schedule)
              .build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      jobService.createJobFull(dto);

      ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
      verify(jobRepository).save(captor.capture());
      assertThat(captor.getValue().getCronExpression()).isEqualTo("0 0 * * * ? *");
    }

    @Test
    void cron_blankExpression_throwsIllegalArgument() {
      ScheduleDTO schedule = new ScheduleDTO();
      schedule.setType(ScheduleType.CRON);
      schedule.setCronExpression("");

      JobRequestDTO dto =
          JobRequestDTO.builder()
              .name("Bad Cron")
              .ownerId(userId)
              .jobType(JobType.LOG)
              .schedule(schedule)
              .build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> jobService.createJobFull(dto))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Cron expression");
    }

    @Test
    void nullOwnerId_throwsRuntimeException() {
      JobRequestDTO dto =
          JobRequestDTO.builder()
              .name("Job")
              .ownerId(null)
              .jobType(JobType.LOG)
              .schedule(new ScheduleDTO())
              .build();

      assertThatThrownBy(() -> jobService.createJobFull(dto)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void unknownUser_throwsResourceNotFound() {
      ScheduleDTO schedule = new ScheduleDTO();
      schedule.setType(ScheduleType.ONE_TIME);
      schedule.setRunAt(LocalDateTime.now().plusHours(1));

      JobRequestDTO dto =
          JobRequestDTO.builder()
              .name("Job")
              .ownerId(userId)
              .jobType(JobType.LOG)
              .schedule(schedule)
              .build();

      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> jobService.createJobFull(dto))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // runDueJobs — scheduler

  @Nested
  class RunDueJobs {

    private Job buildJob(Status status) {
      return Job.builder()
          .id(1L)
          .name("Test Job")
          .jobType(JobType.LOG)
          .owner(user)
          .status(status)
          .retryCount(0)
          .maxRetries(3)
          .nextRunTime(LocalDateTime.now().minusSeconds(5))
          .build();
    }

    @Test
    void acquiresLock_executesJob_releasesLock() throws Exception {
      Job job = buildJob(Status.PENDING);
      when(jobRepository.findDueJobs(any())).thenReturn(List.of(job));
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
          .thenReturn(true);
      when(executorFactory.get(JobType.LOG)).thenReturn(jobExecutor);
      doNothing().when(jobExecutor).execute(job);

      jobService.runDueJobs();

      verify(jobExecutor).execute(job);
      verify(redisTemplate).delete("job-lock:1");
      verify(jobRepository).save(job);
      assertThat(job.getStatus()).isEqualTo(Status.SUCCESS);
    }

    @Test
    void cannotAcquireLock_skipsJob() throws Exception {
      Job job = buildJob(Status.PENDING);
      when(jobRepository.findDueJobs(any())).thenReturn(List.of(job));
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
          .thenReturn(false);

      jobService.runDueJobs();

      verify(jobExecutor, never()).execute(any());
    }

    @Test
    void noDueJobs_doesNothing() throws Exception {
      when(jobRepository.findDueJobs(any())).thenReturn(List.of());

      jobService.runDueJobs();

      verify(jobExecutor, never()).execute(any());
    }

    @Test
    void retryableException_setsStatusRetrying() throws Exception {
      Job job = buildJob(Status.PENDING);
      when(jobRepository.findDueJobs(any())).thenReturn(List.of(job));
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
          .thenReturn(true);
      when(executorFactory.get(JobType.LOG)).thenReturn(jobExecutor);
      doThrow(new RetryableJobException(JobFailureReason.NETWORK_ERROR, "timeout"))
          .when(jobExecutor)
          .execute(job);
      when(retryPolicy.isRetryable(JobFailureReason.NETWORK_ERROR)).thenReturn(true);

      jobService.runDueJobs();

      assertThat(job.getStatus()).isEqualTo(Status.RETRYING);
      assertThat(job.getRetryCount()).isEqualTo(1);
      assertThat(job.getNextRunTime()).isAfter(LocalDateTime.now());
    }

    @Test
    void nonRetryableException_movesToDeadLetter() throws Exception {
      Job job = buildJob(Status.PENDING);
      when(jobRepository.findDueJobs(any())).thenReturn(List.of(job));
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
          .thenReturn(true);
      when(executorFactory.get(JobType.LOG)).thenReturn(jobExecutor);
      doThrow(new NonRetryableJobException(JobFailureReason.INVALID_CONFIG, "bad config"))
          .when(jobExecutor)
          .execute(job);
      when(retryPolicy.isRetryable(JobFailureReason.INVALID_CONFIG)).thenReturn(false);

      jobService.runDueJobs();

      assertThat(job.getStatus()).isEqualTo(Status.FAILED);
      verify(deadLetterJobRepository).save(any(DeadLetterJob.class));
    }

    @Test
    void maxRetriesReached_movesToDeadLetter() throws Exception {
      Job job = buildJob(Status.RETRYING);
      job.setRetryCount(2);
      job.setMaxRetries(3);

      when(jobRepository.findDueJobs(any())).thenReturn(List.of(job));
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
          .thenReturn(true);
      when(executorFactory.get(JobType.LOG)).thenReturn(jobExecutor);
      doThrow(new RetryableJobException(JobFailureReason.NETWORK_ERROR, "still failing"))
          .when(jobExecutor)
          .execute(job);
      when(retryPolicy.isRetryable(JobFailureReason.NETWORK_ERROR)).thenReturn(true);

      jobService.runDueJobs();

      assertThat(job.getStatus()).isEqualTo(Status.FAILED);
      verify(deadLetterJobRepository).save(any(DeadLetterJob.class));
    }

    @Test
    void recurringJob_success_updatesNextRunTime() throws Exception {
      Job job = buildJob(Status.PENDING);
      job.setRecurring(true);
      job.setCronExpression("0 0 * * * ? *");

      when(jobRepository.findDueJobs(any())).thenReturn(List.of(job));
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
          .thenReturn(true);
      when(executorFactory.get(JobType.LOG)).thenReturn(jobExecutor);
      doNothing().when(jobExecutor).execute(job);

      jobService.runDueJobs();

      assertThat(job.getStatus()).isEqualTo(Status.SUCCESS);
      assertThat(job.getNextRunTime()).isAfter(LocalDateTime.now());
    }

    @Test
    void oneTimeJob_success_nextRunTimeIsNull() throws Exception {
      Job job = buildJob(Status.PENDING);
      job.setRecurring(false);
      job.setCronExpression(null);

      when(jobRepository.findDueJobs(any())).thenReturn(List.of(job));
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
          .thenReturn(true);
      when(executorFactory.get(JobType.LOG)).thenReturn(jobExecutor);
      doNothing().when(jobExecutor).execute(job);

      jobService.runDueJobs();

      assertThat(job.getStatus()).isEqualTo(Status.SUCCESS);
      assertThat(job.getNextRunTime()).isNull();
    }
  }

  // getJob / getJobs

  @Nested
  class GetJobs {

    @Test
    void getJobs_returnsMappedResponses_excludesFailed() {
      Job pending =
          Job.builder()
              .id(1L)
              .name("A")
              .status(Status.PENDING)
              .owner(user)
              .jobType(JobType.LOG)
              .build();
      Job failed =
          Job.builder()
              .id(2L)
              .name("B")
              .status(Status.FAILED)
              .owner(user)
              .jobType(JobType.LOG)
              .build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByOwner(user)).thenReturn(List.of(pending, failed));

      List<JobResponse> result = jobService.getJobs(userId);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("A");
    }

    @Test
    void getJob_found_returnsMappedResponse() {
      Job job =
          Job.builder()
              .id(1L)
              .name("MyJob")
              .status(Status.PENDING)
              .owner(user)
              .jobType(JobType.LOG)
              .build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(job));

      JobResponse result = jobService.getJob(1L, userId);

      assertThat(result.getName()).isEqualTo("MyJob");
    }

    @Test
    void getJob_notFound_throwsResourceNotFound() {
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByIdAndOwner(99L, user)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> jobService.getJob(99L, userId))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // deleteJob

  @Nested
  class DeleteJob {

    @Test
    void deleteJob_deletesJobAndDeadLetterEntries() {
      Job job = Job.builder().id(1L).name("ToDelete").status(Status.FAILED).owner(user).build();
      DeadLetterJob dlJob = DeadLetterJob.builder().id(10L).job(job).build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(job));
      when(deadLetterJobRepository.findByJob_Owner(user)).thenReturn(List.of(dlJob));

      jobService.deleteJob(1L, userId);

      verify(deadLetterJobRepository).delete(dlJob);
      verify(jobRepository).delete(job);
    }

    @Test
    void deleteJob_notFound_throwsResourceNotFound() {
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByIdAndOwner(99L, user)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> jobService.deleteJob(99L, userId))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // pauseJob / resumeJob

  @Nested
  class PauseResume {

    @Test
    void pauseJob_setsStatusPaused() {
      Job job = Job.builder().id(1L).status(Status.PENDING).owner(user).build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(job));
      when(jobRepository.save(job)).thenReturn(job);

      JobResponse result = jobService.pauseJob(1L, userId);

      assertThat(result.getStatus()).isEqualTo(Status.PAUSED);
    }

    @Test
    void resumeJob_fromPaused_setsStatusPending() {
      Job job =
          Job.builder()
              .id(1L)
              .status(Status.PAUSED)
              .owner(user)
              .nextRunTime(LocalDateTime.now().plusMinutes(5))
              .build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(job));
      when(jobRepository.save(job)).thenReturn(job);

      JobResponse result = jobService.resumeJob(1L, userId);

      assertThat(result.getStatus()).isEqualTo(Status.PENDING);
    }

    @Test
    void resumeJob_fromFailed_throwsIllegalState() {
      Job job = Job.builder().id(1L).status(Status.FAILED).owner(user).build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(job));

      assertThatThrownBy(() -> jobService.resumeJob(1L, userId))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("dead letter");
    }

    @Test
    void resumeJob_pastNextRunTime_setsNearFutureRunTime() {
      Job job =
          Job.builder()
              .id(1L)
              .status(Status.PAUSED)
              .owner(user)
              .nextRunTime(LocalDateTime.now().minusHours(1))
              .build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(job));
      when(jobRepository.save(job)).thenReturn(job);

      jobService.resumeJob(1L, userId);

      assertThat(job.getNextRunTime()).isAfter(LocalDateTime.now());
    }
  }

  // bulkOperation

  @Nested
  class BulkOperation {

    private Job buildJob(long id, Status status) {
      return Job.builder().id(id).name("Job " + id).status(status).owner(user).build();
    }

    @Test
    void bulkPause_pausesAllMatchingJobs() {
      Job j1 = buildJob(1L, Status.PENDING);
      Job j2 = buildJob(2L, Status.PENDING);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByOwner(user)).thenReturn(List.of(j1, j2));

      jobService.bulkOperation(List.of(1L, 2L), "pause", userId);

      assertThat(j1.getStatus()).isEqualTo(Status.PAUSED);
      assertThat(j2.getStatus()).isEqualTo(Status.PAUSED);
      verify(jobRepository).saveAll(List.of(j1, j2));
    }

    @Test
    void bulkResume_skipsFailedJobs() {
      Job j1 = buildJob(1L, Status.PAUSED);
      Job j2 = buildJob(2L, Status.FAILED);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByOwner(user)).thenReturn(List.of(j1, j2));

      jobService.bulkOperation(List.of(1L, 2L), "resume", userId);

      assertThat(j1.getStatus()).isEqualTo(Status.PENDING);
      assertThat(j2.getStatus()).isEqualTo(Status.FAILED); // unchanged
    }

    @Test
    void bulkDelete_deletesEachJob() {
      Job j1 = buildJob(1L, Status.PENDING);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByOwner(user)).thenReturn(List.of(j1));
      when(jobRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(j1));
      when(deadLetterJobRepository.findByJob_Owner(user)).thenReturn(List.of());

      jobService.bulkOperation(List.of(1L), "delete", userId);

      verify(jobRepository).delete(j1);
    }

    @Test
    void bulkOperation_invalidOperation_throwsIllegalArgument() {
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByOwner(user))
          .thenReturn(List.of(Job.builder().id(1L).status(Status.PENDING).owner(user).build()));

      assertThatThrownBy(() -> jobService.bulkOperation(List.of(1L), "explode", userId))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bulkOperation_noMatchingJobs_throwsResourceNotFound() {
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByOwner(user)).thenReturn(List.of());

      assertThatThrownBy(() -> jobService.bulkOperation(List.of(99L), "pause", userId))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // replayDeadJob

  @Nested
  class ReplayDeadJob {

    @Test
    void replay_resetsJobAndDeletesDeadLetterEntry() {
      Job job =
          Job.builder()
              .id(1L)
              .name("Dead")
              .status(Status.FAILED)
              .owner(user)
              .retryCount(3)
              .maxRetries(3)
              .recurring(false)
              .build();
      DeadLetterJob dlJob = DeadLetterJob.builder().id(10L).job(job).build();

      when(deadLetterJobRepository.findById(10L)).thenReturn(Optional.of(dlJob));
      when(jobRepository.save(job)).thenReturn(job);

      JobResponse result = jobService.replayDeadJob(10L, userId);

      assertThat(result.getStatus()).isEqualTo(Status.PENDING);
      assertThat(job.getRetryCount()).isEqualTo(0);
      verify(deadLetterJobRepository).delete(dlJob);
    }

    @Test
    void replay_wrongOwner_throwsResourceNotFound() {
      User otherUser = User.builder().id(UUID.randomUUID()).build();
      Job job = Job.builder().id(1L).owner(otherUser).status(Status.FAILED).build();
      DeadLetterJob dlJob = DeadLetterJob.builder().id(10L).job(job).build();

      when(deadLetterJobRepository.findById(10L)).thenReturn(Optional.of(dlJob));

      assertThatThrownBy(() -> jobService.replayDeadJob(10L, userId))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void replay_notFound_throwsResourceNotFound() {
      when(deadLetterJobRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> jobService.replayDeadJob(99L, userId))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // getJobStats

  @Nested
  class GetJobStats {

    @Test
    void stats_countsEachStatusCorrectly() {
      List<Job> jobs =
          List.of(
              Job.builder().status(Status.PENDING).owner(user).build(),
              Job.builder().status(Status.RUNNING).owner(user).build(),
              Job.builder().status(Status.SUCCESS).owner(user).build(),
              Job.builder().status(Status.RETRYING).owner(user).build(),
              Job.builder().status(Status.FAILED).owner(user).build());
      DeadLetterJob dl = DeadLetterJob.builder().job(Job.builder().owner(user).build()).build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByOwner(user)).thenReturn(jobs);
      when(deadLetterJobRepository.findByJob_Owner(user)).thenReturn(List.of(dl));

      JobStatsResponse stats = jobService.getJobStats(userId);

      assertThat(stats.getTotalJobs()).isEqualTo(5);
      assertThat(stats.getPendingJobs()).isEqualTo(1);
      assertThat(stats.getRunningJobs()).isEqualTo(1);
      assertThat(stats.getSuccessfulJobs()).isEqualTo(1);
      assertThat(stats.getRetryingJobs()).isEqualTo(1);
      assertThat(stats.getFailedJobs()).isEqualTo(1);
      assertThat(stats.getDeadLetterJobs()).isEqualTo(1);
    }
  }

  // getJobHistory

  @Nested
  class GetJobHistory {

    @Test
    void returnsHistoryOrderedByRunTime() {
      Job job = Job.builder().id(1L).owner(user).status(Status.SUCCESS).build();
      JobHistory h1 =
          JobHistory.builder()
              .id(1L)
              .job(job)
              .status(Status.SUCCESS)
              .runTime(LocalDateTime.now().minusMinutes(10))
              .retryAttempt(0)
              .build();
      JobHistory h2 =
          JobHistory.builder()
              .id(2L)
              .job(job)
              .status(Status.RETRYING)
              .runTime(LocalDateTime.now().minusMinutes(5))
              .retryAttempt(1)
              .build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(job));
      when(jobHistoryRepository.findByJobOrderByRunTimeDesc(job)).thenReturn(List.of(h2, h1));

      List<JobHistoryResponse> result = jobService.getJobHistory(1L, userId);

      assertThat(result).hasSize(2);
      assertThat(result.get(0).getStatus()).isEqualTo(Status.RETRYING);
    }
  }

  // searchJobs / filterByStatus / filterByDateRange

  @Nested
  class SearchAndFilter {

    @Test
    void searchJobs_excludesFailedResults() {
      Job match =
          Job.builder()
              .id(1L)
              .name("email report")
              .status(Status.SUCCESS)
              .owner(user)
              .jobType(JobType.EMAIL)
              .build();
      Job failed =
          Job.builder()
              .id(2L)
              .name("email cleanup")
              .status(Status.FAILED)
              .owner(user)
              .jobType(JobType.EMAIL)
              .build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.searchByOwnerAndQuery(user, "email")).thenReturn(List.of(match, failed));

      List<JobResponse> result = jobService.searchJobs("email", userId);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("email report");
    }

    @Test
    void filterByStatus_returnsMappedJobs() {
      Job job = Job.builder().id(1L).status(Status.PAUSED).owner(user).jobType(JobType.LOG).build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByOwnerAndStatus(user, Status.PAUSED)).thenReturn(List.of(job));

      List<JobResponse> result = jobService.filterByStatus(Status.PAUSED, userId);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getStatus()).isEqualTo(Status.PAUSED);
    }

    @Test
    void filterByDateRange_excludesFailed() {
      LocalDateTime start = LocalDateTime.now().minusDays(7);
      LocalDateTime end = LocalDateTime.now();

      Job ok = Job.builder().id(1L).status(Status.SUCCESS).owner(user).jobType(JobType.LOG).build();
      Job failed =
          Job.builder().id(2L).status(Status.FAILED).owner(user).jobType(JobType.LOG).build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(jobRepository.findByOwnerAndDateRange(user, start, end)).thenReturn(List.of(ok, failed));

      List<JobResponse> result = jobService.filterByDateRange(start, end, userId);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getStatus()).isEqualTo(Status.SUCCESS);
    }
  }
}
