package org.example.APIManagementSvc.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.APIManagementSvc.domain.enums.RateLimitStatus;

import java.time.LocalDateTime;

/**
 * Public API Rate Limit 로그 엔티티
 * API 호출 제한 관련 로그를 저장
 */
@Entity
@Table(name = "public_api_rate_limit_log")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PublicApiRateLimitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "api_name", nullable = false, length = 255)
    private String apiName;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "request_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_limit_status", nullable = false, length = 50)
    private RateLimitStatus rateLimitStatus;

    @Column(name = "current_usage_count")
    private Integer currentUsageCount;

    @Column(name = "max_allowed_count")
    private Integer maxAllowedCount;

    @Column(name = "reset_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resetTime;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        if (deleted == null) {
            deleted = false;
        }
        if (requestTime == null) {
            requestTime = LocalDateTime.now();
        }
    }

    /**
     * Rate Limit 초과 여부 확인
     */
    public boolean isRateLimitExceeded() {
        return RateLimitStatus.EXCEEDED.equals(rateLimitStatus);
    }

    /**
     * Rate Limit 경고 여부 확인
     */
    public boolean isRateLimitWarning() {
        return RateLimitStatus.WARNING.equals(rateLimitStatus);
    }

    /**
     * 사용량 비율 계산
     */
    public double getUsageRatio() {
        if (maxAllowedCount == null || maxAllowedCount == 0) {
            return 0.0;
        }
        return (double) currentUsageCount / maxAllowedCount;
    }
}
