package org.example.APIManagementSvc.dto.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 🔥 로그 엔트리 DTO
 * 다른 MSA에서 ELK 스택으로 로그를 수집할 수 있도록 제공
 * 
 * 중요: 이 DTO는 데이터베이스에 저장되지 않고 ELK 엔드포인트로만 전송됩니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {

    /** 서비스명 */
    @Builder.Default
    private String serviceName = "API-Management-Service";

    /** 로그 레벨 (INFO, ERROR, WARN, DEBUG, TRACE) */
    private String level;

    /** 로그 메시지 */
    private String message;

    /** 타임스탬프 */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /** 로그 발생 위치 (클래스명.메서드명) */
    private String location;

    /** 로그 발생 스레드 */
    private String threadName;

    /** 요청 ID (MSA 간 트랜잭션 추적용) */
    private String requestId;

    /** 사용자 ID (인증된 사용자의 경우) */
    private String userId;

    /** API 엔드포인트 (API 호출의 경우) */
    private String endpoint;

    /** HTTP 메소드 (API 호출의 경우) */
    private String httpMethod;

    /** HTTP 상태 코드 (API 호출의 경우) */
    private Integer statusCode;

    /** 응답 시간 (밀리초, API 호출의 경우) */
    private Long responseTime;

    /** 클라이언트 IP 주소 */
    private String clientIp;

    /** User-Agent */
    private String userAgent;

    /** 🔥 추가 메타데이터 (유연한 확장을 위한 Map) */
    private Map<String, Object> metadata;

    /**
     * 🔥 간단한 로그 엔트리 생성 (기본 정보만)
     */
    public static LogEntry simple(String level, String message) {
        return LogEntry.builder()
            .level(level)
            .message(message)
            .build();
    }

    /**
     * 🔥 API 호출 로그 생성
     */
    public static LogEntry apiCall(String endpoint, String httpMethod, Integer statusCode, Long responseTime) {
        return LogEntry.builder()
            .level("INFO")
            .message("API 호출 완료")
            .endpoint(endpoint)
            .httpMethod(httpMethod)
            .statusCode(statusCode)
            .responseTime(responseTime)
            .build();
    }

    /**
     * 🔥 에러 로그 생성
     */
    public static LogEntry error(String message, String location, Throwable exception) {
        return LogEntry.builder()
            .level("ERROR")
            .message(message)
            .location(location)
            .metadata(Map.of(
                "exceptionType", exception.getClass().getSimpleName(),
                "exceptionMessage", exception.getMessage()
            ))
            .build();
    }

    /**
     * 🔥 비즈니스 로그 생성
     */
    public static LogEntry business(String message, String operation, Map<String, Object> businessData) {
        return LogEntry.builder()
            .level("INFO")
            .message(message)
            .location(operation)
            .metadata(businessData)
            .build();
    }
}
