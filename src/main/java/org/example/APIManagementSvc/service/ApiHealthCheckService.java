package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.dto.cache.ApiHealthStatusDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * API 헬스체크 서비스
 * 외부 API의 상태를 확인하고 결과를 캐시에 저장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiHealthCheckService {

    private final RedisCacheService redisCacheService;
    private final RestTemplate restTemplate;

    private static final String HEALTH_CHECK_CACHE_PREFIX = "api:health:";
    private static final int HEALTH_CHECK_TIMEOUT_SECONDS = 10;
    private static final int CACHE_TTL_HOURS = 1; // 1시간 동안 캐시 유지

    /**
     * API 헬스체크 수행
     */
    public ApiHealthStatusDto checkApiHealth(ExternalApi api) {
        log.info("API 헬스체크 시작: {}", api.getApiName());
        
        try {
            // 비동기로 헬스체크 수행
            CompletableFuture<ApiHealthStatusDto> healthCheckFuture = CompletableFuture.supplyAsync(() -> {
                return performHealthCheck(api);
            });

            // 타임아웃 설정
            ApiHealthStatusDto result = healthCheckFuture.get(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            // 결과를 캐시에 저장
            saveHealthStatusToCache(api.getApiId(), result);
            
            log.info("API 헬스체크 완료: {} - 상태: {}", api.getApiName(), result.getStatus());
            return result;
            
        } catch (Exception e) {
            log.error("API 헬스체크 실패: {} - 오류: {}", api.getApiName(), e.getMessage());
            
            // 실패 상태를 캐시에 저장
            ApiHealthStatusDto failureResult = ApiHealthStatusDto.builder()
                    .apiId(api.getApiId())
                    .status("FAILED")
                    .responseTime(-1)
                    .errorMessage(e.getMessage())
                    .checkedAt(LocalDateTime.now())
                    .build();
            
            saveHealthStatusToCache(api.getApiId(), failureResult);
            return failureResult;
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
     * API ID로 헬스체크 수행 (캐시 우선)
     */
    public ApiHealthStatusDto checkApiHealthById(String apiId) {
        // 먼저 캐시에서 조회
        ApiHealthStatusDto cachedStatus = getApiHealthStatusFromCache(apiId);
        
        if (cachedStatus != null && !isCacheExpired(cachedStatus)) {
            log.debug("캐시된 헬스체크 결과 사용: {}", apiId);
            return cachedStatus;
        }
        
        // 캐시가 없거나 만료된 경우 새로 헬스체크 수행
        log.debug("캐시 만료 또는 없음, 새로 헬스체크 수행: {}", apiId);
        return null; // 실제 API 엔티티가 필요하므로 호출자가 처리
    }

    /**
     * API ID로 헬스체크 상태 조회 (공개 메서드)
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
     * 특정 API의 헬스체크 강제 갱신
     */
    public void refreshApiHealth(ExternalApi api) {
        log.info("API 헬스체크 강제 갱신: {}", api.getApiName());
        
        // 기존 캐시 삭제
        deleteHealthStatusFromCache(api.getApiId());
        
        // 새로 헬스체크 수행
        checkApiHealth(api);
    }

    /**
     * 모든 API 헬스체크 일괄 수행
     */
    public void checkAllApisHealth(List<ExternalApi> apis) {
        log.info("전체 API 헬스체크 시작: {}개", apis.size());
        
        apis.parallelStream().forEach(api -> {
            try {
                checkApiHealth(api);
            } catch (Exception e) {
                log.error("API 헬스체크 실패: {} - 오류: {}", api.getApiName(), e.getMessage());
            }
        });
        
        log.info("전체 API 헬스체크 완료");
    }
}
