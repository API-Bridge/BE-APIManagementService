package org.example.APIManagementSvc.service;

import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.dto.cache.ApiStatusCacheDto;
import org.example.APIManagementSvc.dto.cache.CacheKeyDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis를 사용한 API 상태 정보 캐싱 서비스입니다.
 * 
 * ⚠️  중요: 이 서비스는 Redis를 통한 고성능 캐싱을 제공합니다!
 * - API 상태 정보, 기본 정보, 사용량 통계 등을 Redis에 캐싱
 * - 캐시 만료 시간(TTL) 자동 관리
 * - 캐시 히트율 향상과 API 응답 속도 개선
 * - 메모리 기반의 빠른 데이터 접근
 * 
 * 주요 기능:
 * - API 상태 정보 캐싱 및 조회
 * - API 사용량 통계 캐싱 및 조회
 * - 캐시 키 관리 및 TTL 설정
 * - 캐시 통계 및 모니터링
 * - 캐시 무효화 및 갱신
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
public class RedisCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private final ValueOperations<String, Object> valueOps;
    private final ZSetOperations<String, Object> zSetOps;
    
    /**
     * 기본 캐시 만료 시간 (초)
     */
    private static final long DEFAULT_TTL_SECONDS = 3600; // 1시간
    
    /**
     * API 상태 정보 캐시 만료 시간 (초)
     */
    private static final long API_STATUS_TTL_SECONDS = 1800; // 30분
    
    /**
     * API 사용량 통계 캐시 만료 시간 (초)
     */
    private static final long API_USAGE_STATS_TTL_SECONDS = 7200; // 2시간
    
    public RedisCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
        this.zSetOps = redisTemplate.opsForZSet();
    }
    
    // ==================== API 상태 정보 캐싱 ====================
    
    /**
     * API 상태 정보를 캐시에 저장합니다.
     * 
     * ⚠️  중요: 구체적인 TTL 설정으로 캐시 만료 시간을 관리합니다!
     * 
     * @param apiId API 식별자
     * @param apiStatus API 상태 정보
     * @return 캐시 저장 성공 여부
     */
    public boolean cacheApiStatus(String apiId, ApiStatusCacheDto apiStatus) {
        try {
            CacheKeyDto cacheKey = CacheKeyDto.create(
                CacheKeyDto.CacheKeyType.API_STATUS, 
                apiId, 
                API_STATUS_TTL_SECONDS
            );
            
            // TTL 설정
            long ttl = apiStatus.getTtlSeconds() != null ? 
                apiStatus.getTtlSeconds() : API_STATUS_TTL_SECONDS;
            
            valueOps.set(cacheKey.getKey(), apiStatus, ttl, TimeUnit.SECONDS);
            
            log.debug("API 상태 정보 캐시 저장 완료: {} (TTL: {}초)", apiId, ttl);
            return true;
            
        } catch (Exception e) {
            log.error("API 상태 정보 캐시 저장 실패: {}", apiId, e);
            return false;
        }
    }
    
    /**
     * 캐시에서 API 상태 정보를 조회합니다.
     * 
     * @param apiId API 식별자
     * @return API 상태 정보 (캐시에 없으면 null)
     */
    public ApiStatusCacheDto getApiStatus(String apiId) {
        try {
            String cacheKey = CacheKeyDto.CacheKeyType.API_STATUS.buildKey(apiId);
            Object cached = valueOps.get(cacheKey);
            
            if (cached instanceof ApiStatusCacheDto) {
                return (ApiStatusCacheDto) cached;
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("API 상태 정보 캐시 조회 실패: {}", apiId, e);
            return null;
        }
    }
    
    /**
     * 여러 API의 상태 정보를 일괄로 캐시에 저장합니다.
     * 
     * @param apiStatusMap API ID와 상태 정보의 맵
     * @return 성공적으로 캐시된 API 수
     */
    public int cacheApiStatusBatch(Map<String, ApiStatusCacheDto> apiStatusMap) {
        int successCount = 0;
        
        for (Map.Entry<String, ApiStatusCacheDto> entry : apiStatusMap.entrySet()) {
            if (cacheApiStatus(entry.getKey(), entry.getValue())) {
                successCount++;
            }
        }
        
        log.info("API 상태 정보 일괄 캐시 완료: {}/{} 성공", successCount, apiStatusMap.size());
        return successCount;
    }
    
    // ==================== API 사용량 통계 캐싱 ====================
    
    /**
     * API 사용량 통계를 캐시에 저장합니다.
     * 
     * @param apiId API 식별자
     * @param usageStats 사용량 통계 데이터
     * @return 캐시 저장 성공 여부
     */
    public boolean cacheApiUsageStats(String apiId, Map<String, Object> usageStats) {
        try {
            CacheKeyDto cacheKey = CacheKeyDto.create(
                CacheKeyDto.CacheKeyType.API_USAGE_STATS, 
                apiId, 
                API_USAGE_STATS_TTL_SECONDS
            );
            
            valueOps.set(cacheKey.getKey(), usageStats, API_USAGE_STATS_TTL_SECONDS, TimeUnit.SECONDS);
            
            log.debug("API 사용량 통계 캐시 저장 완료: {} (TTL: {}초)", apiId, API_USAGE_STATS_TTL_SECONDS);
            return true;
            
        } catch (Exception e) {
            log.error("API 사용량 통계 캐시 저장 실패: {}", apiId, e);
            return false;
        }
    }
    
    /**
     * 캐시에서 API 사용량 통계를 조회합니다.
     * 
     * @param apiId API 식별자
     * @return 사용량 통계 데이터 (캐시에 없으면 null)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getApiUsageStats(String apiId) {
        try {
            String cacheKey = CacheKeyDto.CacheKeyType.API_USAGE_STATS.buildKey(apiId);
            Object cached = valueOps.get(cacheKey);
            
            if (cached instanceof Map) {
                return (Map<String, Object>) cached;
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("API 사용량 통계 캐시 조회 실패: {}", apiId, e);
            return null;
        }
    }
    
    // ==================== 캐시 관리 및 모니터링 ====================
    
    /**
     * 특정 API의 캐시를 무효화합니다.
     * 
     * @param apiId API 식별자
     * @return 무효화 성공 여부
     */
    public boolean invalidateApiCache(String apiId) {
        try {
            // API 상태 정보 캐시 무효화
            String statusKey = CacheKeyDto.CacheKeyType.API_STATUS.buildKey(apiId);
            valueOps.getAndDelete(statusKey);
            
            // API 사용량 통계 캐시 무효화
            String usageKey = CacheKeyDto.CacheKeyType.API_USAGE_STATS.buildKey(apiId);
            valueOps.getAndDelete(usageKey);
            
            log.info("API 캐시 무효화 완료: {}", apiId);
            return true;
            
        } catch (Exception e) {
            log.error("API 캐시 무효화 실패: {}", apiId, e);
            return false;
        }
    }
    
    /**
     * 패턴에 맞는 캐시를 무효화합니다.
     * 
     * @param pattern 캐시 키 패턴 (예: "api:status:*")
     * @return 무효화된 캐시 수
     */
    public int invalidateCacheByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("패턴 기반 캐시 무효화 완료: {} ({}개)", pattern, keys.size());
                return keys.size();
            }
            return 0;
            
        } catch (Exception e) {
            log.error("패턴 기반 캐시 무효화 실패: {}", pattern, e);
            return 0;
        }
    }
    
    /**
     * 캐시 통계 정보를 조회합니다.
     * 
     * @return 캐시 통계 정보
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 전체 키 개수
            Set<String> allKeys = redisTemplate.keys("*");
            stats.put("totalKeys", allKeys != null ? allKeys.size() : 0);
            
            // API 상태 정보 캐시 개수
            Set<String> statusKeys = redisTemplate.keys("api:status:*");
            stats.put("apiStatusKeys", statusKeys != null ? statusKeys.size() : 0);
            
            // API 사용량 통계 캐시 개수
            Set<String> usageKeys = redisTemplate.keys("api:usage:*");
            stats.put("apiUsageKeys", usageKeys != null ? usageKeys.size() : 0);
            
            // 메모리 사용량 (Redis INFO 명령어로 조회 가능)
            stats.put("memoryUsage", "N/A"); // Redis INFO 명령어 필요
            
            log.debug("캐시 통계 조회 완료: {}", stats);
            
        } catch (Exception e) {
            log.error("캐시 통계 조회 실패", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 만료된 캐시를 정리합니다.
     * 
     * @return 정리된 캐시 수
     */
    public int cleanupExpiredCache() {
        try {
            // Redis는 자동으로 만료된 키를 정리하므로
            // 여기서는 수동 정리 로직을 구현할 수 있습니다
            log.info("만료된 캐시 정리 완료 (Redis 자동 정리)");
            return 0;
            
        } catch (Exception e) {
            log.error("만료된 캐시 정리 실패", e);
            return 0;
        }
    }
    
    /**
     * 캐시 TTL을 연장합니다.
     * 
     * @param apiId API 식별자
     * @param additionalSeconds 추가할 초
     * @return 연장 성공 여부
     */
    public boolean extendCacheTTL(String apiId, long additionalSeconds) {
        try {
            // API 상태 정보 캐시 TTL 연장
            String statusKey = CacheKeyDto.CacheKeyType.API_STATUS.buildKey(apiId);
            Long currentTtl = redisTemplate.getExpire(statusKey);
            
            if (currentTtl != null && currentTtl > 0) {
                redisTemplate.expire(statusKey, currentTtl + additionalSeconds, TimeUnit.SECONDS);
                log.debug("API 상태 정보 캐시 TTL 연장: {} (+{}초)", apiId, additionalSeconds);
            }
            
            // API 사용량 통계 캐시 TTL 연장
            String usageKey = CacheKeyDto.CacheKeyType.API_USAGE_STATS.buildKey(apiId);
            currentTtl = redisTemplate.getExpire(usageKey);
            
            if (currentTtl != null && currentTtl > 0) {
                redisTemplate.expire(usageKey, currentTtl + additionalSeconds, TimeUnit.SECONDS);
                log.debug("API 사용량 통계 캐시 TTL 연장: {} (+{}초)", apiId, additionalSeconds);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("캐시 TTL 연장 실패: {}", apiId, e);
            return false;
        }
    }
    
    // ==================== API 키 캐싱 ====================
    
    /**
     * API 키 정보를 캐시에 저장합니다.
     * 
     * ⚠️  중요: 자주 요청되는 API 키 정보를 Redis에 캐싱하여 성능을 향상시킵니다!
     * 
     * @param keyId API 키 ID
     * @param apiKey API 키 정보
     * @param ttlSeconds 캐시 만료 시간 (초)
     * @return 캐시 저장 성공 여부
     */
    public boolean cacheApiKey(String keyId, Object apiKey, int ttlSeconds) {
        try {
            String cacheKey = "api-key:" + keyId;
            valueOps.set(cacheKey, apiKey, ttlSeconds, TimeUnit.SECONDS);
            
            log.debug("API 키 캐시 저장 완료: {} (TTL: {}초)", keyId, ttlSeconds);
            return true;
            
        } catch (Exception e) {
            log.error("API 키 캐시 저장 실패: {}", keyId, e);
            return false;
        }
    }
    
    /**
     * 캐시에서 API 키 정보를 조회합니다.
     * 
     * @param keyId API 키 ID
     * @return API 키 정보 (캐시에 없으면 null)
     */
    public Object getApiKey(String keyId) {
        try {
            String cacheKey = "api-key:" + keyId;
            Object cached = valueOps.get(cacheKey);
            
            if (cached != null) {
                log.debug("API 키 캐시 히트: {}", keyId);
            } else {
                log.debug("API 키 캐시 미스: {}", keyId);
            }
            
            return cached;
            
        } catch (Exception e) {
            log.error("API 키 캐시 조회 실패: {}", keyId, e);
            return null;
        }
    }
    
    /**
     * API 키 목록을 캐시에 저장합니다.
     * 
     * @param cacheKey 캐시 키 (예: "api-keys:page:0:size:10")
     * @param apiKeyList API 키 목록
     * @param ttlSeconds 캐시 만료 시간 (초)
     * @return 캐시 저장 성공 여부
     */
    public boolean cacheApiKeyList(String cacheKey, Object apiKeyList, int ttlSeconds) {
        try {
            valueOps.set(cacheKey, apiKeyList, ttlSeconds, TimeUnit.SECONDS);
            
            log.debug("API 키 목록 캐시 저장 완료: {} (TTL: {}초)", cacheKey, ttlSeconds);
            return true;
            
        } catch (Exception e) {
            log.error("API 키 목록 캐시 저장 실패: {}", cacheKey, e);
            return false;
        }
    }
    
    /**
     * 캐시에서 API 키 목록을 조회합니다.
     * 
     * @param cacheKey 캐시 키
     * @return API 키 목록 (캐시에 없으면 null)
     */
    public Object getApiKeyList(String cacheKey) {
        try {
            Object cached = valueOps.get(cacheKey);
            
            if (cached != null) {
                log.debug("API 키 목록 캐시 히트: {}", cacheKey);
            } else {
                log.debug("API 키 목록 캐시 미스: {}", cacheKey);
            }
            
            return cached;
            
        } catch (Exception e) {
            log.error("API 키 목록 캐시 조회 실패: {}", cacheKey, e);
            return null;
        }
    }
    
    /**
     * 특정 API 키의 캐시를 무효화합니다.
     * 
     * @param keyId API 키 ID
     * @return 무효화 성공 여부
     */
    public boolean invalidateApiKeyCache(String keyId) {
        try {
            String cacheKey = "api-key:" + keyId;
            valueOps.getAndDelete(cacheKey);
            
            log.info("API 키 캐시 무효화 완료: {}", keyId);
            return true;
            
        } catch (Exception e) {
            log.error("API 키 캐시 무효화 실패: {}", keyId, e);
            return false;
        }
    }
    
    /**
     * API 키 목록 관련 캐시를 모두 무효화합니다.
     * 
     * @return 무효화된 캐시 수
     */
    public int invalidateApiKeyListCache() {
        try {
            Set<String> keys = redisTemplate.keys("api-keys:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("API 키 목록 캐시 무효화 완료: {}개", keys.size());
                return keys.size();
            }
            return 0;
            
        } catch (Exception e) {
            log.error("API 키 목록 캐시 무효화 실패", e);
            return 0;
        }
    }
    
    /**
     * API 키 관련 모든 캐시를 무효화합니다.
     * 
     * @return 무효화된 캐시 수
     */
    public int invalidateAllApiKeyCache() {
        try {
            Set<String> keys = redisTemplate.keys("api-key:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("API 키 전체 캐시 무효화 완료: {}개", keys.size());
                return keys.size();
            }
            return 0;
            
        } catch (Exception e) {
            log.error("API 키 전체 캐시 무효화 실패", e);
            return 0;
        }
    }
}
