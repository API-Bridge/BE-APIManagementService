package org.example.APIManagementSvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.annotation.RateLimit;
import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.dto.cache.ApiHealthStatusDto;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.dto.common.PageResponse;
import org.example.APIManagementSvc.service.ApiHealthCheckService;
import org.example.APIManagementSvc.service.ExternalApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * API 헬스체크 컨트롤러
 * 외부 API의 상태를 확인하고 모니터링하는 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api-health")
@RequiredArgsConstructor
public class ApiHealthController {

    private final ApiHealthCheckService apiHealthCheckService;
    private final ExternalApiService externalApiService;

    /**
     * 특정 API의 헬스체크 수행
     */
    @GetMapping("/check/{apiId}")
    @RateLimit(value = 10, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<ApiHealthStatusDto>> checkApiHealth(@PathVariable String apiId) {
        log.info("API 헬스체크 요청: {}", apiId);
        
        try {
            // API 존재 여부 확인
            ExternalApi api = externalApiService.getApiById(apiId)
                    .orElseThrow(() -> new IllegalArgumentException("API not found: " + apiId));
            
            // 헬스체크 수행
            ApiHealthStatusDto healthStatus = apiHealthCheckService.checkApiHealth(api);
            
            return ResponseEntity.ok(ApiResponse.success(healthStatus));
            
        } catch (Exception e) {
            log.error("API 헬스체크 실패: {} - {}", apiId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("헬스체크 실패: " + e.getMessage()));
        }
    }

    /**
     * 특정 API의 헬스 상태 조회 (캐시에서)
     */
    @GetMapping("/status/{apiId}")
    @RateLimit(value = 30, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<ApiHealthStatusDto>> getApiHealthStatus(@PathVariable String apiId) {
        log.debug("API 헬스 상태 조회: {}", apiId);
        
        try {
            ApiHealthStatusDto healthStatus = apiHealthCheckService.getApiHealthStatus(apiId);
            
            if (healthStatus != null) {
                return ResponseEntity.ok(ApiResponse.success(healthStatus));
            } else {
                return ResponseEntity.ok(ApiResponse.success(null, "헬스체크 결과가 없습니다."));
            }
            
        } catch (Exception e) {
            log.error("API 헬스 상태 조회 실패: {} - {}", apiId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("상태 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 특정 API의 헬스체크 강제 갱신
     */
    @PostMapping("/refresh/{apiId}")
    @RateLimit(value = 5, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<ApiHealthStatusDto>> refreshApiHealth(@PathVariable String apiId) {
        log.info("API 헬스체크 강제 갱신 요청: {}", apiId);
        
        try {
            // API 존재 여부 확인
            ExternalApi api = externalApiService.getApiById(apiId)
                    .orElseThrow(() -> new IllegalArgumentException("API not found: " + apiId));
            
            // 헬스체크 강제 갱신
            apiHealthCheckService.refreshApiHealth(api);
            
            // 갱신된 상태 조회
            ApiHealthStatusDto healthStatus = apiHealthCheckService.getApiHealthStatus(apiId);
            
            return ResponseEntity.ok(ApiResponse.success(healthStatus, "헬스체크가 갱신되었습니다."));
            
        } catch (Exception e) {
            log.error("API 헬스체크 갱신 실패: {} - {}", apiId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("헬스체크 갱신 실패: " + e.getMessage()));
        }
    }

    /**
     * 전체 API 헬스체크 일괄 수행
     */
    @PostMapping("/check-all")
    @RateLimit(value = 1, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<String>> checkAllApisHealth() {
        log.info("전체 API 헬스체크 일괄 수행 요청");
        
        try {
            // 모든 활성 API 조회
            List<ExternalApi> apis = externalApiService.getAllActiveApis();
            
            if (apis.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("갱신할 API가 없습니다."));
            }
            
            // 비동기로 헬스체크 수행
            apiHealthCheckService.checkAllApisHealth(apis);
            
            return ResponseEntity.ok(ApiResponse.success(
                String.format("%d개 API의 헬스체크가 시작되었습니다.", apis.size())
            ));
            
        } catch (Exception e) {
            log.error("전체 API 헬스체크 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("전체 헬스체크 실패: " + e.getMessage()));
        }
    }

    /**
     * 도메인별 API 헬스 상태 요약
     */
    @GetMapping("/summary/domain/{domain}")
    @RateLimit(value = 20, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<Object>> getHealthSummaryByDomain(@PathVariable String domain) {
        log.debug("도메인별 헬스 상태 요약 조회: {}", domain);
        
        try {
            // 도메인별 API 조회
            List<ExternalApi> apis = externalApiService.searchApis(
                "도메인:" + org.example.APIManagementSvc.domain.enums.ApiDomain.valueOf(domain.toUpperCase())
            );
            
            // 각 API의 헬스 상태 조회
            List<ApiHealthStatusDto> healthStatuses = apis.stream()
                    .map(api -> apiHealthCheckService.getApiHealthStatus(api.getApiId()))
                    .filter(status -> status != null)
                    .toList();
            
            // 상태별 통계 계산
            long healthyCount = healthStatuses.stream().filter(ApiHealthStatusDto::isHealthy).count();
            long unhealthyCount = healthStatuses.size() - healthyCount;
            
            var summary = Map.of(
                "domain", domain,
                "totalApis", apis.size(),
                "healthyApis", healthyCount,
                "unhealthyApis", unhealthyCount,
                "healthRate", apis.isEmpty() ? 0.0 : (double) healthyCount / apis.size() * 100,
                "healthStatuses", healthStatuses
            );
            
            return ResponseEntity.ok(ApiResponse.success(summary));
            
        } catch (Exception e) {
            log.error("도메인별 헬스 상태 요약 조회 실패: {} - {}", domain, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("요약 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 전체 API 헬스체크 결과 조회 (페이징)
     */
    @GetMapping("/health/status/all")
    public ApiResponse<PageResponse<ApiHealthStatusDto>> getAllApiHealthStatus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            List<ExternalApi> apis = externalApiService.getAllActiveApis();
            
            // 페이징 처리
            int start = page * size;
            int end = Math.min(start + size, apis.size());
            
            if (start >= apis.size()) {
                return ApiResponse.success(new PageResponse<>(
                    new ArrayList<>(), page, size, apis.size(), apis.size()
                ));
            }
            
            List<ApiHealthStatusDto> healthStatuses = apis.subList(start, end)
                .stream()
                .map(api -> apiHealthCheckService.getApiHealthStatus(api.getApiId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            return ApiResponse.success(new PageResponse<>(
                healthStatuses, page, size, apis.size(), apis.size()
            ));
            
        } catch (Exception e) {
            log.error("전체 API 헬스체크 결과 조회 실패: {}", e.getMessage(), e);
            return ApiResponse.error("전체 API 헬스체크 결과 조회에 실패했습니다: " + e.getMessage());
        }
    }

    // === 사용 불가능한 API 목록 관리 ===

    /**
     * 사용 불가능한 API 목록 조회
     * 
     * @return 사용 불가능한 API ID 목록
     */
    @GetMapping("/health/unavailable")
    public ApiResponse<List<String>> getUnavailableApis() {
        try {
            List<String> unavailableApis = apiHealthCheckService.getUnavailableApisFromCache();
            return ApiResponse.success(unavailableApis);
        } catch (Exception e) {
            log.error("사용 불가능한 API 목록 조회 실패: {}", e.getMessage(), e);
            return ApiResponse.error("사용 불가능한 API 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 사용 불가능한 API 개수 조회
     * 
     * @return 사용 불가능한 API 개수
     */
    @GetMapping("/health/unavailable/count")
    public ApiResponse<Integer> getUnavailableApisCount() {
        try {
            int count = apiHealthCheckService.getUnavailableApisCount();
            return ApiResponse.success(count);
        } catch (Exception e) {
            log.error("사용 불가능한 API 개수 조회 실패: {}", e.getMessage(), e);
            return ApiResponse.error("사용 불가능한 API 개수 조회에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 특정 API 사용 불가능 여부 확인
     * 
     * @param apiId API ID
     * @return 사용 불가능 여부
     */
    @GetMapping("/health/unavailable/{apiId}")
    public ApiResponse<Boolean> isApiUnavailable(@PathVariable String apiId) {
        try {
            boolean isUnavailable = apiHealthCheckService.isApiUnavailable(apiId);
            return ApiResponse.success(isUnavailable);
        } catch (Exception e) {
            log.error("API 사용 불가능 여부 확인 실패: {} - {}", apiId, e.getMessage(), e);
            return ApiResponse.error("API 사용 불가능 여부 확인에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 사용 불가능한 API 목록 캐시 강제 갱신
     * 
     * @return 갱신 성공 여부
     */
    @PostMapping("/health/unavailable/refresh")
    public ApiResponse<String> refreshUnavailableApisCache() {
        try {
            List<ExternalApi> apis = externalApiService.getAllActiveApis();
            apiHealthCheckService.refreshUnavailableApisCache(apis);
            return ApiResponse.success("사용 불가능한 API 목록 캐시가 성공적으로 갱신되었습니다.");
        } catch (Exception e) {
            log.error("사용 불가능한 API 목록 캐시 갱신 실패: {}", e.getMessage(), e);
            return ApiResponse.error("사용 불가능한 API 목록 캐시 갱신에 실패했습니다: " + e.getMessage());
        }
    }
}
