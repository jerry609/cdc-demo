package com.example.cdcdemo.controller;

import com.example.cdcdemo.model.integration.DataIntegrationRequest;
import com.example.cdcdemo.model.integration.IntegrationStatus;
import com.example.cdcdemo.service.DataIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
public class DataIntegrationController {

    private final DataIntegrationService integrationService;

    @PostMapping
    public ResponseEntity<IntegrationStatus> submitIntegration(
            @Valid @RequestBody DataIntegrationRequest request) {
        // 设置请求时间（如果未提供）
        if (request.getRequestTime() == null) {
            request.setRequestTime(LocalDateTime.now());
        }

        IntegrationStatus status = integrationService.submitIntegration(request);
        // 构造 Location 头指向状态查询 URL
        String location = "/api/integration/" + status.getIntegrationId();
        return ResponseEntity
                .accepted() // 设置 202 Accepted 状态码
                .location(URI.create(location)) // 添加 Location 头
                .body(status); // 返回 IntegrationStatus
    }

    @GetMapping("/{integrationId}")
    public ResponseEntity<IntegrationStatus> getIntegrationStatus(
            @PathVariable String integrationId) {
        IntegrationStatus status = integrationService.getIntegrationStatus(integrationId);
        if (status != null) {
            return ResponseEntity.ok(status);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<IntegrationStatus>> getAllIntegrationJobs() {
        return ResponseEntity.ok(integrationService.getAllIntegrationJobs());
    }
}
