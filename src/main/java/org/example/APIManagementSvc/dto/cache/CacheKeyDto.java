package org.example.APIManagementSvc.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis 캐시 키 관리를 위한 DTO입니다.
 * 
 * ⚠️  중요: 이 DTO는 Redis 캐시 키의 구조와 만료 시간을 관리합니다!
 * - 일관된 캐시 키 네이밍 컨벤션 제공
 * - 캐시 만료 시간 설정 및 관리
 * - 캐시 키 패턴별 분류 및 관리
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheKeyDto {
    
    /**
     * 캐시 키 타입
     */
    private CacheKeyType keyType;
    
    /**
     * 캐시 키 문자열
     */
    private String key;
    
    /**
     * TTL (초)
     */
    private Long ttlSeconds;
    
    /**
     * 캐시 키 설명
     */
    private String description;
    
    /**
     * 캐시 키 패턴
     */
    private String pattern;
    
    /**
     * 생성 시간 (Unix timestamp)
     */
    private Long createdAt;
    
    /**
     * 마지막 접근 시간 (Unix timestamp)
     */
    private Long lastAccessedAt;
    
    /**
     * 접근 횟수
     */
    private Long accessCount;
    
    /**
     * 활성 상태
     */
    private Boolean isActive;
    
    /**
     * 캐시 키 타입 열거형
     */
    public enum CacheKeyType {
        /**
         * API 상태 정보 캐시
         */
        API_STATUS("api:status"),
        
        /**
         * API 사용량 통계 캐시
         */
        API_USAGE_STATS("api:usage");
        
        private final String prefix;
        
        CacheKeyType(String prefix) {
            this.prefix = prefix;
        }
        
        /**
         * 캐시 키 접두사 반환
         */
        public String getPrefix() {
            return prefix;
        }
        
        /**
         * 식별자를 포함한 전체 캐시 키 생성
         */
        public String buildKey(String identifier) {
            return prefix + ":" + identifier;
        }
        
        /**
         * 패턴 매칭을 위한 와일드카드 키 생성
         */
        public String getPatternKey() {
            return prefix + ":*";
        }
    }
    
    /**
     * 새로운 캐시 키 DTO 생성
     */
    public static CacheKeyDto create(CacheKeyType keyType, String identifier, Long ttlSeconds) {
        return CacheKeyDto.builder()
                .keyType(keyType)
                .key(keyType.buildKey(identifier))
                .ttlSeconds(ttlSeconds)
                .description(keyType.name() + " 캐시 키")
                .pattern(keyType.getPatternKey())
                .createdAt(System.currentTimeMillis() / 1000)
                .lastAccessedAt(System.currentTimeMillis() / 1000)
                .accessCount(0L)
                .isActive(true)
                .build();
    }
    
    /**
     * 캐시 키가 유효한지 확인
     */
    public boolean isValid() {
        return key != null && !key.isEmpty() && ttlSeconds != null && ttlSeconds > 0;
    }
    
    /**
     * 캐시 키 만료 여부 확인
     */
    public boolean isExpired() {
        if (createdAt == null || ttlSeconds == null) {
            return false;
        }
        long currentTime = System.currentTimeMillis() / 1000;
        return (currentTime - createdAt) > ttlSeconds;
    }
    
    /**
     * 접근 시간 업데이트
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = System.currentTimeMillis() / 1000;
        this.accessCount = (this.accessCount != null ? this.accessCount : 0) + 1;
    }
}
