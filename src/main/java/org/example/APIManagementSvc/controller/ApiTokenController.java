package org.example.APIManagementSvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.annotation.RateLimit;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.service.ApiTokenRefreshService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * API 토큰 갱신 컨트롤러
 * API 토큰의 자동/수동 갱신을 관리하는 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/tokens")
@RequiredArgsConstructor
public class ApiTokenController {

    private final ApiTokenRefreshService apiTokenRefreshService;

    /**
     * 특정 API의 토큰 수동 갱신
     */
    @PostMapping("/refresh/{apiId}")
    @RateLimit(value = 5, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<String>> manuallyRefreshToken(@PathVariable String apiId) {
        log.info("수동 토큰 갱신 요청: {}", apiId);
        
        try {
            apiTokenRefreshService.manuallyRefreshToken(apiId);
            
            return ResponseEntity.ok(ApiResponse.success(
                "API " + apiId + "의 토큰이 성공적으로 갱신되었습니다."
            ));
            
        } catch (Exception e) {
            log.error("수동 토큰 갱신 실패: {} - {}", apiId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("토큰 갱신 실패: " + e.getMessage()));
        }
    }

    /**
     * 특정 API의 토큰 갱신 상태 확인
     */
    @GetMapping("/status/{apiId}")
    @RateLimit(value = 20, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<String>> getTokenRefreshStatus(@PathVariable String apiId) {
        log.debug("토큰 갱신 상태 확인: {}", apiId);
        
        try {
            String status = apiTokenRefreshService.getTokenRefreshStatus(apiId);
            
            return ResponseEntity.ok(ApiResponse.success(status));
            
        } catch (Exception e) {
            log.error("토큰 갱신 상태 확인 실패: {} - {}", apiId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("상태 확인 실패: " + e.getMessage()));
        }
    }

    /**
     * 전체 토큰 자동 갱신 작업 트리거
     */
    @PostMapping("/refresh-all")
    @RateLimit(value = 1, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<String>> triggerAllTokenRefresh() {
        log.info("전체 토큰 자동 갱신 작업 트리거");
        
        try {
            // 스케줄된 작업을 즉시 실행
            apiTokenRefreshService.refreshExpiredTokens();
            
            return ResponseEntity.ok(ApiResponse.success(
                "전체 토큰 갱신 작업이 시작되었습니다."
            ));
            
        } catch (Exception e) {
            log.error("전체 토큰 갱신 작업 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("토큰 갱신 작업 실패: " + e.getMessage()));
        }
    }

    /**
     * SGIS API 토큰 발급 URL 정보 조회
     */
    @GetMapping("/sgis-info")
    @RateLimit(value = 50, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<Object>> getSgisTokenInfo() {
        log.debug("SGIS API 토큰 발급 정보 조회");
        
        try {
            var sgisInfo = Map.of(
                "tokenUrl", "https://sgisapi.kostat.go.kr/OpenAPI3/auth/authentication.json",
                "description", "통계청 SGIS API 토큰 발급 엔드포인트",
                "refreshInterval", "4시간마다 자동 갱신",
                "note", "실제 구현 시 SGIS API 문서를 참조하여 정확한 요청 형식 구성 필요"
            );
            
            return ResponseEntity.ok(ApiResponse.success(sgisInfo));
            
        } catch (Exception e) {
            log.error("SGIS API 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("정보 조회 실패: " + e.getMessage()));
        }
    }
}
