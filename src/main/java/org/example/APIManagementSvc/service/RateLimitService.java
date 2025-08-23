package org.example.APIManagementSvc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * API Rate Limiting을 처리하는 서비스 클래스입니다.
 * 
 * 이 서비스는 메모리 기반으로 API 호출 횟수를 추적하고 제한합니다.
 * 프로덕션 환경에서는 Redis나 다른 분산 캐시를 사용하는 것을 권장합니다.
 * 
 * ⚠️  중요: 구체적인 한도(횟수)를 정확하게 제한할 수 있습니다!
 * 
 * 주요 기능:
 * - 시간 기반 호출 횟수 제한 (구체적인 횟수 설정 가능)
 * - 다양한 시간 단위 지원 (초, 분, 시간, 일)
 * - 메모리 기반 호출 이력 관리
 * - 자동 정리 및 메모리 최적화
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
public class RateLimitService {
    
    /**
     * Rate Limiting 데이터를 저장하는 메모리 캐시
     * Key: Rate Limiting 키
     * Value: 호출 이력 정보
     */
    private final Map<String, RateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();
    
    /**
     * 지정된 키에 대해 Rate Limiting을 검사합니다.
     * 
     * ⚠️  중요: limit 파라미터로 구체적인 한도(횟수)를 정확하게 설정할 수 있습니다!
     * 예: limit = 5이면 정확히 5회까지만 허용, 6번째부터는 거부
     * 
     * @param key Rate Limiting 키
     * @param limit 허용되는 최대 호출 횟수 (구체적인 숫자)
     * @param timeUnit 시간 단위
     * @return 호출 허용 여부
     */
    public boolean isAllowed(String key, int limit, TimeUnit timeUnit) {
        LocalDateTime now = LocalDateTime.now();
        RateLimitInfo info = rateLimitCache.get(key); // 선언은 여기서 한 번만

        // 0 이하 한도인 경우 특별 처리
        if (limit <= 0) {
            if (info == null) {
                // 이전에 이 키에 대한 정보가 없었다면, 새 정보를 0 카운트로 생성 (상태 조회를 위함)
                info = new RateLimitInfo(now, limit);
                rateLimitCache.put(key, info);
            } else {
                // 정보가 있다면, 시간 경계에 따라 리셋 처리 (카운트는 여전히 증가시키지 않음)
                LocalDateTime boundary = calculateBoundary(now, timeUnit);
                if (info.getStartTime().isBefore(boundary)) {
                    info.reset(now, limit);
                }
            }
            return false; // 0 이하 한도이므로 항상 거부
        }

        // 정상적인 (limit > 0) Rate Limiting 로직 시작
        // info가 null이거나, 이전에 limit <= 0으로 설정되어 있던 경우 (info.getLimit() <= 0)
        // 새로운 RateLimitInfo를 생성하거나 기존 것을 재설정
        if (info == null || info.getLimit() <= 0 || info.getStartTime().isBefore(calculateBoundary(now, timeUnit))) {
            info = new RateLimitInfo(now, limit);
            rateLimitCache.put(key, info);
            info.incrementCount(); // 새로운 주기의 첫 번째 호출 또는 첫 호출
            return true;
        }

        // 현재 카운트가 한도 미만인 경우 허용
        if (info.getCurrentCount() < limit) {
            info.incrementCount();
            return true;
        } else {
            // 한도 초과
            log.debug("Rate limit exceeded for key: {}, current: {}, limit: {}",
                    key, info.getCurrentCount(), limit);
            return false;
        }
    }
    
    /**
     * 현재 Rate Limiting 상태를 조회합니다.
     * 
     * @param key Rate Limiting 키
     * @return Rate Limiting 상태 정보
     */
    public RateLimitStatus getRateLimitStatus(String key) {
        RateLimitInfo info = rateLimitCache.get(key);
        
        if (info == null) {
            // 해당 키에 대해 isAllowed가 한 번도 호출되지 않은 경우
            return new RateLimitStatus(0, 0, 0, true);
        }
        
        // isAllowed 로직과 일관되게, 허용 여부는 현재 카운트가 limit보다 작은지 여부로 판단
        // 단, limit이 0 이하이면 항상 allowed가 false여야 함.
        boolean allowed = info.getLimit() > 0 && info.getCurrentCount() < info.getLimit();

        return new RateLimitStatus(
            info.getCurrentCount(),
            info.getLimit(),
            Math.max(0, info.getLimit() - info.getCurrentCount()), // 남은 횟수는 음수가 될 수 없음
            allowed
        );
    }
    
    /**
     * Rate Limiting 캐시를 정리합니다.
     * 오래된 데이터를 제거하여 메모리 사용량을 최적화합니다.
     */
    public void cleanupCache() {
        LocalDateTime cutoff = LocalDateTime.now().minus(1, ChronoUnit.DAYS);
        
        rateLimitCache.entrySet().removeIf(entry -> 
            entry.getValue().getStartTime().isBefore(cutoff)
        );
        
        log.debug("Rate limit cache cleaned up. Current size: {}", rateLimitCache.size());
    }
    
    /**
     * 시간 단위에 따른 경계 시간을 계산합니다.
     * 
     * @param now 현재 시간
     * @param timeUnit 시간 단위
     * @return 경계 시간
     */
    private LocalDateTime calculateBoundary(LocalDateTime now, TimeUnit timeUnit) {
        switch (timeUnit) {
            case SECONDS:
                return now.truncatedTo(ChronoUnit.SECONDS);
            case MINUTES:
                return now.truncatedTo(ChronoUnit.MINUTES);
            case HOURS:
                return now.truncatedTo(ChronoUnit.HOURS);
            case DAYS:
                return now.truncatedTo(ChronoUnit.DAYS);
            default:
                return now.truncatedTo(ChronoUnit.HOURS);
        }
    }
    
    /**
     * Rate Limiting 정보를 저장하는 내부 클래스
     */
    private static class RateLimitInfo {
        private LocalDateTime startTime;
        private int currentCount;
        private int limit;
        
        public RateLimitInfo(LocalDateTime startTime, int limit) {
            this.startTime = startTime;
            this.limit = limit;
            this.currentCount = 0; // 초기화 시 0으로 설정
        }
        
        public void reset(LocalDateTime newStartTime, int newLimit) {
            this.startTime = newStartTime;
            this.limit = newLimit;
            this.currentCount = 0; // 리셋 시 0으로 설정
        }
        
        public void incrementCount() {
            this.currentCount++;
        }
        
        public LocalDateTime getStartTime() {
            return startTime;
        }
        
        public int getCurrentCount() {
            return currentCount;
        }
        
        public int getLimit() {
            return limit;
        }
    }
    
    /**
     * Rate Limiting 상태 정보를 담는 DTO 클래스
     */
    public static class RateLimitStatus {
        private final int currentCount;
        private final int limit;
        private final int remaining;
        private final boolean allowed;
        
        public RateLimitStatus(int currentCount, int limit, int remaining, boolean allowed) {
            this.currentCount = currentCount;
            this.limit = limit;
            this.remaining = remaining;
            this.allowed = allowed;
        }
        
        public int getCurrentCount() {
            return currentCount;
        }
        
        public int getLimit() {
            return limit;
        }
        
        public int getRemaining() {
            return remaining;
        }
        
        public boolean isAllowed() {
            return allowed;
        }
    }
}
