package org.example.APIManagementSvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.annotation.RateLimit;
import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.dto.cache.ApiHealthStatusDto;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.service.ApiHealthCheckService;
import org.example.APIManagementSvc.service.ExternalApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Map;

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
     * 특정 API의 헬스 상태 조회 (캐시에서) - X
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
     * 특정 API의 헬스체크 강제 갱신 --???
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
     * 도메인별 API 헬스 상태 요약 - X
     */
    @GetMapping("/summary/domain/{domain}")
    @RateLimit(value = 20, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<Object>> getHealthSummaryByDomain(@PathVariable String domain) {
        log.debug("도메인별 헬스 상태 요약 조회: {}", domain);
        
        try {
            // 도메인별 API 조회
            List<ExternalApi> apis = externalApiService.getApisByDomain(
                org.example.APIManagementSvc.domain.enums.ApiDomain.valueOf(domain.toUpperCase())
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
}
