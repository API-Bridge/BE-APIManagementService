package org.example.APIManagementSvc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * API 로그 엔티티
 * API 사용 로그 및 감사를 위한 엔티티
 */
@Entity
@Table(name = "api_logs", indexes = {
    @Index(name = "idx_log_type", columnList = "log_type"),
    @Index(name = "idx_api_id", columnList = "api_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_provider", columnList = "provider")
})
@Getter
@Setter
public class ApiLog {

    /** Log ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그 타입 */
    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false, length = 50)
    private LogType logType;

    /** API ID */
    @Column(name = "api_id", length = 36)
    private String apiId;

    /** 사용자 ID */
    @Column(name = "user_id", length = 100)
    private String userId;

    /** 관리자 ID */
    @Column(name = "admin_id", length = 100)
    private String adminId;

    /** 로그 메시지 */
    @Column(name = "message", length = 500)
    private String message;

    /** 상세 정보 */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /** 처리 시간 (밀리초) */
    @Column(name = "duration")
    private Long duration;

    /** 성공 여부 */
    @Column(name = "success")
    private Boolean success;

    /** 에러 코드 */
    @Column(name = "error_code", length = 50)
    private String errorCode;

    /** 에러 메시지 */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /** 요청 데이터 */
    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;

    /** 응답 데이터 */
    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    /** IP 주소 */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** User Agent */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /** 세션 ID */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /** 요청 ID */
    @Column(name = "request_id", length = 100)
    private String requestId;

    /** 로그 레벨 */
    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", length = 20)
    private LogLevel logLevel = LogLevel.INFO;

    /** 태그 */
    @Column(name = "tags", length = 500)
    private String tags;

    /** 관련 로그 ID */
    @Column(name = "related_log_id")
    private Long relatedLogId;

    /** 로그 그룹 ID */
    @Column(name = "log_group_id", length = 100)
    private String logGroupId;

    /** 제공자 */
    @Column(name = "provider", length = 100)
    private String provider;

    /** 생성 일시 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // === 로그 타입 ENUM ===
    public enum LogType {
        API_CREATION("API 생성"),
        API_UPDATE("API 수정"),
        API_DELETION("API 삭제"),
        API_USAGE("API 사용"),
        TOKEN_REFRESH("토큰 갱신"),
        RATE_LIMIT_EXCEEDED("Rate Limit 초과"),
        CACHE_HIT("캐시 히트"),
        CACHE_MISS("캐시 미스"),
        AI_CLASSIFICATION("AI 분류"),
        ERROR("에러"),
        AUTHENTICATION("인증"),
        AUTHORIZATION("권한"),
        SYSTEM("시스템");

        private final String description;

        LogType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // === 로그 레벨 ENUM ===
    public enum LogLevel {
        DEBUG("디버그"),
        INFO("정보"),
        WARN("경고"),
        ERROR("에러"),
        FATAL("치명적");

        private final String description;

        LogLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // === 정적 팩토리 메서드 ===

    /**
     * API 생성 로그
     */
    public static ApiLog createApiCreationLog(String apiId, String userId, String message) {
        ApiLog log = new ApiLog();
        log.setLogType(LogType.API_CREATION);
        log.setApiId(apiId);
        log.setUserId(userId);
        log.setMessage(message);
        log.setSuccess(true);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    /**
     * API 사용 로그
     */
    public static ApiLog createApiUsageLog(String apiId, String userId, Long duration, Boolean success) {
        ApiLog log = new ApiLog();
        log.setLogType(LogType.API_USAGE);
        log.setApiId(apiId);
        log.setUserId(userId);
        log.setDuration(duration);
        log.setSuccess(success);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    /**
     * 토큰 갱신 로그
     */
    public static ApiLog createTokenRefreshLog(String apiId, Boolean success, String message) {
        ApiLog log = new ApiLog();
        log.setLogType(LogType.TOKEN_REFRESH);
        log.setApiId(apiId);
        log.setSuccess(success);
        log.setMessage(message);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    /**
     * Rate Limit 초과 로그
     */
    public static ApiLog createRateLimitLog(String apiId, String userId, String ipAddress) {
        ApiLog log = new ApiLog();
        log.setLogType(LogType.RATE_LIMIT_EXCEEDED);
        log.setApiId(apiId);
        log.setUserId(userId);
        log.setIpAddress(ipAddress);
        log.setSuccess(false);
        log.setMessage("Rate limit exceeded");
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    /**
     * AI 분류 로그
     */
    public static ApiLog createAiClassificationLog(String apiId, Boolean success, String message) {
        ApiLog log = new ApiLog();
        log.setLogType(LogType.AI_CLASSIFICATION);
        log.setApiId(apiId);
        log.setSuccess(success);
        log.setMessage(message);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    /**
     * 에러 로그
     */
    public static ApiLog createErrorLog(String apiId, String errorCode, String errorMessage) {
        ApiLog log = new ApiLog();
        log.setLogType(LogType.ERROR);
        log.setApiId(apiId);
        log.setErrorCode(errorCode);
        log.setErrorMessage(errorMessage);
        log.setSuccess(false);
        log.setLogLevel(LogLevel.ERROR);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }
}
