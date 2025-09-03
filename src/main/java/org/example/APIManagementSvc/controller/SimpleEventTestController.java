package org.example.APIManagementSvc.controller;

import org.example.APIManagementSvc.event.model.*;
import org.springframework.kafka.core.KafkaTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 간단한 Kafka 이벤트 발행 테스트용 컨트롤러
 * EventPublisher 없이 직접 KafkaTemplate 사용
 */
@RestController
@RequestMapping("/api/v1/simple-test/events")
@RequiredArgsConstructor
@Slf4j
public class SimpleEventTestController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 외부 API 등록 이벤트 간단 테스트 발행
     */
    @PostMapping("/api-registered")
    public ResponseEntity<Map<String, Object>> publishApiRegisteredEvent() {
        String testApiId = UUID.randomUUID().toString();
        
        ExternalApiRegisteredEvent event = new ExternalApiRegisteredEvent(
                testApiId,
                "Simple Test API",
                "https://simple-test-api.example.com",
                "Simple test API description",
                "TEST",
                "1.0",
                "test-user"
        );

        try {
            log.info("Attempting to send event to topic: external_api_events, key: {}, event: {}", testApiId, event);
            kafkaTemplate.send("external_api_events", testApiId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send message to Kafka: ", ex);
                    } else {
                        log.info("Successfully sent message to Kafka: partition={}, offset={}", 
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
            log.info("Successfully published API Registered Event: {}", testApiId);
            
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "API Registered Event published",
                    "eventId", event.getEventId(),
                    "apiId", testApiId,
                    "topic", "external_api_events"
            ));
        } catch (Exception e) {
            log.error("Failed to publish event", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to publish event: " + e.getMessage()
            ));
        }
    }

    /**
     * 외부 API 삭제 이벤트 간단 테스트 발행
     */
    @PostMapping("/api-deleted")
    public ResponseEntity<Map<String, Object>> publishApiDeletedEvent() {
        String testApiId = UUID.randomUUID().toString();
        ExternalApiDeletedEvent event = new ExternalApiDeletedEvent(
                testApiId,
                "Simple Test API",
                "https://simple-test-api.example.com",
                "test-user",
                "Simple test deletion"
        );

        try {
            kafkaTemplate.send("external_api_events", testApiId, event);
            log.info("Successfully published API Deleted Event: {}", testApiId);
            
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "API Deleted Event published",
                    "eventId", event.getEventId(),
                    "apiId", testApiId,
                    "topic", "external_api_events"
            ));
        } catch (Exception e) {
            log.error("Failed to publish event", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to publish event: " + e.getMessage()
            ));
        }
    }

    /**
     * 모든 이벤트 타입을 순차적으로 발행하는 간단한 테스트
     */
    @PostMapping("/all-events")
    public ResponseEntity<Map<String, Object>> publishAllEvents() {
        try {
            String testApiIdStr = UUID.randomUUID().toString();
            
            // 1. API 등록 이벤트
            ExternalApiRegisteredEvent registeredEvent = new ExternalApiRegisteredEvent(
                    testApiIdStr, "Simple Integration Test API", "https://simple-test-api.example.com",
                    "Simple integration test API", "TEST", "1.0", "test-user"
            );
            kafkaTemplate.send("external_api_events", testApiIdStr, registeredEvent);
            
            // 2. API 삭제 이벤트
            ExternalApiDeletedEvent deletedEvent = new ExternalApiDeletedEvent(
                    testApiIdStr, "Simple Integration Test API", "https://simple-test-api.example.com",
                    "test-user", "Simple integration test cleanup"
            );
            kafkaTemplate.send("external_api_events", testApiIdStr, deletedEvent);
            
            log.info("Successfully published all simple events for API: {}", testApiIdStr);
            
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "All simple event types published successfully",
                    "testApiId", testApiIdStr,
                    "eventsPublished", 2,
                    "events", Map.of(
                            "registered", registeredEvent.getEventId(),
                            "deleted", deletedEvent.getEventId()
                    )
            ));
        } catch (Exception e) {
            log.error("Failed to publish events", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to publish events: " + e.getMessage()
            ));
        }
    }

    /**
     * 간단한 이벤트 시스템 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSimpleEventSystemStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "ACTIVE",
                "description", "Simple Event Test Controller",
                "availableTopics", Map.of(
                        "external_api_events", "외부 API 관련 이벤트 (등록, 삭제)"
                ),
                "testEndpoints", Map.of(
                        "POST /api/v1/simple-test/events/api-registered", "간단한 API 등록 이벤트 발행",
                        "POST /api/v1/simple-test/events/api-deleted", "간단한 API 삭제 이벤트 발행",
                        "POST /api/v1/simple-test/events/all-events", "모든 간단한 이벤트 타입 발행",
                        "GET /api/v1/simple-test/events/status", "간단한 이벤트 시스템 상태 확인"
                )
        ));
    }
}