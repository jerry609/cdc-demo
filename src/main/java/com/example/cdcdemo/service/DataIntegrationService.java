package com.example.cdcdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.cdcdemo.mapper.CustomerMapper;
import com.example.cdcdemo.mapper.IntegrationJobMapper;
import com.example.cdcdemo.model.Customer;
import com.example.cdcdemo.model.integration.DataIntegrationRequest;
import com.example.cdcdemo.model.integration.IntegrationJob;
import com.example.cdcdemo.model.integration.IntegrationStatus;
import com.example.cdcdemo.publisher.ChangeEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataIntegrationService {

    private final IntegrationJobMapper integrationJobMapper;
    private final CustomerMapper customerMapper;
    private final CustomerService customerService;
    private final ChangeEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String INTEGRATION_STATUS_KEY = "integration:status:";
    private static final String INTEGRATION_JOBS_KEY = "integration:jobs";
     @Autowired
    private RedisTemplate<String, IntegrationStatus> integrationStatusRedisTemplate;


    public IntegrationStatus getIntegrationStatus(String integrationId) {
        // Use the typed template to retrieve IntegrationStatus directly
        IntegrationStatus cachedStatus = integrationStatusRedisTemplate
            .opsForValue()
            .get(INTEGRATION_STATUS_KEY + integrationId);
        if (cachedStatus != null) {
            return cachedStatus; // No casting needed
        }

        // Fetch from database if not in cache
        IntegrationJob job = integrationJobMapper.selectOne(
            new QueryWrapper<IntegrationJob>().eq("integration_id", integrationId)
        );
        if (job == null) {
            return null;
        }

        // Build IntegrationStatus from job
        IntegrationStatus status = IntegrationStatus.builder()
            .integrationId(job.getIntegrationId())
            .sourceName(job.getSourceName())
            .targetEntity(job.getTargetEntity())
            .status(job.getStatus())
            .startTime(job.getStartTime())
            .endTime(job.getEndTime())
            .recordsProcessed(job.getRecordsProcessed())
            .recordsSuccess(job.getRecordsSuccess())
            .recordsFailed(job.getRecordsFailed())
            .errorMessage(job.getErrorMessage())
            .build();

        // Cache the status using the typed template
        integrationStatusRedisTemplate
            .opsForValue()
            .set(INTEGRATION_STATUS_KEY + integrationId, status, 24, TimeUnit.HOURS);

        return status;
    }

    // Update submitIntegration to use the typed template
    public IntegrationStatus submitIntegration(DataIntegrationRequest request) {
        String integrationId = UUID.randomUUID().toString();
        IntegrationJob job = IntegrationJob.builder()
            .integrationId(integrationId)
            .sourceName(request.getSourceName())
            .sourceType(request.getSourceType())
            .targetEntity(request.getTargetEntity())
            .integrationStrategy(request.getIntegrationStrategy())
            .status("PENDING")
            .requestTime(LocalDateTime.now())
            .fieldMappings(serializeToJson(request.getFieldMappings()))
            .sourceConfig(serializeToJson(request.getSourceConfig()))
            .build();
        integrationJobMapper.insert(job);

        IntegrationStatus initialStatus = IntegrationStatus.builder()
            .integrationId(integrationId)
            .sourceName(request.getSourceName())
            .targetEntity(request.getTargetEntity())
            .status("PENDING")
            .startTime(null)
            .endTime(null)
            .recordsProcessed(null)
            .recordsSuccess(null)
            .recordsFailed(null)
            .errorMessage(null)
            .build();

        // Cache using the typed template
        integrationStatusRedisTemplate
            .opsForValue()
            .set(INTEGRATION_STATUS_KEY + integrationId, initialStatus, 24, TimeUnit.HOURS);

        // Trigger async processing (assumed method)
        processIntegration(integrationId);

        return initialStatus;
    }

    /**
     * Get all integration jobs
     */
    public List<IntegrationStatus> getAllIntegrationJobs() {
        // Get all jobs from database
        List<IntegrationJob> jobs = integrationJobMapper.selectList(null);

        // Convert to status objects
        return jobs.stream()
                .map(job -> IntegrationStatus.builder()
                        .integrationId(job.getIntegrationId())
                        .sourceName(job.getSourceName())
                        .targetEntity(job.getTargetEntity())
                        .status(job.getStatus())
                        .startTime(job.getStartTime())
                        .endTime(job.getEndTime())
                        .recordsProcessed(job.getRecordsProcessed())
                        .recordsSuccess(job.getRecordsSuccess())
                        .recordsFailed(job.getRecordsFailed())
                        .errorMessage(job.getErrorMessage())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Process the integration asynchronously
     */
    @Async
    protected void processIntegration(String integrationId) {
        log.info("Starting integration process for job: {}", integrationId);
        try {
            // Fetch the job using integration_id instead of id
            IntegrationJob job = integrationJobMapper.selectOne(
                    new QueryWrapper<IntegrationJob>().eq("integration_id", integrationId)
            );
            if (job == null) {
                log.error("Integration job not found: {}", integrationId);
                return;
            }

            // Update status to PROCESSING
            updateJobStatus(job, "PROCESSING", null);

            // Deserialize field mappings and source config
            Map<String, String> fieldMappings = deserializeFromJson(job.getFieldMappings(), Map.class);
            Map<String, Object> sourceConfig = deserializeFromJson(job.getSourceConfig(), Map.class);

            // Get data from the source
            List<Map<String, Object>> sourceData = fetchSourceData(job.getSourceType(), sourceConfig);

            // Process the data based on target entity
            if ("customer".equals(job.getTargetEntity().toLowerCase())) {
                processCustomerData(job, sourceData, fieldMappings);
            } else {
                throw new RuntimeException("Unsupported target entity: " + job.getTargetEntity());
            }

            // Update job status to COMPLETED
            updateJobStatus(job, "COMPLETED", null);
            log.info("Integration job completed successfully: {}", integrationId);

        } catch (Exception e) {
            log.error("Error processing integration job: {}", integrationId, e);
            IntegrationJob job = integrationJobMapper.selectOne(
                    new QueryWrapper<IntegrationJob>().eq("integration_id", integrationId)
            );
            updateJobStatus(job, "FAILED", e.getMessage());
        }
    }

    /**
     * Process customer data integration
     */
    private void processCustomerData(IntegrationJob job, List<Map<String, Object>> sourceData, Map<String, String> fieldMappings) {
        long processed = 0;
        long success = 0;
        long failed = 0;

        for (Map<String, Object> sourceRecord : sourceData) {
            try {
                processed++;

                // Map source fields to target fields
                Customer customer = new Customer();

                // Apply field mappings
                for (Map.Entry<String, String> mapping : fieldMappings.entrySet()) {
                    String sourceField = mapping.getKey();
                    String targetField = mapping.getValue();

                    if (sourceRecord.containsKey(sourceField)) {
                        Object value = sourceRecord.get(sourceField);

                        // Set value based on target field
                        switch (targetField.toLowerCase()) {
                            case "id":
                                if (value != null) {
                                    customer.setId(Long.valueOf(value.toString()));
                                }
                                break;
                            case "name":
                                customer.setName(value != null ? value.toString() : null);
                                break;
                            case "email":
                                customer.setEmail(value != null ? value.toString() : null);
                                break;
                            case "phone":
                                customer.setPhone(value != null ? value.toString() : null);
                                break;
                            case "address":
                                customer.setAddress(value != null ? value.toString() : null);
                                break;
                            default:
                                log.warn("Unknown target field: {}", targetField);
                        }
                    }
                }

                // Apply integration strategy
                switch (job.getIntegrationStrategy().toUpperCase()) {
                    case "MERGE":
                        if (customer.getId() != null) {
                            // Get existing customer
                            Customer existingCustomer = customerMapper.selectById(customer.getId());
                            if (existingCustomer != null) {
                                // Apply non-null values from new customer
                                if (customer.getName() != null) {
                                    existingCustomer.setName(customer.getName());
                                }
                                if (customer.getEmail() != null) {
                                    existingCustomer.setEmail(customer.getEmail());
                                }
                                if (customer.getPhone() != null) {
                                    existingCustomer.setPhone(customer.getPhone());
                                }
                                if (customer.getAddress() != null) {
                                    existingCustomer.setAddress(customer.getAddress());
                                }

                                // Update customer
                                customerService.updateCustomer(existingCustomer.getId(), existingCustomer);
                            } else {
                                // Create new customer
                                customerService.createCustomer(customer);
                            }
                        } else {
                            // Create new customer
                            customerService.createCustomer(customer);
                        }
                        break;

                    case "REPLACE":
                        if (customer.getId() != null) {
                            // Check if customer exists
                            Customer existingCustomer = customerMapper.selectById(customer.getId());
                            if (existingCustomer != null) {
                                // Replace customer
                                customerService.updateCustomer(existingCustomer.getId(), customer);
                            } else {
                                // Create new customer
                                customerService.createCustomer(customer);
                            }
                        } else {
                            // Create new customer
                            customerService.createCustomer(customer);
                        }
                        break;

                    case "APPEND":
                        // Always create new customer
                        customer.setId(null); // Ensure ID is null to create new entry
                        customerService.createCustomer(customer);
                        break;

                    default:
                        throw new RuntimeException("Unsupported integration strategy: " + job.getIntegrationStrategy());
                }

                success++;
            } catch (Exception e) {
                log.error("Error processing record: {}", sourceRecord, e);
                failed++;
            }
        }

        // Update job with counts
        job.setRecordsProcessed(processed);
        job.setRecordsSuccess(success);
        job.setRecordsFailed(failed);
        integrationJobMapper.updateById(job);
    }

    /**
     * Update the status of an integration job
     */
    private void updateJobStatus(IntegrationJob job, String status, String errorMessage) {
        job.setStatus(status);

        if ("PROCESSING".equals(status)) {
            job.setStartTime(LocalDateTime.now());
        } else if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            job.setEndTime(LocalDateTime.now());
        }

        if (errorMessage != null) {
            job.setErrorMessage(errorMessage);
        }

        integrationJobMapper.updateById(job);

        // Update Redis cache
        IntegrationStatus statusObj = IntegrationStatus.builder()
                .integrationId(job.getIntegrationId())
                .sourceName(job.getSourceName())
                .targetEntity(job.getTargetEntity())
                .status(job.getStatus())
                .startTime(job.getStartTime())
                .endTime(job.getEndTime())
                .recordsProcessed(job.getRecordsProcessed())
                .recordsSuccess(job.getRecordsSuccess())
                .recordsFailed(job.getRecordsFailed())
                .errorMessage(job.getErrorMessage())
                .build();

        redisTemplate.opsForValue().set(
                INTEGRATION_STATUS_KEY + job.getIntegrationId(),
                statusObj,
                24,
                TimeUnit.HOURS
        );
    }

    /**
     * Fetch data from the source based on source type and configuration
     */
    private List<Map<String, Object>> fetchSourceData(String sourceType, Map<String, Object> sourceConfig) {
        List<Map<String, Object>> data = new ArrayList<>();

        switch (sourceType.toUpperCase()) {
            case "CSV":
                data = fetchCsvData(sourceConfig);
                break;
            case "JSON":
                data = fetchJsonData(sourceConfig);
                break;
            case "API":
                data = fetchApiData(sourceConfig);
                break;
            case "MOCK":
                data = generateMockData(sourceConfig);
                break;
            default:
                throw new RuntimeException("Unsupported source type: " + sourceType);
        }

        return data;
    }

    /**
     * Generate mock data for testing
     */
    private List<Map<String, Object>> generateMockData(Map<String, Object> sourceConfig) {
        int count = 10;
        if (sourceConfig.containsKey("count")) {
            count = Integer.parseInt(sourceConfig.get("count").toString());
        }

        List<Map<String, Object>> mockData = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", i + 100L);
            record.put("name", "Integration User " + i);
            record.put("email", "integration_user" + i + "@example.com");
            record.put("phone", "555-" + String.format("%04d", i));
            record.put("address", i + " Integration Street");
            mockData.add(record);
        }

        return mockData;
    }

    /**
     * Fetch data from a CSV file
     */
    private List<Map<String, Object>> fetchCsvData(Map<String, Object> sourceConfig) {
        // In a real implementation, this would read from a CSV file
        log.info("Fetching CSV data with config: {}", sourceConfig);
        // For now, return mock data
        return generateMockData(sourceConfig);
    }

    /**
     * Fetch data from JSON
     */
    private List<Map<String, Object>> fetchJsonData(Map<String, Object> sourceConfig) {
        // In a real implementation, this would parse JSON data
        log.info("Fetching JSON data with config: {}", sourceConfig);
        // For now, return mock data
        return generateMockData(sourceConfig);
    }

    /**
     * Fetch data from an API
     */
    private List<Map<String, Object>> fetchApiData(Map<String, Object> sourceConfig) {
        // In a real implementation, this would call an external API
        log.info("Fetching API data with config: {}", sourceConfig);
        // For now, return mock data
        return generateMockData(sourceConfig);
    }

    /**
     * Serialize an object to JSON string
     */
    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error serializing to JSON", e);
            return "{}";
        }
    }

    /**
     * Deserialize from JSON string
     */
    private <T> T deserializeFromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.error("Error deserializing from JSON", e);
            try {
                return type.newInstance();
            } catch (Exception ex) {
                throw new RuntimeException("Error creating new instance", ex);
            }
        }
    }
}