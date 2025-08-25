package org.example.APIManagementSvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.dto.cache.ApiHealthStatusDto;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.dto.common.PageResponse;
import org.example.APIManagementSvc.service.ApiHealthCheckService;
import org.example.APIManagementSvc.service.ApiManagementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * API 헬스체크 컨트롤러
 * 스케줄러로 자동화된 헬스체크 결과를 조회하는 기능만 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api-health")
@RequiredArgsConstructor
public class ApiHealthController {

    private final ApiHealthCheckService apiHealthCheckService;
    private final ApiManagementService apiManagementService;

    /**
     * 전체 API 헬스체크 결과 조회 (페이징)
     * GET /api-health/health/status/all
     */
    @GetMapping("/health/status/all")
    public ApiResponse<PageResponse<ApiHealthStatusDto>> getAllApiHealthStatus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            List<ExternalApi> apis = apiManagementService.getAllActiveApis();
            
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

    /**
     * 사용 불가능한 API 목록 조회
     * GET /api-health/health/unavailable
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
     * 특정 API 사용 불가능 여부 확인
     * GET /api-health/health/unavailable/{apiId}
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
}
