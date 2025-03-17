package com.example.cdcdemo.model.integration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataIntegrationRequest implements Serializable {
    private String sourceName;
    private String sourceType;
    private Map<String, Object> sourceConfig;
    private String targetEntity;
    private String integrationStrategy; // MERGE, REPLACE, APPEND
    private Map<String, String> fieldMappings;
    private Boolean validateData;
    private LocalDateTime requestTime;
}