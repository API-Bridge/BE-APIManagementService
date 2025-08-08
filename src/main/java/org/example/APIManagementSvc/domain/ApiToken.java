package org.example.APIManagementSvc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * API 토큰 관리 엔티티
 * API 엑세스키 및 토큰 갱신 스케줄링을 위한 엔티티
 */
@Entity
@Table(name = "api_tokens", indexes = {
    @Index(name = "idx_api_id", columnList = "api_id"),
    @Index(name = "idx_token_status", columnList = "token_status"),
    @Index(name = "idx_expires_at", columnList = "expires_at"),
    @Index(name = "idx_refresh_status", columnList = "refresh_status")
})
@Getter
@Setter
public class ApiToken {

    /** Token ID */
    @Id
    @Column(name = "token_id", nullable = false, length = 36)
    private String tokenId;

    /** API ID */
    @Column(name = "api_id", nullable = false, length = 36)
    private String apiId;

    /** 토큰 값 */
    @Column(name = "token_value", columnDefinition = "TEXT")
    private String tokenValue;

    /** 토큰 타입 (API_KEY, OAUTH2, BEARER 등) */
    @Column(name = "token_type", length = 50)
    private String tokenType;

    /** 만료 일시 */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** 갱신 주기 (분) */
    @Column(name = "refresh_interval")
    private Integer refreshInterval = 60;

    /** 마지막 갱신 일시 */
    @Column(name = "last_refresh_at")
    private LocalDateTime lastRefreshAt;

    /** 갱신 상태 (SUCCESS, FAILED, PENDING) */
    @Column(name = "refresh_status", length = 20)
    private String refreshStatus = "PENDING";

    /** 갱신 로그 */
    @Column(name = "refresh_log", columnDefinition = "TEXT")
    private String refreshLog;

    /** 토큰 상태 (ACTIVE, EXPIRED, REVOKED) */
    @Column(name = "token_status", length = 20)
    private String tokenStatus = "ACTIVE";

    /** 발급자 */
    @Column(name = "issuer", length = 100)
    private String issuer;

    /** 발급자 ID */
    @Column(name = "issued_by", length = 100)
    private String issuedBy;

    /** 발급 일시 */
    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    /** 사용 횟수 */
    @Column(name = "usage_count")
    private Long usageCount = 0L;

    /** 마지막 사용 일시 */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /** 메타데이터 */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    // === 비즈니스 로직 메서드 ===

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 갱신 필요 여부 확인
     */
    public boolean needsRefresh() {
        if (lastRefreshAt == null || refreshInterval == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(lastRefreshAt.plusMinutes(refreshInterval));
    }

    /**
     * 토큰 사용 기록
     */
    public void recordUsage() {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * 갱신 상태 업데이트
     */
    public void updateRefreshStatus(String status, String log) {
        this.refreshStatus = status;
        this.refreshLog = log;
        this.lastRefreshAt = LocalDateTime.now();
    }

    /**
     * 토큰 만료 처리
     */
    public void markAsExpired() {
        this.tokenStatus = "EXPIRED";
        this.refreshStatus = "FAILED";
    }
}
