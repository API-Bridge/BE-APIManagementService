package org.example.APIManagementSvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.service.RedisCacheService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐싱 기능을 테스트하기 위한 컨트롤러입니다.
 * 
 * ⚠️  중요: 이 컨트롤러는 Redis 캐싱 시스템의 동작을 테스트합니다!
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/redis-cache-test")
@RequiredArgsConstructor
public class RedisCacheTestController {
    
    private final RedisCacheService redisCacheService;
    
    /**
     * 간단한 테스트 엔드포인트
     */
    @GetMapping("/test")
    public ApiResponse<String> test() {
        log.info("Redis 캐시 테스트 컨트롤러 테스트 엔드포인트 호출");
        return ApiResponse.success("Redis 캐시 테스트 컨트롤러가 정상적으로 등록되었습니다!");
    }
    
    /**
     * 상태 확인 엔드포인트
     */
    @GetMapping("/status")
    public ApiResponse<String> status() {
        log.info("Redis 캐시 테스트 컨트롤러 상태 확인");
        return ApiResponse.success("Redis 캐시 테스트 컨트롤러 정상 동작 중");
    }
    
    /**
     * 캐시 저장 테스트
     */
    @PostMapping("/cache/{key}")
    public ApiResponse<String> setCache(@PathVariable String key, @RequestBody Map<String, Object> value) {
        log.info("캐시 저장 테스트: key={}, value={}", key, value);
        
        // API 키 캐싱 메소드를 이용해 테스트 데이터 저장 (TTL: 5분)
        boolean success = redisCacheService.cacheApiKey("test:" + key, value, 300);
        
        if (success) {
            return ApiResponse.success("캐시 저장 성공: " + key);
        } else {
            return ApiResponse.error("캐시 저장 실패: " + key);
        }
    }
    
    /**
     * 캐시 조회 테스트
     */
    @GetMapping("/cache/{key}")
    public ApiResponse<Object> getCache(@PathVariable String key) {
        log.info("캐시 조회 테스트: key={}", key);
        
        Object cached = redisCacheService.getApiKey("test:" + key);
        
        if (cached != null) {
            return ApiResponse.success(cached, "캐시 조회 성공 (Cache Hit)");
        } else {
            return ApiResponse.success(null, "캐시 조회 실패 (Cache Miss)");
        }
    }
    
    /**
     * 캐시 삭제 테스트
     */
    @DeleteMapping("/cache/{key}")
    public ApiResponse<String> deleteCache(@PathVariable String key) {
        log.info("캐시 삭제 테스트: key={}", key);
        
        boolean success = redisCacheService.invalidateApiKeyCache("test:" + key);
        
        if (success) {
            return ApiResponse.success("캐시 삭제 성공: " + key);
        } else {
            return ApiResponse.error("캐시 삭제 실패: " + key);
        }
    }
    
    /**
     * 캐시 통계 조회
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getCacheStats() {
        log.info("캐시 통계 조회");
        
        Map<String, Object> stats = redisCacheService.getCacheStatistics();
        return ApiResponse.success(stats, "캐시 통계 조회 성공");
    }
    
    /**
     * 성능 테스트 - 여러 값 저장
     */
    @PostMapping("/performance-test/{count}")
    public ApiResponse<Map<String, Object>> performanceTest(@PathVariable int count) {
        log.info("성능 테스트 시작: {}개 데이터 저장/조회", count);
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        
        // 저장 테스트
        for (int i = 0; i < count; i++) {
            Map<String, Object> testData = new HashMap<>();
            testData.put("id", i);
            testData.put("name", "test-" + i);
            testData.put("timestamp", System.currentTimeMillis());
            
            if (redisCacheService.cacheApiKey("perf-test:" + i, testData, 600)) {
                successCount++;
            }
        }
        
        long writeTime = System.currentTimeMillis() - startTime;
        
        // 조회 테스트
        int hitCount = 0;
        long readStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < count; i++) {
            Object cached = redisCacheService.getApiKey("perf-test:" + i);
            if (cached != null) {
                hitCount++;
            }
        }
        
        long readTime = System.currentTimeMillis() - readStartTime;
        long totalTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", count);
        result.put("writeSuccessCount", successCount);
        result.put("readHitCount", hitCount);
        result.put("writeTimeMs", writeTime);
        result.put("readTimeMs", readTime);
        result.put("totalTimeMs", totalTime);
        result.put("writeAvgMs", (double) writeTime / count);
        result.put("readAvgMs", (double) readTime / count);
        
        log.info("성능 테스트 완료: {} 저장, {} 조회, 총 {}ms", successCount, hitCount, totalTime);
        
        return ApiResponse.success(result, "성능 테스트 완료");
    }
    
    /**
     * 모든 테스트 캐시 삭제
     */
    @DeleteMapping("/cleanup")
    public ApiResponse<Map<String, Object>> cleanup() {
        log.info("테스트 캐시 정리 시작");
        
        // 테스트 관련 캐시 삭제
        int testCacheCount = redisCacheService.invalidateCacheByPattern("api-key:test:*");
        int perfCacheCount = redisCacheService.invalidateCacheByPattern("api-key:perf-test:*");
        
        Map<String, Object> result = new HashMap<>();
        result.put("testCacheDeleted", testCacheCount);
        result.put("perfCacheDeleted", perfCacheCount);
        result.put("totalDeleted", testCacheCount + perfCacheCount);
        
        log.info("테스트 캐시 정리 완료: test={}, perf={}", testCacheCount, perfCacheCount);
        
        return ApiResponse.success(result, "테스트 캐시 정리 완료");
    }
}
