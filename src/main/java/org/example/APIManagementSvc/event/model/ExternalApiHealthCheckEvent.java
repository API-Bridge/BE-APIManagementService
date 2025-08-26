package org.example.APIManagementSvc.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 외부 API 헬스체크 이벤트
 * 외부 API의 상태 점검 결과를 알리는 이벤트
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExternalApiHealthCheckEvent extends BaseEvent {
    
    private Long apiId;
    private String apiName;
    private String apiUrl;
    private String status; // "HEALTHY", "UNHEALTHY", "TIMEOUT", "ERROR"
    private Integer responseTime; // 응답 시간 (ms)
    private Integer statusCode; // HTTP 상태 코드
    private String errorMessage; // 오류 메시지 (비정상인 경우)
    private LocalDateTime checkedAt; // 체크 시간

}