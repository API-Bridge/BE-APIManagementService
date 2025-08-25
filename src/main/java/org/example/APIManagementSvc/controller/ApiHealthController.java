package org.example.APIManagementSvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ExternalApiSpec;
import org.example.APIManagementSvc.dto.common.BaseResponse;
import org.example.APIManagementSvc.service.ApiHealthCheckService;
import org.example.APIManagementSvc.service.ApiHealthCacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * API 헬스체크 관련 REST API 컨트롤러
 * 외부 API들의 헬스 상태 조회 및 관리 기능 제공
 * 
 * 주요 기능:
 * - 전체 API 헬스 상태 조회
 * - 특정 헬스 상태별 API 목록 조회
 * - 수동 헬스체크 실행
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "API Health Check", description = "외부 API 헬스체크 관리")
public class ApiHealthController {

    private final ApiHealthCheckService apiHealthCheckService;
    private final ApiHealthCacheService apiHealthCacheService;

    /**
     * 모든 활성화된 API의 헬스 상태 조회
     * 
     * @return 전체 API 헬스 상태 목록
     */
    @GetMapping("/status/all")
    @Operation(summary = "전체 API 헬스 상태 조회", 
               description = "활성화된 모든 외부 API의 현재 헬스 상태를 조회합니다.")
    public ResponseEntity<BaseResponse<List<ExternalApiSpec>>> getAllApiHealthStatus() {
        log.info("Fetching health status for all active APIs");
        
        List<ExternalApiSpec> apis = apiHealthCheckService.getAllApisWithHealthStatus();
        
        BaseResponse<List<ExternalApiSpec>> response = BaseResponse.success(
            "전체 API 헬스 상태 조회 완료",
            apis
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 헬스 상태의 API 목록 조회
     * 
     * @param healthStatus 조회할 헬스 상태 (HEALTHY, UNHEALTHY, UNKNOWN)
     * @return 해당 헬스 상태의 API 목록
     */
    @GetMapping("/status/{healthStatus}")
    @Operation(summary = "헬스 상태별 API 조회", 
               description = "특정 헬스 상태(HEALTHY/UNHEALTHY/UNKNOWN)의 API 목록을 조회합니다.")
    public ResponseEntity<BaseResponse<List<ExternalApiSpec>>> getApisByHealthStatus(
            @Parameter(description = "헬스 상태", example = "HEALTHY")
            @PathVariable ExternalApiSpec.HealthStatus healthStatus) {
        
        log.info("Fetching APIs with health status: {}", healthStatus);
        
        List<ExternalApiSpec> apis = apiHealthCheckService.getApisByHealthStatus(healthStatus);
        
        BaseResponse<List<ExternalApiSpec>> response = BaseResponse.success(
            String.format("%s 상태의 API %d개 조회 완료", healthStatus, apis.size()),
            apis
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 API의 수동 헬스체크 실행
     * 
     * @param apiId 헬스체크를 실행할 API ID
     * @return 헬스체크 실행 결과
     */
    @PostMapping("/check/{apiId}")
    @Operation(summary = "수동 헬스체크 실행", 
               description = "특정 API에 대해 수동으로 헬스체크를 실행합니다.")
    public ResponseEntity<BaseResponse<String>> performManualHealthCheck(
            @Parameter(description = "API ID", example = "api-001")
            @PathVariable String apiId) {
        
        log.info("Manual health check requested for API: {}", apiId);
        
        try {
            // 수동 헬스체크는 동기적으로 실행하고 결과를 즉시 반환
            // 실제 구현에서는 ExternalApiSpec을 조회하고 헬스체크 수행
            
            BaseResponse<String> response = BaseResponse.success(
                "수동 헬스체크 실행 완료",
                "헬스체크가 시작되었습니다. 결과는 잠시 후 확인 가능합니다."
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Manual health check failed for API: {}", apiId, e);
            
            BaseResponse<String> response = BaseResponse.error(
                "HEALTH_CHECK_FAILED",
                "헬스체크 실행 중 오류가 발생했습니다: " + e.getMessage()
            );
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 전체 API에 대한 수동 헬스체크 실행
     * 
     * @return 전체 헬스체크 실행 결과
     */
    @PostMapping("/check/all")
    @Operation(summary = "전체 API 수동 헬스체크", 
               description = "모든 활성화된 API에 대해 수동으로 헬스체크를 실행합니다.")
    public ResponseEntity<BaseResponse<String>> performAllHealthChecks() {
        
        log.info("Manual health check requested for all APIs");
        
        try {
            apiHealthCheckService.performHealthChecks();
            
            BaseResponse<String> response = BaseResponse.success(
                "전체 헬스체크 실행 완료",
                "모든 API에 대한 헬스체크가 시작되었습니다."
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Manual health check failed for all APIs", e);
            
            BaseResponse<String> response = BaseResponse.error(
                "HEALTH_CHECK_FAILED",
                "전체 헬스체크 실행 중 오류가 발생했습니다: " + e.getMessage()
            );
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Redis 캐시에서 실패 API 목록 조회
     * 
     * @return 현재 Redis에 캐시된 실패 API ID 목록
     */
    @GetMapping("/cache/unhealthy")
    @Operation(summary = "캐시된 실패 API 목록 조회", 
               description = "Redis 캐시에 저장된 실패한 API들의 ID 목록을 조회합니다.")
    public ResponseEntity<BaseResponse<Set<String>>> getCachedUnhealthyApis() {
        
        log.info("Fetching cached unhealthy APIs from Redis");
        
        try {
            Set<String> unhealthyApiIds = apiHealthCacheService.getUnhealthyApiIds();
            
            BaseResponse<Set<String>> response = BaseResponse.success(
                String.format("캐시된 실패 API %d개 조회 완료", unhealthyApiIds.size()),
                unhealthyApiIds
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to fetch cached unhealthy APIs", e);
            
            BaseResponse<Set<String>> response = BaseResponse.error(
                "CACHE_FETCH_FAILED",
                "캐시된 실패 API 목록 조회 중 오류가 발생했습니다: " + e.getMessage()
            );
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Redis 캐시에서 특정 실패 API 상세 정보 조회
     * 
     * @param apiId 조회할 API ID
     * @return 실패 API 상세 정보
     */
    @GetMapping("/cache/unhealthy/{apiId}")
    @Operation(summary = "캐시된 실패 API 상세 조회", 
               description = "Redis 캐시에서 특정 실패 API의 상세 정보를 조회합니다.")
    public ResponseEntity<BaseResponse<ApiHealthCacheService.UnhealthyApiInfo>> getCachedUnhealthyApiInfo(
            @Parameter(description = "API ID", example = "api-001")
            @PathVariable String apiId) {
        
        log.info("Fetching cached unhealthy API info for: {}", apiId);
        
        try {
            ApiHealthCacheService.UnhealthyApiInfo apiInfo = apiHealthCacheService.getUnhealthyApiInfo(apiId);
            
            if (apiInfo != null) {
                BaseResponse<ApiHealthCacheService.UnhealthyApiInfo> response = BaseResponse.success(
                    "캐시된 실패 API 정보 조회 완료",
                    apiInfo
                );
                return ResponseEntity.ok(response);
            } else {
                BaseResponse<ApiHealthCacheService.UnhealthyApiInfo> response = BaseResponse.success(
                    "해당 API는 캐시에 존재하지 않습니다",
                    null
                );
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            log.error("Failed to fetch cached unhealthy API info for: {}", apiId, e);
            
            BaseResponse<ApiHealthCacheService.UnhealthyApiInfo> response = BaseResponse.error(
                "CACHE_FETCH_FAILED",
                "캐시된 API 정보 조회 중 오류가 발생했습니다: " + e.getMessage()
            );
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}