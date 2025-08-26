package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ExternalApiSpec;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * API 헬스체크 결과 Redis 캐시 관리 서비스
 * 
 * 헬스체크에 실패한 외부 API 정보를 Redis에 캐시하여 관리하는 서비스
 * 
 * 주요 기능:
 * - 헬스체크 실패 API 정보를 Redis에 저장 (TTL: 10분)
 * - 실패 API 목록 조회 및 관리
 * - 캐시된 실패 API 정보 삭제 (정상화 시)
 * - 실패 API 상태 모니터링 지원
 * 
 * Redis 키 구조:
 * - unhealthy_api:{apiId} : API 상세 정보
 * - unhealthy_apis_set : 실패 API ID 집합
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiHealthCacheService {

    /** Redis 데이터 접근을 위한 RedisTemplate */
    private final RedisTemplate<String, Object> redisTemplate;
    
    /** 실패 API 캐시 키 접두사 */
    private static final String UNHEALTHY_API_KEY_PREFIX = "unhealthy_api:";
    
    /** 실패 API 집합 키 */
    private static final String UNHEALTHY_APIS_SET_KEY = "unhealthy_apis_set";
    
    /** 캐시 TTL: 10분 (600초) */
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    /**
     * 헬스체크 실패 API를 Redis 캐시에 저장
     * 
     * API 상세 정보와 실패 시각을 포함하여 Redis에 저장하고
     * TTL을 10분으로 설정하여 자동 만료 처리
     * 
     * 저장 데이터:
     * - API ID, 이름, URL, 설명
     * - 실패 시각 정보
     * - 마지막 헬스체크 시간
     * 
     * @param apiSpec 헬스체크에 실패한 API 명세
     */
    public void cacheUnhealthyApi(ExternalApiSpec apiSpec) {
        try {
            String apiKey = UNHEALTHY_API_KEY_PREFIX + apiSpec.getApiId();
            
            // API 정보를 캐시용 객체로 변환
            UnhealthyApiInfo unhealthyInfo = UnhealthyApiInfo.builder()
                    .apiId(apiSpec.getApiId())
                    .apiName(apiSpec.getApiName())
                    .apiUrl(apiSpec.getApiUrl())
                    .apiDescription(apiSpec.getApiDescription())
                    .apiIssuer(apiSpec.getApiIssuer())
                    .failedAt(LocalDateTime.now())
                    .lastHealthCheck(apiSpec.getLastHealthCheck())
                    .build();

            // Redis에 API 정보 저장 (TTL: 10분)
            redisTemplate.opsForValue().set(apiKey, unhealthyInfo, CACHE_TTL);
            
            // 실패 API ID를 집합에 추가 (TTL: 10분)
            redisTemplate.opsForSet().add(UNHEALTHY_APIS_SET_KEY, apiSpec.getApiId());
            redisTemplate.expire(UNHEALTHY_APIS_SET_KEY, CACHE_TTL);

            log.info("Cached unhealthy API: {} with TTL: {} minutes", 
                    apiSpec.getApiName(), CACHE_TTL.toMinutes());
                    
        } catch (Exception e) {
            log.error("Failed to cache unhealthy API: {}", apiSpec.getApiName(), e);
        }
    }

    /**
     * API가 정상화되었을 때 캐시에서 제거
     * 
     * 헬스체크가 성공으로 변경될 때 Redis에서 해당 API 정보를 삭제
     * 
     * @param apiId 정상화된 API ID
     */
    public void removeHealthyApi(String apiId) {
        try {
            String apiKey = UNHEALTHY_API_KEY_PREFIX + apiId;
            
            // API 정보 삭제
            redisTemplate.delete(apiKey);
            
            // 실패 API 집합에서 제거
            redisTemplate.opsForSet().remove(UNHEALTHY_APIS_SET_KEY, apiId);
            
            log.info("Removed healthy API from cache: {}", apiId);
            
        } catch (Exception e) {
            log.error("Failed to remove healthy API from cache: {}", apiId, e);
        }
    }

    /**
     * 현재 캐시된 모든 실패 API ID 목록 조회
     * 
     * Redis에 저장된 모든 실패 API의 ID 집합을 반환
     * 
     * @return Set<String> 실패 API ID 집합
     */
    public Set<String> getUnhealthyApiIds() {
        try {
            Set<Object> apiIds = redisTemplate.opsForSet().members(UNHEALTHY_APIS_SET_KEY);
            return apiIds != null ? 
                   apiIds.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet()) :
                   java.util.Collections.emptySet();
        } catch (Exception e) {
            log.error("Failed to get unhealthy API IDs from cache", e);
            return java.util.Collections.emptySet();
        }
    }

    /**
     * 특정 API의 실패 정보 조회
     * 
     * Redis에서 특정 API의 상세한 실패 정보를 조회
     * 
     * @param apiId 조회할 API ID
     * @return UnhealthyApiInfo 실패 API 정보 (없으면 null)
     */
    public UnhealthyApiInfo getUnhealthyApiInfo(String apiId) {
        try {
            String apiKey = UNHEALTHY_API_KEY_PREFIX + apiId;
            return (UnhealthyApiInfo) redisTemplate.opsForValue().get(apiKey);
        } catch (Exception e) {
            log.error("Failed to get unhealthy API info for: {}", apiId, e);
            return null;
        }
    }

    /**
     * 실패 API 캐시 정보를 담는 내부 클래스
     * 
     * Redis에 저장될 실패 API의 상세 정보를 담는 데이터 클래스
     */
    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UnhealthyApiInfo implements java.io.Serializable {
        /** API 고유 식별자 */
        private String apiId;
        
        /** API 이름 */
        private String apiName;
        
        /** API URL */
        private String apiUrl;
        
        /** API 설명 */
        private String apiDescription;
        
        /** API 발행자 */
        private String apiIssuer;
        
        /** 실패 시각 */
        private LocalDateTime failedAt;
        
        /** 마지막 헬스체크 시간 */
        private LocalDateTime lastHealthCheck;

        /**
         * 실패 시각을 포맷된 문자열로 반환
         * @return String 포맷된 실패 시각
         */
        public String getFormattedFailedAt() {
            return failedAt != null ? 
                   failedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : 
                   "알 수 없음";
        }
    }
}