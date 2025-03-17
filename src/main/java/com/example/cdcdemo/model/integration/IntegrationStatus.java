package com.example.cdcdemo.model.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationStatus implements Serializable {
    private String integrationId;
    private String sourceName;
    private String targetEntity;
    private String status;  // PENDING, PROCESSING, COMPLETED, FAILED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long recordsProcessed;
    private Long recordsSuccess;
    private Long recordsFailed;
    private String errorMessage;
}