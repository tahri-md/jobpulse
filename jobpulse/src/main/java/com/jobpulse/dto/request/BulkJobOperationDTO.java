package com.jobpulse.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkJobOperationDTO {
    private List<Long> jobIds;
    private String operation; // "pause", "resume", "delete"
}
