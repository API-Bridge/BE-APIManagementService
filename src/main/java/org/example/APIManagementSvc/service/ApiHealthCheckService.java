package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.dto.cache.ApiHealthStatusDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * API 헬스체크 서비스
 * 스케줄러 기반으로 외부 API의 상태를 자동으로 확인하고 결과를 캐시에 저장합니다.
 * ApiHealthController에서 헬스체크 결과를 조회할 수 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiHealthCheckService {

    private final RedisCacheService redisCacheService;
    private final RestTemplate restTemplate;
    private final ApiManagementService apiManagementService;

    private static final String HEALTH_CHECK_CACHE_PREFIX = "api:health:";
    private static final int HEALTH_CHECK_TIMEOUT_SECONDS = 10;
    private static final int CACHE_TTL_HOURS = 1; // 1시간 동안 캐시 유지

    // 사용 불가능한 API 목록 캐시 관련 상수
    private static final String UNAVAILABLE_APIS_CACHE_KEY = "unavailable_apis";
    private static final int UNAVAILABLE_APIS_CACHE_TTL_HOURS = 2; // 2시간 동안 캐시 유지

    // === 자동 스케줄링 헬스체크 ===

    /**
     * 매 5분마다 활성 API들의 헬스체크 수행
     * 외부에서 호출할 수 없으며 스케줄러에 의해 자동 실행됩니다.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5분마다
    public void scheduledHealthCheck() {
        log.info("정기 헬스체크 시작 (5분 간격)");
        try {
            List<ExternalApi> activeApis = apiManagementService.getAllActiveApis();
            if (!activeApis.isEmpty()) {
                checkAllApisHealth(activeApis);
                // 사용 불가능한 API 목록 캐시 업데이트
                updateUnavailableApisCache(activeApis);
                log.info("정기 헬스체크 완료: {}개 API", activeApis.size());
            } else {
                log.debug("헬스체크할 활성 API가 없습니다");
            }
        } catch (Exception e) {
            log.error("정기 헬스체크 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 매 30분마다 중요 API들의 상세 헬스체크 수행
     * 외부에서 호출할 수 없으며 스케줄러에 의해 자동 실행됩니다.
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30분마다
    public void scheduledDetailedHealthCheck() {
        log.info("상세 헬스체크 시작 (30분 간격)");
        try {
            List<ExternalApi> importantApis = apiManagementService.getImportantApis();
            if (!importantApis.isEmpty()) {
                importantApis.parallelStream().forEach(api -> {
                    try {
                        // 상세 헬스체크 (더 긴 타임아웃, 재시도 등)
                        performDetailedHealthCheck(api);
                    } catch (Exception e) {
                        log.error("상세 헬스체크 실패: {} - {}", api.getApiName(), e.getMessage());
                    }
                });
                // 사용 불가능한 API 목록 캐시 업데이트
                updateUnavailableApisCache(importantApis);
                log.info("상세 헬스체크 완료: {}개 중요 API", importantApis.size());
            }
        } catch (Exception e) {
            log.error("상세 헬스체크 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 매일 새벽 2시에 전체 API 헬스체크 수행
     * 외부에서 호출할 수 없으며 스케줄러에 의해 자동 실행됩니다.
     */
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
    public void scheduledDailyHealthCheck() {
        log.info("일일 전체 헬스체크 시작");
        try {
            List<ExternalApi> allApis = apiManagementService.getAllApis();
            if (!allApis.isEmpty()) {
                checkAllApisHealth(allApis);
                // 사용 불가능한 API 목록 캐시 업데이트
                updateUnavailableApisCache(allApis);
                log.info("일일 전체 헬스체크 완료: {}개 API", allApis.size());
            }
        } catch (Exception e) {
            log.error("일일 전체 헬스체크 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 상세 헬스체크 수행 (더 정확한 상태 확인)
     */
    private void performDetailedHealthCheck(ExternalApi api) {
        log.debug("상세 헬스체크 시작: {}", api.getApiName());
        
        try {
            // 여러 번의 요청으로 안정성 확인
            int successCount = 0;
            int totalAttempts = 3;
            
            for (int i = 0; i < totalAttempts; i++) {
                try {
                    ResponseEntity<String> response = restTemplate.getForEntity(api.getApiUrl(), String.class);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount++;
                    }
                    Thread.sleep(1000); // 1초 간격으로 요청
                } catch (Exception e) {
                    log.debug("상세 헬스체크 시도 {} 실패: {} - {}", i + 1, api.getApiName(), e.getMessage());
                }
            }
            
            // 성공률 계산
            double successRate = (double) successCount / totalAttempts;
            String status = successRate >= 0.7 ? "HEALTHY" : successRate >= 0.3 ? "UNSTABLE" : "UNHEALTHY";
            
            ApiHealthStatusDto detailedResult = ApiHealthStatusDto.builder()
                    .apiId(api.getApiId())
                    .status(status)
                    .responseTime(-1) // 상세 헬스체크에서는 응답시간 측정하지 않음
                    .successRate(successRate)
                    .checkedAt(LocalDateTime.now())
                    .build();
            
            saveHealthStatusToCache(api.getApiId(), detailedResult);
            log.debug("상세 헬스체크 완료: {} - 상태: {}, 성공률: {}", api.getApiName(), status, successRate);
            
        } catch (Exception e) {
            log.error("상세 헬스체크 실패: {} - {}", api.getApiName(), e.getMessage());
            
            ApiHealthStatusDto failureResult = ApiHealthStatusDto.builder()
                    .apiId(api.getApiId())
                    .status("FAILED")
                    .responseTime(-1)
                    .errorMessage(e.getMessage())
                    .checkedAt(LocalDateTime.now())
                    .build();
            
            saveHealthStatusToCache(api.getApiId(), failureResult);
        }
    }

    /**
     * 실제 헬스체크 수행
     */
    private ApiHealthStatusDto performHealthCheck(ExternalApi api) {
        long startTime = System.currentTimeMillis();
        
        try {
            // API 엔드포인트에 HEAD 요청으로 헬스체크
            ResponseEntity<String> response = restTemplate.getForEntity(api.getApiUrl(), String.class);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            String status = response.getStatusCode().is2xxSuccessful() ? "HEALTHY" : "UNHEALTHY";
            
            return ApiHealthStatusDto.builder()
                    .apiId(api.getApiId())
                    .status(status)
                    .responseTime(responseTime)
                    .httpStatus(response.getStatusCode().value())
                    .checkedAt(LocalDateTime.now())
                    .build();
                    
        } catch (ResourceAccessException e) {
            // 연결 실패 (타임아웃, 연결 거부 등)
            long responseTime = System.currentTimeMillis() - startTime;
            
            return ApiHealthStatusDto.builder()
                    .apiId(api.getApiId())
                    .status("UNREACHABLE")
                    .responseTime(responseTime)
                    .errorMessage("Connection failed: " + e.getMessage())
                    .checkedAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            // 기타 오류
            long responseTime = System.currentTimeMillis() - startTime;
            
            return ApiHealthStatusDto.builder()
                    .apiId(api.getApiId())
                    .status("ERROR")
                    .responseTime(responseTime)
                    .errorMessage(e.getMessage())
                    .checkedAt(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * 헬스체크 결과를 캐시에 저장
     */
    private void saveHealthStatusToCache(String apiId, ApiHealthStatusDto healthStatus) {
        try {
            String cacheKey = "api_health:" + apiId;
            // RedisCacheService의 cacheApiStatus 메서드 사용
            // ApiStatusCacheDto로 변환 필요
            log.debug("헬스체크 결과 캐시 저장: {}", apiId);
        } catch (Exception e) {
            log.warn("헬스체크 결과 캐시 저장 실패: {} - {}", apiId, e.getMessage());
        }
    }

    /**
     * 캐시에서 API 헬스 상태 조회
     */
    private ApiHealthStatusDto getApiHealthStatusFromCache(String apiId) {
        try {
            String cacheKey = "api_health:" + apiId;
            // RedisCacheService의 getApiStatus 메서드 사용
            // ApiStatusCacheDto에서 ApiHealthStatusDto로 변환 필요
            log.debug("캐시에서 헬스체크 결과 조회: {}", apiId);
            return null; // 임시로 null 반환
        } catch (Exception e) {
            log.warn("캐시에서 헬스체크 결과 조회 실패: {} - {}", apiId, e.getMessage());
            return null;
        }
    }

    /**
     * 캐시에서 API 헬스 상태 삭제
     */
    private void deleteHealthStatusFromCache(String apiId) {
        try {
            String cacheKey = "api_health:" + apiId;
            // RedisCacheService의 deleteApiStatus 메서드 사용
            log.debug("캐시에서 헬스체크 결과 삭제: {}", apiId);
        } catch (Exception e) {
            log.warn("캐시에서 헬스체크 결과 삭제 실패: {} - {}", apiId, e.getMessage());
        }
    }

    /**
     * API ID로 헬스체크 상태 조회 (공개 메서드)
     * ApiHealthController에서 사용
     */
    public ApiHealthStatusDto getApiHealthStatus(String apiId) {
        // 먼저 캐시에서 조회
        ApiHealthStatusDto cachedStatus = getApiHealthStatusFromCache(apiId);
        
        if (cachedStatus != null && !isCacheExpired(cachedStatus)) {
            log.debug("캐시된 헬스체크 결과 사용: {}", apiId);
            return cachedStatus;
        }
        
        // 캐시가 없거나 만료된 경우 null 반환
        log.debug("캐시 만료 또는 없음: {}", apiId);
        return null;
    }

    /**
     * 캐시 만료 여부 확인
     */
    private boolean isCacheExpired(ApiHealthStatusDto healthStatus) {
        if (healthStatus.getCheckedAt() == null) return true;
        
        LocalDateTime expiryTime = healthStatus.getCheckedAt().plusHours(CACHE_TTL_HOURS);
        return LocalDateTime.now().isAfter(expiryTime);
    }

    /**
     * 모든 API 헬스체크 일괄 수행
     * 스케줄러에서 사용
     */
    private void checkAllApisHealth(List<ExternalApi> apis) {
        log.info("전체 API 헬스체크 시작: {}개", apis.size());
        
        apis.parallelStream().forEach(api -> {
            try {
                // 각 API에 대해 헬스체크 수행
                ApiHealthStatusDto healthStatus = performHealthCheck(api);
                saveHealthStatusToCache(api.getApiId(), healthStatus);
            } catch (Exception e) {
                log.error("API 헬스체크 실패: {} - 오류: {}", api.getApiName(), e.getMessage());
            }
        });
        
        log.info("전체 API 헬스체크 완료");
    }

    /**
     * 사용 불가능한 API 목록 캐시 업데이트
     */
    private void updateUnavailableApisCache(List<ExternalApi> apis) {
        try {
            List<String> unavailableApiIds = new ArrayList<>();
            
            // 각 API의 헬스체크 결과를 확인하여 사용 불가능한 API 식별
            for (ExternalApi api : apis) {
                ApiHealthStatusDto healthStatus = getApiHealthStatus(api.getApiId());
                if (healthStatus != null && !healthStatus.isHealthy()) {
                    unavailableApiIds.add(api.getApiId());
                }
            }
            
            // Redis에 사용 불가능한 API 목록 저장
            redisCacheService.cacheApiKeyList(UNAVAILABLE_APIS_CACHE_KEY, unavailableApiIds, UNAVAILABLE_APIS_CACHE_TTL_HOURS * 60 * 60);
            
            log.debug("사용 불가능한 API 목록 캐시 업데이트 완료. 총 {}개 API", unavailableApiIds.size());
        } catch (Exception e) {
            log.error("사용 불가능한 API 목록 캐시 업데이트 실패: {}", e.getMessage(), e);
        }
    }

    // === 사용 불가능한 API 목록 관리 (ApiHealthController에서 사용) ===

    /**
     * 캐시에서 사용 불가능한 API 목록 조회
     * ApiHealthController에서 사용
     * 
     * @return 사용 불가능한 API ID 목록 (캐시에 없으면 빈 리스트)
     */
    @SuppressWarnings("unchecked")
    public List<String> getUnavailableApisFromCache() {
        try {
            Object cached = redisCacheService.getApiKeyList(UNAVAILABLE_APIS_CACHE_KEY);
            if (cached instanceof List) {
                List<String> unavailableApis = (List<String>) cached;
                log.debug("사용 불가능한 API 목록 캐시 히트: {}개", unavailableApis.size());
                return unavailableApis;
            } else {
                log.debug("사용 불가능한 API 목록 캐시 미스 또는 형식 오류");
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("사용 불가능한 API 목록 캐시 조회 실패: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 특정 API가 사용 불가능한지 확인
     * ApiHealthController에서 사용
     * 
     * @param apiId API ID
     * @return 사용 불가능 여부
     */
    public boolean isApiUnavailable(String apiId) {
        List<String> unavailableApis = getUnavailableApisFromCache();
        return unavailableApis.contains(apiId);
    }
}
