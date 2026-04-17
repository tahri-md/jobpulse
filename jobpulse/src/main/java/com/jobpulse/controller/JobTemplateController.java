package com.jobpulse.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.jobpulse.dto.request.JobTemplateRequestDTO;
import com.jobpulse.dto.response.JobTemplateResponse;
import com.jobpulse.model.User;
import com.jobpulse.service.JobTemplateService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/templates")
@Slf4j
public class JobTemplateController {

    @Autowired
    private JobTemplateService jobTemplateService;

    @PostMapping
    public ResponseEntity<JobTemplateResponse> createTemplate(
            @RequestBody JobTemplateRequestDTO request,
            @AuthenticationPrincipal User user) {
        log.info("Creating job template: {} for user: {}", request.getName(), user.getId());
        JobTemplateResponse response = jobTemplateService.createTemplate(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<JobTemplateResponse>> getAvailableTemplates(@AuthenticationPrincipal User user) {
        log.debug("Fetching available templates for user: {}", user.getId());
        return ResponseEntity.ok(jobTemplateService.getAvailableTemplates(user));
    }

    @GetMapping("/own")
    public ResponseEntity<List<JobTemplateResponse>> getOwnTemplates(@AuthenticationPrincipal User user) {
        log.debug("Fetching own templates for user: {}", user.getId());
        return ResponseEntity.ok(jobTemplateService.getUserTemplates(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobTemplateResponse> getTemplate(
            @PathVariable long id,
            @AuthenticationPrincipal User user) {
        log.debug("Fetching template: {} for user: {}", id, user.getId());
        return ResponseEntity.ok(jobTemplateService.getTemplate(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobTemplateResponse> updateTemplate(
            @PathVariable long id,
            @RequestBody JobTemplateRequestDTO request,
            @AuthenticationPrincipal User user) {
        log.info("Updating job template: {} for user: {}", id, user.getId());
        JobTemplateResponse response = jobTemplateService.updateTemplate(id, request, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable long id,
            @AuthenticationPrincipal User user) {
        log.info("Deleting job template: {} for user: {}", id, user.getId());
        jobTemplateService.deleteTemplate(id, user);
        return ResponseEntity.noContent().build();
    }
}
