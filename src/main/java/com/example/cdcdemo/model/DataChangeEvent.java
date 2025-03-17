package com.example.cdcdemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataChangeEvent implements Serializable {
    private String entityType;
    private Long entityId;
    private String operation; // CREATE, UPDATE, DELETE
    private Object data;
    private LocalDateTime timestamp;
}