package org.example.APIManagementSvc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * API 캐싱 엔티티
 * 자주 요청되는 API 응답을 캐싱하기 위한 엔티티
 */
@Entity
@Table(name = "api_cache", indexes = {
    @Index(name = "idx_api_id", columnList = "api_id"),
    @Index(name = "idx_cache_key", columnList = "cache_key"),
    @Index(name = "idx_expires_at", columnList = "expires_at"),
    @Index(name = "idx_last_accessed", columnList = "last_accessed")
})
@Getter
@Setter
public class ApiCache {

    /** Cache ID */
    @Id
    @Column(name = "cache_id", nullable = false, length = 36)
    private String cacheId;

    /** API ID */
    @Column(name = "api_id", nullable = false, length = 36)
    private String apiId;

    /** 캐시 키 */
    @Column(name = "cache_key", nullable = false, length = 500)
    private String cacheKey;

    /** 캐시된 응답 데이터 */
    @Column(name = "cached_response", columnDefinition = "TEXT")
    private String cachedResponse;

    /** 응답 헤더 */
    @Column(name = "response_headers", columnDefinition = "TEXT")
    private String responseHeaders;

    /** HTTP 상태 코드 */
    @Column(name = "status_code")
    private Integer statusCode;

    /** 캐시 만료 일시 */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 마지막 접근 일시 */
    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    /** 접근 횟수 */
    @Column(name = "access_count")
    private Long accessCount = 0L;

    /** 캐시 크기 (바이트) */
    @Column(name = "cache_size")
    private Long cacheSize;

    /** 캐시 상태 (ACTIVE, EXPIRED, INVALID) */
    @Column(name = "cache_status", length = 20)
    private String cacheStatus = "ACTIVE";

    /** 캐시 타입 (RESPONSE, METADATA, STATISTICS) */
    @Column(name = "cache_type", length = 20)
    private String cacheType = "RESPONSE";

    /** 메타데이터 */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    // === 비즈니스 로직 메서드 ===

    /**
     * 캐시 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 캐시 접근 기록
     */
    public void recordAccess() {
        this.accessCount++;
        this.lastAccessed = LocalDateTime.now();
    }

    /**
     * 캐시 무효화
     */
    public void invalidate() {
        this.cacheStatus = "INVALID";
    }

    /**
     * 캐시 만료 처리
     */
    public void markAsExpired() {
        this.cacheStatus = "EXPIRED";
    }

    /**
     * 캐시 활성화
     */
    public void activate() {
        this.cacheStatus = "ACTIVE";
    }

    /**
     * 캐시 유효성 확인
     */
    public boolean isValid() {
        return "ACTIVE".equals(cacheStatus) && !isExpired();
    }

    /**
     * 캐시 크기 업데이트
     */
    public void updateCacheSize() {
        if (cachedResponse != null) {
            this.cacheSize = (long) cachedResponse.getBytes().length;
        }
    }
}
