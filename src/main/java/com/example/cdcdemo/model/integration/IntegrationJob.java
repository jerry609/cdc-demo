package com.example.cdcdemo.model.integration;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("integration_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationJob implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String integrationId;
    private String sourceName;
    private String sourceType;
    private String targetEntity;
    private String integrationStrategy;
    private String status;
    private LocalDateTime requestTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long recordsProcessed;
    private Long recordsSuccess;
    private Long recordsFailed;
    private String errorMessage;
    private String fieldMappings; // JSON string of field mappings
    private String sourceConfig;  // JSON string of source config
}