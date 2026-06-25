package com.jobpulse.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jobpulse.dto.request.JobRequestDTO.JobType;
import com.jobpulse.dto.request.JobTemplateRequestDTO;
import com.jobpulse.dto.response.JobTemplateResponse;
import com.jobpulse.model.JobTemplate;
import com.jobpulse.model.User;
import com.jobpulse.repository.JobTemplateRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobTemplateServiceTest {

  @Mock private JobTemplateRepository jobTemplateRepository;

  @InjectMocks private JobTemplateService jobTemplateService;

  private User owner;

  @BeforeEach
  void setUp() {
    owner = User.builder().id(UUID.randomUUID()).username("tahri").build();
  }

  private JobTemplate buildTemplate(long id, boolean isPublic) {
    return JobTemplate.builder()
        .id(id)
        .name("Template " + id)
        .description("desc")
        .jobType(JobType.LOG)
        .payload("{}")
        .cronExpression("0 0 * * * ? *")
        .maxRetries(3)
        .isPublic(isPublic)
        .owner(owner)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  private JobTemplateRequestDTO buildDto(boolean isPublic) {
    return JobTemplateRequestDTO.builder()
        .name("My Template")
        .description("A test template")
        .jobType(JobType.LOG)
        .payload("{\"key\":\"value\"}")
        .cronExpression("0 0 * * * ? *")
        .maxRetries(3)
        .isPublic(isPublic)
        .build();
  }

  // ─────────────────────────────────────────────────────────────────
  // createTemplate
  // ─────────────────────────────────────────────────────────────────

  @Nested
  class CreateTemplate {

    @Test
    void validRequest_savesAndReturnsMappedResponse() {
      JobTemplate saved = buildTemplate(1L, false);
      when(jobTemplateRepository.save(any(JobTemplate.class))).thenReturn(saved);

      JobTemplateResponse result = jobTemplateService.createTemplate(buildDto(false), owner);

      assertThat(result.getName()).isEqualTo("Template 1");
      assertThat(result.getOwnerName()).isEqualTo("tahri");
      verify(jobTemplateRepository).save(any(JobTemplate.class));
    }

    @Test
    void savedTemplateHasCorrectFields() {
      JobTemplate saved = buildTemplate(1L, true);
      when(jobTemplateRepository.save(any())).thenReturn(saved);

      jobTemplateService.createTemplate(buildDto(true), owner);

      ArgumentCaptor<JobTemplate> captor = ArgumentCaptor.forClass(JobTemplate.class);
      verify(jobTemplateRepository).save(captor.capture());

      JobTemplate captured = captor.getValue();
      assertThat(captured.getOwner()).isEqualTo(owner);
      assertThat(captured.getCreatedAt()).isNotNull();
      assertThat(captured.getUpdatedAt()).isNotNull();
    }
  }

  // ─────────────────────────────────────────────────────────────────
  // getAvailableTemplates
  // ─────────────────────────────────────────────────────────────────

  @Nested
  class GetAvailableTemplates {

    @Test
    void returnsOwnAndPublicTemplates() {
      JobTemplate own = buildTemplate(1L, false);
      JobTemplate pub = buildTemplate(2L, true);

      when(jobTemplateRepository.findByOwnerOrIsPublicTrue(owner, true))
          .thenReturn(List.of(own, pub));

      List<JobTemplateResponse> result = jobTemplateService.getAvailableTemplates(owner);

      assertThat(result).hasSize(2);
    }

    @Test
    void emptyRepository_returnsEmptyList() {
      when(jobTemplateRepository.findByOwnerOrIsPublicTrue(owner, true)).thenReturn(List.of());

      assertThat(jobTemplateService.getAvailableTemplates(owner)).isEmpty();
    }
  }

  // ─────────────────────────────────────────────────────────────────
  // getUserTemplates
  // ─────────────────────────────────────────────────────────────────

  @Nested
  class GetUserTemplates {

    @Test
    void returnsOnlyOwnerTemplates() {
      JobTemplate t = buildTemplate(1L, false);
      when(jobTemplateRepository.findByOwner(owner)).thenReturn(List.of(t));

      List<JobTemplateResponse> result = jobTemplateService.getUserTemplates(owner);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getOwnerName()).isEqualTo("tahri");
    }
  }

  // ─────────────────────────────────────────────────────────────────
  // getTemplate
  // ─────────────────────────────────────────────────────────────────

  @Nested
  class GetTemplate {

    @Test
    void ownTemplate_returnsResponse() {
      JobTemplate t = buildTemplate(1L, false);
      when(jobTemplateRepository.findById(1L)).thenReturn(Optional.of(t));

      JobTemplateResponse result = jobTemplateService.getTemplate(1L, owner);

      assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void notFound_throwsRuntimeException() {
      when(jobTemplateRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> jobTemplateService.getTemplate(99L, owner))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("not found");
    }
  }

  // ─────────────────────────────────────────────────────────────────
  // updateTemplate
  // ─────────────────────────────────────────────────────────────────

  @Nested
  class UpdateTemplate {

    @Test
    void validUpdate_returnsUpdatedResponse() {
      JobTemplate existing = buildTemplate(1L, false);
      JobTemplate updated = buildTemplate(1L, true);
      updated.setName("Updated");

      when(jobTemplateRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(existing));
      when(jobTemplateRepository.save(any())).thenReturn(updated);

      JobTemplateRequestDTO dto = buildDto(true);
      dto.setName("Updated");

      JobTemplateResponse result = jobTemplateService.updateTemplate(1L, dto, owner);

      assertThat(result.getName()).isEqualTo("Updated");
    }

    @Test
    void notOwner_throwsRuntimeException() {
      when(jobTemplateRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> jobTemplateService.updateTemplate(1L, buildDto(false), owner))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("not found");
    }
  }

  // ─────────────────────────────────────────────────────────────────
  // deleteTemplate
  // ─────────────────────────────────────────────────────────────────

  @Nested
  class DeleteTemplate {

    @Test
    void ownerDeletes_callsRepositoryDelete() {
      JobTemplate t = buildTemplate(1L, false);
      when(jobTemplateRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(t));

      jobTemplateService.deleteTemplate(1L, owner);

      verify(jobTemplateRepository).delete(t);
    }

    @Test
    void notOwner_throwsRuntimeException() {
      when(jobTemplateRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> jobTemplateService.deleteTemplate(1L, owner))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("not found");
    }
  }
}
