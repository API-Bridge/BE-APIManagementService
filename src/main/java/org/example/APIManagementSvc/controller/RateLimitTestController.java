package org.example.APIManagementSvc.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.annotation.RateLimit;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.service.RateLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting 기능을 테스트하기 위한 컨트롤러입니다.
 * 
 * 이 컨트롤러는 다양한 Rate Limiting 설정을 테스트할 수 있도록
 * 여러 엔드포인트를 제공합니다.
 * 
 * ⚠️  중요: 각 엔드포인트에 구체적인 한도(횟수)를 정확하게 설정했습니다!
 * - IP 기반: 1분에 최대 5회 (정확히 5회까지만 허용)
 * - API 키 기반: 1시간에 최대 100회 (정확히 100회까지만 허용)
 * - 사용자 기반: 1일에 최대 1000회 (정확히 1000회까지만 허용)
 * - 세션 기반: 1분에 최대 3회 (정확히 3회까지만 허용)
 * 
 * 테스트 시나리오:
 * - IP 기반 제한 테스트
 * - API 키 기반 제한 테스트
 * - 시간 단위별 제한 테스트
 * - Rate Limiting 상태 조회
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rate-limit-test")
public class RateLimitTestController {
    
    @Autowired
    private RateLimitService rateLimitService;
    
    /**
     * IP 기반 Rate Limiting 테스트 (1분에 최대 5회)
     * 
     * ⚠️  중요: 구체적인 한도 설정!
     * - value = 5: 정확히 5회까지만 허용
     * - timeUnit = MINUTES: 1분 단위로 카운터 초기화
     * - 6번째 요청부터는 429 에러 반환
     * 
     * @return API 응답
     */
    @GetMapping("/ip-based")
    @RateLimit(value = 5, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ApiResponse<String> testIpBasedRateLimit() {
        log.info("IP 기반 Rate Limiting 테스트 호출됨");
        return ApiResponse.<String>builder()
                .success(true)
                .message("IP 기반 Rate Limiting 테스트 성공")
                .data("현재 시간: " + java.time.LocalDateTime.now())
                .build();
    }
    
    /**
     * API 키 기반 Rate Limiting 테스트 (1시간에 최대 100회)
     * 
     * @param apiKey API 키 (헤더 또는 쿼리 파라미터)
     * @return API 응답
     */
    @GetMapping("/api-key-based")
    @RateLimit(value = 100, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.API_KEY)
    public ApiResponse<String> testApiKeyBasedRateLimit(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestParam(value = "apiKey", required = false) String queryApiKey) {
        
        String key = apiKey != null ? apiKey : queryApiKey;
        log.info("API 키 기반 Rate Limiting 테스트 호출됨 - API Key: {}", key);
        
        return ApiResponse.<String>builder()
                .success(true)
                .message("API 키 기반 Rate Limiting 테스트 성공")
                .data("API Key: " + key + ", 현재 시간: " + java.time.LocalDateTime.now())
                .build();
    }
    
    /**
     * 사용자 ID 기반 Rate Limiting 테스트 (1일 최대 1000회)
     * 
     * @param userId 사용자 ID (헤더 또는 쿼리 파라미터)
     * @return API 응답
     */
    @GetMapping("/user-based")
    @RateLimit(value = 1000, timeUnit = TimeUnit.DAYS, keyType = RateLimit.KeyType.USER_ID)
    public ApiResponse<String> testUserBasedRateLimit(
            @RequestHeader(value = "X-User-ID", required = false) String userId,
            @RequestParam(value = "userId", required = false) String queryUserId) {
        
        String user = userId != null ? userId : queryUserId;
        log.info("사용자 기반 Rate Limiting 테스트 호출됨 - User ID: {}", user);
        
        return ApiResponse.<String>builder()
                .success(true)
                .message("사용자 기반 Rate Limiting 테스트 성공")
                .data("User ID: " + user + ", 현재 시간: " + java.time.LocalDateTime.now())
                .build();
    }
    
    /**
     * 세션 기반 Rate Limiting 테스트 (1분에 최대 3회)
     * 
     * @return API 응답
     */
    @GetMapping("/session-based")
    @RateLimit(value = 3, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.SESSION)
    public ApiResponse<String> testSessionBasedRateLimit() {
        log.info("세션 기반 Rate Limiting 테스트 호출됨");
        return ApiResponse.<String>builder()
                .success(true)
                .message("세션 기반 Rate Limiting 테스트 성공")
                .data("현재 시간: " + java.time.LocalDateTime.now())
                .build();
    }
    
    /**
     * Rate Limiting 상태를 조회합니다.
     * 
     * @param key Rate Limiting 키
     * @return Rate Limiting 상태 정보
     */
    @GetMapping("/status")
    public ApiResponse<RateLimitService.RateLimitStatus> getRateLimitStatus(
            @RequestParam String key) {
        
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);
        
        return ApiResponse.<RateLimitService.RateLimitStatus>builder()
                .success(true)
                .message("Rate Limiting 상태 조회 성공")
                .data(status)
                .build();
    }
    
    /**
     * Rate Limiting 캐시를 정리합니다.
     * 
     * @return API 응답
     */
    @PostMapping("/cleanup")
    public ApiResponse<String> cleanupRateLimitCache() {
        rateLimitService.cleanupCache();
        
        return ApiResponse.<String>builder()
                .success(true)
                .message("Rate Limiting 캐시 정리 완료")
                .data("캐시 정리 시간: " + java.time.LocalDateTime.now())
                .build();
    }
}
