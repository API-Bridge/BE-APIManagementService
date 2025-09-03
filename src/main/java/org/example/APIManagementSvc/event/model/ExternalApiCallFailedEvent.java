package org.example.APIManagementSvc.event.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 외부 API 호출 실패 이벤트
 * 외부 API 호출이 실패했을 때 발행되는 이벤트
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExternalApiCallFailedEvent extends BaseEvent {
    
    private ExternalApiCallFailedPayload payload;

    @Data
    @SuperBuilder
    @NoArgsConstructor
    public static class ExternalApiCallFailedPayload {
        private String apiId;
        private String apiName;
        private String apiUrl;
        private String httpMethod;
        private Integer statusCode; // HTTP 응답 상태 코드
        private String errorMessage; // 실패 원인
        private String errorType; // "TIMEOUT", "CONNECTION_ERROR", "HTTP_ERROR", "UNKNOWN"
        private Integer responseTime; // 응답 시간 (ms)
        private LocalDateTime failedAt; // 실패 시간
        private String requestId; // 요청 식별자 (추적용)
        private String calledBy; // 호출한 서비스/사용자
    }
}