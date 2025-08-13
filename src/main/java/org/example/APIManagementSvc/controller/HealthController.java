package org.example.APIManagementSvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * 시스템 상태 및 헬스 체크 엔드포인트 제공
 */
@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    /**
     * 기본 헬스 체크
     * GET /api/v1/health
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        log.debug("Health check requested");
        
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("service", "API Management Service");
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("version", "1.0.0");
        
        return ResponseEntity.ok(ApiResponse.success(healthInfo, "서비스가 정상 동작 중입니다."));
    }

    /**
     * 상세 헬스 체크
     * GET /api/v1/health/detailed
     */
    @GetMapping("/detailed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detailedHealthCheck() {
        log.debug("Detailed health check requested");
        
        Map<String, Object> detailedHealth = new HashMap<>();
        detailedHealth.put("service", "API Management Service");
        detailedHealth.put("status", "UP");
        detailedHealth.put("timestamp", LocalDateTime.now());
        detailedHealth.put("version", "1.0.0");
        detailedHealth.put("environment", "development");
        detailedHealth.put("javaVersion", System.getProperty("java.version"));
        detailedHealth.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        detailedHealth.put("memory", getMemoryInfo());
        
        return ResponseEntity.ok(ApiResponse.success(detailedHealth, "상세 헬스 체크가 완료되었습니다."));
    }

    /**
     * 메모리 정보 조회
     */
    private Map<String, Object> getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        Map<String, Object> memoryInfo = new HashMap<>();
        memoryInfo.put("total", formatBytes(totalMemory));
        memoryInfo.put("used", formatBytes(usedMemory));
        memoryInfo.put("free", formatBytes(freeMemory));
        memoryInfo.put("max", formatBytes(maxMemory));
        memoryInfo.put("usagePercent", String.format("%.2f%%", (double) usedMemory / totalMemory * 100));
        
        return memoryInfo;
    }

    /**
     * 바이트를 읽기 쉬운 형태로 변환
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}