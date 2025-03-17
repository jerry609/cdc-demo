-- Create integration_jobs table
CREATE TABLE IF NOT EXISTS integration_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    integration_id VARCHAR(36) NOT NULL,
    source_name VARCHAR(100) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    target_entity VARCHAR(50) NOT NULL,
    integration_strategy VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    request_time TIMESTAMP NULL,
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    records_processed BIGINT DEFAULT 0,
    records_success BIGINT DEFAULT 0,
    records_failed BIGINT DEFAULT 0,
    error_message TEXT NULL,
    field_mappings TEXT NULL,
    source_config TEXT NULL,
    UNIQUE KEY (integration_id),
    INDEX idx_status (status),
    INDEX idx_source_name (source_name),
    INDEX idx_target_entity (target_entity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;