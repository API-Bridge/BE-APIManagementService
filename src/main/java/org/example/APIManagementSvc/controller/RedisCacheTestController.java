package org.example.APIManagementSvc.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.annotation.RateLimit;
import org.example.APIManagementSvc.dto.cache.ApiStatusCacheDto;
import org.example.APIManagementSvc.dto.cache.CacheKeyDto;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 캐싱 기능을 테스트하기 위한 컨트롤러입니다.
 * 
 * ⚠️  중요: 이 컨트롤러는 Redis 캐싱 시스템의 동작을 테스트합니다!
 * - API 상태 정보 캐싱 테스트
 * - API 사용량 통계 캐싱 테스트
 * - 캐시 관리 및 모니터링 테스트
 * 
 * 테스트 시나리오:
 * 1. API 상태 정보 캐싱 및 조회
 * 2. API 사용량 통계 캐싱 및 조회
 * 3. 캐시 무효화 및 통계 조회
 * 4. 캐시 TTL 연장 및 정리
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/redis-cache-test")
public class RedisCacheTestController {
    
    @Autowired
    private RedisCacheService redisCacheService;
    
    // ==================== API 상태 정보 캐싱 테스트 ====================
    
    /**
     * API 상태 정보를 캐시에 저장합니다.
     * 
     * @param apiId API 식별자
     * @return API 응답
     */
    @PostMapping("/cache-api-status/{apiId}")
    @RateLimit(value = 10, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ApiResponse<String> cacheApiStatus(@PathVariable String apiId) {
        try {
            // 테스트용 API 상태 정보 생성
            ApiStatusCacheDto apiStatus = ApiStatusCacheDto.builder()
                    .apiId(apiId)
                    .apiName("테스트 API - " + apiId)
                    .domain(org.example.APIManagementSvc.domain.enums.ApiDomain.GOVERNMENT)
                    .keyword(org.example.APIManagementSvc.domain.enums.ApiKeyword.STATISTICS)
                    .status("ACTIVE")
                    .dailyLimit(5000L)
                    .monthlyLimit(100000L)
                    .currentDailyUsage(1250L)
                    .currentMonthlyUsage(35000L)
                    .lastUsedAt(LocalDateTime.now())
                    .provider("SGIS")
                    .version("1.0.0")
                    .documentationUrl("https://sgis.kr/api/docs/" + apiId)
                    .supportContact("support@sgis.kr")
                    .lastUpdated(LocalDateTime.now())
                    .ttlSeconds(1800L) // 30분
                    .build();
            
            boolean success = redisCacheService.cacheApiStatus(apiId, apiStatus);
            
            if (success) {
                log.info("API 상태 정보 캐시 저장 성공: {}", apiId);
                return ApiResponse.<String>builder()
                        .success(true)
                        .message("API 상태 정보가 성공적으로 캐시에 저장되었습니다")
                        .data("API ID: " + apiId + " - 상태 정보 캐시 저장 완료")
                        .build();
            } else {
                log.error("API 상태 정보 캐시 저장 실패: {}", apiId);
                return ApiResponse.<String>builder()
                        .success(false)
                        .message("API 상태 정보 캐시 저장에 실패했습니다")
                        .data("API ID: " + apiId + " - 상태 정보 캐시 저장 실패")
                        .build();
            }
            
        } catch (Exception e) {
            log.error("API 상태 정보 캐시 저장 중 오류 발생: {}", apiId, e);
            return ApiResponse.<String>builder()
                    .success(false)
                    .message("API 상태 정보 캐시 저장 중 오류가 발생했습니다: " + e.getMessage())
                    .data("API ID: " + apiId + " - 오류 발생")
                    .build();
        }
    }
    
    /**
     * 캐시에서 API 상태 정보를 조회합니다.
     * 
     * @param apiId API 식별자
     * @return API 응답
     */
    @GetMapping("/get-api-status/{apiId}")
    @RateLimit(value = 20, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ApiResponse<Object> getApiStatus(@PathVariable String apiId) {
        try {
            ApiStatusCacheDto apiStatus = redisCacheService.getApiStatus(apiId);
            
            if (apiStatus != null) {
                log.info("API 상태 정보 캐시 조회 성공: {}", apiId);
                return ApiResponse.builder()
                        .success(true)
                        .message("API 상태 정보를 캐시에서 조회했습니다")
                        .data(apiStatus)
                        .build();
            } else {
                log.info("API 상태 정보 캐시 미스: {}", apiId);
                return ApiResponse.builder()
                        .success(false)
                        .message("캐시에서 API 상태 정보를 찾을 수 없습니다")
                        .data("API ID: " + apiId + " - 캐시 미스")
                        .build();
            }
            
        } catch (Exception e) {
            log.error("API 상태 정보 캐시 조회 중 오류 발생: {}", apiId, e);
            return ApiResponse.builder()
                    .success(false)
                    .message("API 상태 정보 캐시 조회 중 오류가 발생했습니다: " + e.getMessage())
                    .data("API ID: " + apiId + " - 오류 발생")
                    .build();
        }
    }
    
    // ==================== API 사용량 통계 캐싱 테스트 ====================
    
    /**
     * API 사용량 통계를 캐시에 저장합니다.
     * 
     * @param apiId API 식별자
     * @return API 응답
     */
    @PostMapping("/cache-api-usage-stats/{apiId}")
    @RateLimit(value = 10, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ApiResponse<String> cacheApiUsageStats(@PathVariable String apiId) {
        try {
            // 테스트용 사용량 통계 데이터 생성
            Map<String, Object> usageStats = new HashMap<>();
            usageStats.put("apiId", apiId);
            usageStats.put("timestamp", LocalDateTime.now().toString());
            usageStats.put("dailyRequests", 1250L);
            usageStats.put("monthlyRequests", 35000L);
            usageStats.put("dailySuccess", 1240L);
            usageStats.put("monthlySuccess", 34800L);
            usageStats.put("dailyFailures", 10L);
            usageStats.put("monthlyFailures", 200L);
            usageStats.put("peakHour", "14:00");
            usageStats.put("popularUsers", new String[]{"org1", "org2", "org3"});
            usageStats.put("averageResponseTime", 150L);
            usageStats.put("lastUpdated", LocalDateTime.now().toString());
            
            boolean success = redisCacheService.cacheApiUsageStats(apiId, usageStats);
            
            if (success) {
                log.info("API 사용량 통계 캐시 저장 성공: {}", apiId);
                return ApiResponse.<String>builder()
                        .success(true)
                        .message("API 사용량 통계가 성공적으로 캐시에 저장되었습니다")
                        .data("API ID: " + apiId + " - 사용량 통계 캐시 저장 완료")
                        .build();
            } else {
                log.error("API 사용량 통계 캐시 저장 실패: {}", apiId);
                return ApiResponse.<String>builder()
                        .success(false)
                        .message("API 사용량 통계 캐시 저장에 실패했습니다")
                        .data("API ID: " + apiId + " - 사용량 통계 캐시 저장 실패")
                        .build();
            }
            
        } catch (Exception e) {
            log.error("API 사용량 통계 캐시 저장 중 오류 발생: {}", apiId, e);
            return ApiResponse.<String>builder()
                    .success(false)
                    .message("API 사용량 통계 캐시 저장 중 오류가 발생했습니다: " + e.getMessage())
                    .data("API ID: " + apiId + " - 오류 발생")
                    .build();
        }
    }
    
    /**
     * 캐시에서 API 사용량 통계를 조회합니다.
     * 
     * @param apiId API 식별자
     * @return API 응답
     */
    @GetMapping("/get-api-usage-stats/{apiId}")
    @RateLimit(value = 20, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ApiResponse<Object> getApiUsageStats(@PathVariable String apiId) {
        try {
            Map<String, Object> usageStats = redisCacheService.getApiUsageStats(apiId);
            
            if (usageStats != null) {
                log.info("API 사용량 통계 캐시 조회 성공: {}", apiId);
                return ApiResponse.builder()
                        .success(true)
                        .message("API 사용량 통계를 캐시에서 조회했습니다")
                        .data(usageStats)
                        .build();
            } else {
                log.info("API 사용량 통계 캐시 미스: {}", apiId);
                return ApiResponse.builder()
                        .success(false)
                        .message("캐시에서 API 사용량 통계를 찾을 수 없습니다")
                        .data("API ID: " + apiId + " - 캐시 미스")
                        .build();
            }
            
        } catch (Exception e) {
            log.error("API 사용량 통계 캐시 조회 중 오류 발생: {}", apiId, e);
            return ApiResponse.builder()
                    .success(false)
                    .message("API 사용량 통계 캐시 조회 중 오류가 발생했습니다: " + e.getMessage())
                    .data("API ID: " + apiId + " - 오류 발생")
                    .build();
        }
    }
    
    // ==================== 캐시 관리 및 모니터링 테스트 ====================
    
    /**
     * 특정 API의 캐시를 무효화합니다.
     * 
     * @param apiId API 식별자
     * @return API 응답
     */
    @DeleteMapping("/invalidate-api-cache/{apiId}")
    @RateLimit(value = 5, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ApiResponse<String> invalidateApiCache(@PathVariable String apiId) {
        try {
            boolean success = redisCacheService.invalidateApiCache(apiId);
            
            if (success) {
                log.info("API 캐시 무효화 성공: {}", apiId);
                return ApiResponse.<String>builder()
                        .success(true)
                        .message("API 캐시가 성공적으로 무효화되었습니다")
                        .data("API ID: " + apiId + " - 캐시 무효화 완료")
                        .build();
            } else {
                log.error("API 캐시 무효화 실패: {}", apiId);
                return ApiResponse.<String>builder()
                        .success(false)
                        .message("API 캐시 무효화에 실패했습니다")
                        .data("API ID: " + apiId + " - 캐시 무효화 실패")
                        .build();
            }
            
        } catch (Exception e) {
            log.error("API 캐시 무효화 중 오류 발생: {}", apiId, e);
            return ApiResponse.<String>builder()
                    .success(false)
                    .message("API 캐시 무효화 중 오류가 발생했습니다: " + e.getMessage())
                    .data("API ID: " + apiId + " - 오류 발생")
                    .build();
        }
    }
    
    /**
     * 캐시 통계 정보를 조회합니다.
     * 
     * @return API 응답
     */
    @GetMapping("/cache-statistics")
    @RateLimit(value = 30, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ApiResponse<Object> getCacheStatistics() {
        try {
            Map<String, Object> stats = redisCacheService.getCacheStatistics();
            
            log.info("캐시 통계 조회 성공");
            return ApiResponse.builder()
                    .success(true)
                    .message("캐시 통계 정보를 조회했습니다")
                    .data(stats)
                    .build();
            
        } catch (Exception e) {
            log.error("캐시 통계 조회 중 오류 발생", e);
            return ApiResponse.builder()
                    .success(false)
                    .message("캐시 통계 조회 중 오류가 발생했습니다: " + e.getMessage())
                    .data("오류 발생")
                    .build();
        }
    }
    
    /**
     * 만료된 캐시를 정리합니다.
     * 
     * @return API 응답
     */
    @PostMapping("/cleanup-expired-cache")
    @RateLimit(value = 10, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ApiResponse<String> cleanupExpiredCache() {
        try {
            int cleanedCount = redisCacheService.cleanupExpiredCache();
            
            log.info("만료된 캐시 정리 완료: {}개", cleanedCount);
            return ApiResponse.<String>builder()
                    .success(true)
                    .message("만료된 캐시 정리가 완료되었습니다")
                    .data("정리된 캐시: " + cleanedCount + "개")
                    .build();
            
        } catch (Exception e) {
            log.error("만료된 캐시 정리 중 오류 발생", e);
            return ApiResponse.<String>builder()
                    .success(false)
                    .message("만료된 캐시 정리 중 오류가 발생했습니다: " + e.getMessage())
                    .data("오류 발생")
                    .build();
        }
    }
    
    /**
     * 캐시 TTL을 연장합니다.
     * 
     * @param apiId API 식별자
     * @param additionalSeconds 추가할 초
     * @return API 응답
     */
    @PostMapping("/extend-cache-ttl/{apiId}")
    @RateLimit(value = 10, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ApiResponse<String> extendCacheTTL(
            @PathVariable String apiId,
            @RequestParam(defaultValue = "3600") long additionalSeconds) {
        try {
            boolean success = redisCacheService.extendCacheTTL(apiId, additionalSeconds);
            
            if (success) {
                log.info("캐시 TTL 연장 성공: {} (+{}초)", apiId, additionalSeconds);
                return ApiResponse.<String>builder()
                        .success(true)
                        .message("캐시 TTL이 성공적으로 연장되었습니다")
                        .data("API ID: " + apiId + " - TTL 연장 완료 (+" + additionalSeconds + "초)")
                        .build();
            } else {
                log.error("캐시 TTL 연장 실패: {}", apiId);
                return ApiResponse.<String>builder()
                        .success(false)
                        .message("캐시 TTL 연장에 실패했습니다")
                        .data("API ID: " + apiId + " - TTL 연장 실패")
                        .build();
            }
            
        } catch (Exception e) {
            log.error("캐시 TTL 연장 중 오류 발생: {}", apiId, e);
            return ApiResponse.<String>builder()
                    .success(false)
                    .message("캐시 TTL 연장 중 오류가 발생했습니다: " + e.getMessage())
                    .data("API ID: " + apiId + " - 오류 발생")
                    .build();
        }
    }
}
