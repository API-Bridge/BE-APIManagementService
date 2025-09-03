package org.example.APIManagementSvc.event.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * AI 서비스에서 전송되는 외부 API 호출 실패 이벤트 래퍼
 * Kafka 메시지의 실제 구조에 맞춰 설계된 모델
 */
@Data
@SuperBuilder
@NoArgsConstructor
public class ExternalApiCallFailedEventWrapper {
    
    private String eventId;
    private String traceId;
    private String serviceName;
    private LocalDateTime timestamp;
    private String eventType;
    private ExternalApiCallFailedPayload payload;
    
    @Data
    @SuperBuilder
    @NoArgsConstructor
    public static class ExternalApiCallFailedPayload {
        private String apiId;
        private String apiName;
        private String apiUrl;
        private String httpMethod;
        private Integer statusCode;
        private String errorMessage;
        private String errorType;
        private Integer responseTime;
        private LocalDateTime failedAt;
        private String requestId;
        private String calledBy;
    }
}