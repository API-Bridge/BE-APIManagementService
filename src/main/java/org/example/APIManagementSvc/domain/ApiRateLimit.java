package org.example.APIManagementSvc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * API Rate Limiting 엔티티
 * API별 요청 횟수 제한 및 초기화를 위한 엔티티
 */
@Entity
@Table(name = "api_rate_limits", indexes = {
    @Index(name = "idx_api_id", columnList = "api_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_reset_at", columnList = "reset_at"),
    @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
public class ApiRateLimit {

    /** Rate Limit ID */
    @Id
    @Column(name = "rate_limit_id", nullable = false, length = 36)
    private String rateLimitId;

    /** API ID */
    @Column(name = "api_id", nullable = false, length = 36)
    private String apiId;

    /** 사용자 ID (선택사항) */
    @Column(name = "user_id", length = 100)
    private String userId;

    /** IP 주소 (선택사항) */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** 제한 횟수 */
    @Column(name = "limit_count", nullable = false)
    private Integer limitCount;

    /** 현재 사용 횟수 */
    @Column(name = "current_count", nullable = false)
    private Integer currentCount = 0;

    /** 제한 단위 (MINUTE, HOUR, DAY) */
    @Column(name = "limit_unit", length = 20)
    private String limitUnit = "MINUTE";

    /** 리셋 일시 */
    @Column(name = "reset_at", nullable = false)
    private LocalDateTime resetAt;

    /** 상태 (ACTIVE, SUSPENDED) */
    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    /** 마지막 요청 일시 */
    @Column(name = "last_request_at")
    private LocalDateTime lastRequestAt;

    /** 첫 요청 일시 */
    @Column(name = "first_request_at")
    private LocalDateTime firstRequestAt;

    /** 설명 */
    @Column(name = "description", length = 500)
    private String description;

    /** 메타데이터 */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    // === 비즈니스 로직 메서드 ===

    /**
     * 요청 처리
     */
    public boolean processRequest() {
        if (!"ACTIVE".equals(status)) {
            return false;
        }

        // 리셋 시간이 지났으면 카운트 초기화
        if (LocalDateTime.now().isAfter(resetAt)) {
            resetCount();
        }

        // 제한에 도달했는지 확인
        if (currentCount >= limitCount) {
            return false;
        }

        // 요청 처리
        currentCount++;
        lastRequestAt = LocalDateTime.now();
        
        if (firstRequestAt == null) {
            firstRequestAt = LocalDateTime.now();
        }

        return true;
    }

    /**
     * 제한 확인
     */
    public boolean isLimitExceeded() {
        if (!"ACTIVE".equals(status)) {
            return true;
        }

        // 리셋 시간이 지났으면 카운트 초기화
        if (LocalDateTime.now().isAfter(resetAt)) {
            resetCount();
        }

        return currentCount >= limitCount;
    }

    /**
     * 카운트 초기화
     */
    public void resetCount() {
        this.currentCount = 0;
        this.firstRequestAt = null;
        updateResetTime();
    }

    /**
     * 리셋 시간 업데이트
     */
    public void updateResetTime() {
        LocalDateTime now = LocalDateTime.now();
        switch (limitUnit) {
            case "MINUTE":
                this.resetAt = now.plusMinutes(1);
                break;
            case "HOUR":
                this.resetAt = now.plusHours(1);
                break;
            case "DAY":
                this.resetAt = now.plusDays(1);
                break;
            default:
                this.resetAt = now.plusMinutes(1);
        }
    }

    /**
     * 남은 요청 횟수 계산
     */
    public int getRemainingRequests() {
        return Math.max(0, limitCount - currentCount);
    }

    /**
     * 사용률 계산 (0.0 ~ 1.0)
     */
    public double getUsageRate() {
        return (double) currentCount / limitCount;
    }
}
