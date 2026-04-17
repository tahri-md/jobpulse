package com.jobpulse.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jobpulse.dto.request.JobTemplateRequestDTO;
import com.jobpulse.dto.response.JobTemplateResponse;
import com.jobpulse.model.JobTemplate;
import com.jobpulse.model.User;
import com.jobpulse.repository.JobTemplateRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JobTemplateService {

    @Autowired
    private JobTemplateRepository jobTemplateRepository;

    public JobTemplateResponse createTemplate(JobTemplateRequestDTO dto, User owner) {
        JobTemplate template = JobTemplate.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .jobType(dto.getJobType())
                .payload(dto.getPayload())
                .cronExpression(dto.getCronExpression())
                .maxRetries(dto.getMaxRetries())
                .isPublic(dto.isPublic())
                .owner(owner)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        JobTemplate saved = jobTemplateRepository.save(template);
        log.info("Job template created: {} by user: {}", saved.getId(), owner.getId());
        return mapToResponse(saved);
    }

    public List<JobTemplateResponse> getAvailableTemplates(User owner) {
        return jobTemplateRepository.findByOwnerOrIsPublicTrue(owner, true)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<JobTemplateResponse> getUserTemplates(User owner) {
        return jobTemplateRepository.findByOwner(owner)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public JobTemplateResponse getTemplate(long id, User user) {
        JobTemplate template = jobTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        if (template.getOwner().getId() != user.getId() && !template.isPublic()) {
            throw new RuntimeException("Unauthorized");
        }

        return mapToResponse(template);
    }

    public JobTemplateResponse updateTemplate(long id, JobTemplateRequestDTO dto, User user) {
        JobTemplate template = jobTemplateRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setJobType(dto.getJobType());
        template.setPayload(dto.getPayload());
        template.setCronExpression(dto.getCronExpression());
        template.setMaxRetries(dto.getMaxRetries());
        template.setPublic(dto.isPublic());
        template.setUpdatedAt(LocalDateTime.now());

        JobTemplate updated = jobTemplateRepository.save(template);
        log.info("Job template updated: {} by user: {}", id, user.getId());
        return mapToResponse(updated);
    }

    public void deleteTemplate(long id, User user) {
        JobTemplate template = jobTemplateRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        jobTemplateRepository.delete(template);
        log.info("Job template deleted: {} by user: {}", id, user.getId());
    }

    private JobTemplateResponse mapToResponse(JobTemplate template) {
        return JobTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .jobType(template.getJobType())
                .payload(template.getPayload())
                .cronExpression(template.getCronExpression())
                .maxRetries(template.getMaxRetries())
                .isPublic(template.isPublic())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .ownerName(template.getOwner().getUsername())
                .build();
    }
}
