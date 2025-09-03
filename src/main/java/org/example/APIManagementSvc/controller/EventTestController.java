package org.example.APIManagementSvc.controller;

import org.example.APIManagementSvc.event.model.*;
import org.example.APIManagementSvc.event.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka 이벤트 발행/구독 테스트용 컨트롤러
 * 
 * 이벤트 시스템의 동작을 확인하기 위한 테스트 엔드포인트를 제공
 * 각 이벤트 타입별로 발행 테스트 가능
 */
@RestController
@RequestMapping("/api/v1/test/events")
@RequiredArgsConstructor
@Slf4j
public class EventTestController {

    private final EventPublisher eventPublisher;

    /**
     * 외부 API 등록 이벤트 테스트 발행
     */
    @PostMapping("/api-registered")
    public ResponseEntity<Map<String, Object>> publishApiRegisteredEvent(
            @RequestBody(required = false) Map<String, String> requestBody) {
        
        String apiId = requestBody != null ? requestBody.get("apiId") : UUID.randomUUID().toString();
        String apiName = requestBody != null ? requestBody.get("apiName") : "Test API";
        String apiUrl = requestBody != null ? requestBody.get("apiUrl") : "https://test-api.example.com";
        
        ExternalApiRegisteredEvent event = new ExternalApiRegisteredEvent(
                apiId,
                apiName,
                apiUrl,
                "Test API Description",
                "TEST",
                "1.0",
                "test-user"
        );

        eventPublisher.publishEvent("external_api_events", event);
        
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "API Registered Event published",
                "eventId", event.getEventId(),
                "apiId", apiId,
                "topic", "external_api_events"
        ));
    }

    /**
     * 외부 API 삭제 이벤트 테스트 발행
     */
    @PostMapping("/api-deleted")
    public ResponseEntity<Map<String, Object>> publishApiDeletedEvent(
            @RequestBody(required = false) Map<String, String> requestBody) {
        
        String apiId = requestBody != null ? requestBody.get("apiId") : UUID.randomUUID().toString();
        String apiName = requestBody != null ? requestBody.get("apiName") : "Test API";
        
        ExternalApiDeletedEvent event = new ExternalApiDeletedEvent(
                apiId,
                apiName,
                "https://test-api.example.com",
                "test-user",
                "Test deletion"
        );

        eventPublisher.publishEvent("external_api_events", event);
        
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "API Deleted Event published",
                "eventId", event.getEventId(),
                "apiId", apiId,
                "topic", "external_api_events"
        ));
    }

    /**
     * 외부 API 헬스체크 이벤트 테스트 발행
     */
    @PostMapping("/api-healthcheck")
    public ResponseEntity<Map<String, Object>> publishApiHealthCheckEvent(
            @RequestBody(required = false) Map<String, String> requestBody) {
        
        Long apiId = requestBody != null && requestBody.get("apiId") != null ? 
                     Long.parseLong(requestBody.get("apiId")) : Math.abs(UUID.randomUUID().hashCode()) % 1000000L;
        String apiName = requestBody != null ? requestBody.get("apiName") : "Test API";
        String status = requestBody != null ? requestBody.get("status") : "HEALTHY";
        
        ExternalApiHealthCheckEvent event = ExternalApiHealthCheckEvent.builder()
                .apiId(apiId)
                .apiName(apiName)
                .apiUrl("https://test-api.example.com")
                .status(status)
                .responseTime(150)
                .timestamp(LocalDateTime.now())
                .errorMessage("HEALTHY".equals(status) ? null : "Test error message")
                .build();
        event.setEventType("EXTERNAL_API_HEALTH_CHECK");
        event.setEventId(UUID.randomUUID().toString());
        event.setSourceService("api-management-svc");
        event.setCorrelationId(UUID.randomUUID().toString());

        eventPublisher.publishEvent("external_api_events", event);
        
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "API Health Check Event published",
                "eventId", event.getEventId(),
                "apiId", apiId,
                "healthStatus", status,
                "topic", "external_api_events"
        ));
    }

    /**
     * 외부 API 호출 실패 이벤트 테스트 발행
     */
    @PostMapping("/api-call-failed")
    public ResponseEntity<Map<String, Object>> publishApiCallFailedEvent(
            @RequestBody(required = false) Map<String, String> requestBody) {
        
        Long apiId = requestBody != null && requestBody.get("apiId") != null ? 
                     Long.parseLong(requestBody.get("apiId")) : Math.abs(UUID.randomUUID().hashCode()) % 1000000L;
        String apiName = requestBody != null ? requestBody.get("apiName") : "Test API";
        String errorType = requestBody != null ? requestBody.get("errorType") : "TIMEOUT";
        
        ExternalApiCallFailedEvent.ExternalApiCallFailedPayload payload = 
                ExternalApiCallFailedEvent.ExternalApiCallFailedPayload.builder()
                .apiId(String.valueOf(apiId))
                .apiName(apiName)
                .apiUrl("https://test-api.example.com")
                .httpMethod("GET")
                .statusCode(500)
                .errorMessage("Test error: Connection timeout")
                .errorType(errorType)
                .responseTime(5000)
                .failedAt(LocalDateTime.now())
                .requestId(UUID.randomUUID().toString())
                .calledBy("test-controller")
                .build();

        ExternalApiCallFailedEvent event = ExternalApiCallFailedEvent.builder()
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();
        event.setEventType("EXTERNAL_API_CALL_FAILED");
        event.setEventId(UUID.randomUUID().toString());
        event.setSourceService("api-management-svc");
        event.setCorrelationId(UUID.randomUUID().toString());

        eventPublisher.publishEvent("external_api_called_failed", event);
        
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "API Call Failed Event published",
                "eventId", event.getEventId(),
                "apiId", apiId,
                "errorType", errorType,
                "topic", "external_api_called_failed"
        ));
    }

    /**
     * 모든 이벤트 타입을 순차적으로 발행하는 통합 테스트
     */
    @PostMapping("/all-events")
    public ResponseEntity<Map<String, Object>> publishAllEvents() {
        
        String testApiIdStr = UUID.randomUUID().toString();
        Long testApiId = Math.abs(testApiIdStr.hashCode()) % 1000000L;
        String testApiName = "Integration Test API";
        
        // 1. API 등록 이벤트
        ExternalApiRegisteredEvent registeredEvent = new ExternalApiRegisteredEvent(
                testApiIdStr, testApiName, "https://test-api.example.com",
                "Integration test API", "TEST", "1.0", "test-user"
        );
        eventPublisher.publishEvent("external_api_events", registeredEvent);
        
        // 2. API 헬스체크 이벤트 (정상)
        ExternalApiHealthCheckEvent healthyEvent = ExternalApiHealthCheckEvent.builder()
                .apiId(testApiId)
                .apiName(testApiName)
                .apiUrl("https://test-api.example.com")
                .status("HEALTHY")
                .responseTime(120)
                .build();
        healthyEvent.setEventType("EXTERNAL_API_HEALTH_CHECK");
        healthyEvent.setEventId(UUID.randomUUID().toString());
        healthyEvent.setSourceService("api-management-svc");
        healthyEvent.setCorrelationId(UUID.randomUUID().toString());
        eventPublisher.publishEvent("external_api_events", healthyEvent);
        
        // 3. API 호출 실패 이벤트
        ExternalApiCallFailedEvent.ExternalApiCallFailedPayload failedPayload = 
                ExternalApiCallFailedEvent.ExternalApiCallFailedPayload.builder()
                .apiId(testApiIdStr)
                .apiName(testApiName)
                .apiUrl("https://test-api.example.com")
                .httpMethod("POST")
                .statusCode(503)
                .errorMessage("Connection refused")
                .errorType("CONNECTION_ERROR")
                .responseTime(3000)
                .failedAt(LocalDateTime.now())
                .requestId(UUID.randomUUID().toString())
                .calledBy("integration-test")
                .build();

        ExternalApiCallFailedEvent failedEvent = ExternalApiCallFailedEvent.builder()
                .payload(failedPayload)
                .timestamp(LocalDateTime.now())
                .build();
        failedEvent.setEventType("EXTERNAL_API_CALL_FAILED");
        failedEvent.setEventId(UUID.randomUUID().toString());
        failedEvent.setSourceService("api-management-svc");
        failedEvent.setCorrelationId(UUID.randomUUID().toString());
        eventPublisher.publishEvent("external_api_called_failed", failedEvent);
        
        // 4. API 헬스체크 이벤트 (비정상)
        ExternalApiHealthCheckEvent unhealthyEvent = ExternalApiHealthCheckEvent.builder()
                .apiId(testApiId)
                .apiName(testApiName)
                .apiUrl("https://test-api.example.com")
                .status("UNHEALTHY")
                .responseTime(5000)
                .errorMessage("Service unavailable")
                .build();
        unhealthyEvent.setEventType("EXTERNAL_API_HEALTH_CHECK");
        unhealthyEvent.setEventId(UUID.randomUUID().toString());
        unhealthyEvent.setSourceService("api-management-svc");
        unhealthyEvent.setCorrelationId(UUID.randomUUID().toString());
        eventPublisher.publishEvent("external_api_events", unhealthyEvent);
        
        // 5. API 삭제 이벤트
        ExternalApiDeletedEvent deletedEvent = new ExternalApiDeletedEvent(
                testApiIdStr, testApiName, "https://test-api.example.com",
                "test-user", "Integration test cleanup"
        );
        eventPublisher.publishEvent("external_api_events", deletedEvent);
        
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "All event types published successfully",
                "testApiId", testApiIdStr,
                "eventsPublished", 5,
                "events", Map.of(
                        "registered", registeredEvent.getEventId(),
                        "healthyCheck", healthyEvent.getEventId(),
                        "callFailed", failedEvent.getEventId(),
                        "unhealthyCheck", unhealthyEvent.getEventId(),
                        "deleted", deletedEvent.getEventId()
                )
        ));
    }

    /**
     * 이벤트 시스템 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getEventSystemStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "ACTIVE",
                "availableTopics", Map.of(
                        "external_api_events", "외부 API 관련 이벤트 (등록, 삭제, 헬스체크)",
                        "external_api_called_failed", "API 호출 실패 이벤트"
                ),
                "availableEventTypes", Map.of(
                        "EXTERNAL_API_REGISTERED", "API 등록 이벤트",
                        "EXTERNAL_API_DELETED", "API 삭제 이벤트",
                        "EXTERNAL_API_HEALTH_CHECK", "API 헬스체크 이벤트",
                        "EXTERNAL_API_CALL_FAILED", "API 호출 실패 이벤트"
                ),
                "testEndpoints", Map.of(
                        "POST /api/v1/test/events/api-registered", "API 등록 이벤트 발행",
                        "POST /api/v1/test/events/api-deleted", "API 삭제 이벤트 발행",
                        "POST /api/v1/test/events/api-healthcheck", "API 헬스체크 이벤트 발행",
                        "POST /api/v1/test/events/api-call-failed", "API 호출 실패 이벤트 발행",
                        "POST /api/v1/test/events/all-events", "모든 이벤트 타입 발행",
                        "GET /api/v1/test/events/status", "이벤트 시스템 상태 확인"
                )
        ));
    }
}