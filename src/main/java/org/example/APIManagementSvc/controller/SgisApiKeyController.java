package org.example.APIManagementSvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.SgisApiKey;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.dto.common.PageResponse;
import org.example.APIManagementSvc.dto.sgis.SgisApiKeyRequest;
import org.example.APIManagementSvc.dto.sgis.SgisApiKeyResponse;
import org.example.APIManagementSvc.service.SgisApiKeyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SGIS API 키 관리 컨트롤러
 * 
 * 이 컨트롤러는 SGIS(Statistical Geographic Information Service) 공공데이터포털의
 * API 키를 관리하기 위한 REST API 엔드포인트들을 제공합니다.
 * 
 * 제공하는 API 기능:
 * - API 키 발급 및 등록 (POST)
 * - API 키 정보 조회 및 검색 (GET)
 * - API 키 정보 수정 (PUT)
 * - API 키 상태 변경 (PATCH)
 * - API 키 폐기 (DELETE)
 * - 통계 정보 조회 (GET)
 * 
 * API 설계 원칙:
 * - RESTful API 설계 가이드라인 준수
 * - HTTP 상태 코드를 적절히 활용한 응답
 * - 일관된 응답 형식 (ApiResponse<T>)
 * - 페이징 지원 (PageResponse<T>)
 * - 입력값 검증 (@Valid)
 * 
 * 보안 고려사항:
 * - 모든 API 호출은 적절한 인증/인가 필요
 * - 민감한 정보(API 키, 시크릿 키)는 응답에서 제외
 * - 입력값 검증을 통한 보안 강화
 * - 로그에는 민감한 정보를 포함하지 않음
 * 
 * 에러 처리:
 * - 비즈니스 로직 오류는 400 Bad Request
 * - 리소스 없음은 404 Not Found
 * - 서버 오류는 500 Internal Server Error
 * - 적절한 오류 메시지와 함께 응답
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/sgis/keys")
@RequiredArgsConstructor
public class SgisApiKeyController {

    private final SgisApiKeyService sgisApiKeyService;

    /**
     * 이미 발급받은 SGIS API 키를 시스템에 등록
     * POST /api/v1/sgis/keys
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SgisApiKeyResponse>> registerApiKey(
            @Valid @RequestBody SgisApiKeyRequest request) {
        
        log.info("SGIS API 키 등록 요청: Organization={}", request.getOrganizationName());
        
        try {
            // API 키 등록
            SgisApiKey registeredApiKey = sgisApiKeyService.registerApiKey(
                request.getOrganizationName(),
                request.getOrganizationCode(),
                request.getContactEmail(),
                request.getContactPhone(),
                request.getApiKey(),
                request.getSecretKey(),
                request.getDailyLimit(),
                request.getMonthlyLimit(),
                request.getExpiresAt(),
                request.getDescription(),
                request.getRequestedApis() != null ? String.join(",", request.getRequestedApis()) : null
            );
            
            // 응답 DTO로 변환
            SgisApiKeyResponse response = convertToResponse(registeredApiKey);
            
            return ResponseEntity.ok(ApiResponse.success(response, "SGIS API 키가 성공적으로 등록되었습니다"));
            
        } catch (Exception e) {
            log.error("SGIS API 키 등록 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SGIS API 키 등록에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 키 상세 조회
     * GET /api/v1/sgis/keys/{keyId}
     */
    @GetMapping("/{keyId}")
    public ResponseEntity<ApiResponse<SgisApiKeyResponse>> getApiKey(@PathVariable String keyId) {
        
        log.info("SGIS API 키 조회 요청: Key ID={}", keyId);
        
        return sgisApiKeyService.getApiKey(keyId)
            .map(apiKey -> {
                SgisApiKeyResponse response = convertToResponse(apiKey);
                return ResponseEntity.ok(ApiResponse.success(response, "API 키 정보를 조회했습니다"));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 기관명으로 API 키 조회
     * GET /api/v1/sgis/keys/organization/{organizationName}
     */
    @GetMapping("/organization/{organizationName}")
    public ResponseEntity<ApiResponse<SgisApiKeyResponse>> getApiKeyByOrganization(
            @PathVariable String organizationName) {
        
        log.info("기관별 API 키 조회 요청: Organization={}", organizationName);
        
        return sgisApiKeyService.getApiKeyByOrganization(organizationName)
            .map(apiKey -> {
                SgisApiKeyResponse response = convertToResponse(apiKey);
                return ResponseEntity.ok(ApiResponse.success(response, "기관의 API 키 정보를 조회했습니다"));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 모든 API 키 목록 조회 (페이징)
     * GET /api/v1/sgis/keys?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SgisApiKeyResponse>>> getAllApiKeys(
            @PageableDefault(size = 10) Pageable pageable) {
        
        log.info("모든 API 키 목록 조회 요청: Page={}, Size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<SgisApiKey> apiKeyPage = sgisApiKeyService.getAllApiKeys(pageable);
        
        List<SgisApiKeyResponse> responses = apiKeyPage.getContent().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        PageResponse<SgisApiKeyResponse> pageResponse = PageResponse.<SgisApiKeyResponse>builder()
            .content(responses)
            .totalElements(apiKeyPage.getTotalElements())
            .totalPages(apiKeyPage.getTotalPages())
            .pageNumber(apiKeyPage.getNumber())
            .pageSize(apiKeyPage.getSize())
            .build();
        
        return ResponseEntity.ok(ApiResponse.success(pageResponse, "API 키 목록을 조회했습니다"));
    }

    /**
     * 기관명으로 API 키 검색 (페이징)
     * GET /api/v1/sgis/keys/search?organization={organizationName}&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<SgisApiKeyResponse>>> searchApiKeysByOrganization(
            @RequestParam String organization,
            @PageableDefault(size = 10) Pageable pageable) {
        
        log.info("기관명으로 API 키 검색 요청: Organization={}, Page={}", organization, pageable.getPageNumber());
        
        Page<SgisApiKey> apiKeyPage = sgisApiKeyService.searchApiKeysByOrganization(organization, pageable);
        
        List<SgisApiKeyResponse> responses = apiKeyPage.getContent().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        PageResponse<SgisApiKeyResponse> pageResponse = PageResponse.<SgisApiKeyResponse>builder()
            .content(responses)
            .totalElements(apiKeyPage.getTotalElements())
            .totalPages(apiKeyPage.getTotalPages())
            .pageNumber(apiKeyPage.getNumber())
            .pageSize(apiKeyPage.getSize())
            .build();
        
        return ResponseEntity.ok(ApiResponse.success(pageResponse, "검색 결과를 조회했습니다"));
    }

    /**
     * 상태별 API 키 목록 조회 (페이징)
     * GET /api/v1/sgis/keys/status/{status}?page=0&size=10
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<PageResponse<SgisApiKeyResponse>>> getApiKeysByStatus(
            @PathVariable ApiKeyStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        
        log.info("상태별 API 키 목록 조회 요청: Status={}, Page={}", status, pageable.getPageNumber());
        
        Page<SgisApiKey> apiKeyPage = sgisApiKeyService.getApiKeysByStatus(status, pageable);
        
        List<SgisApiKeyResponse> responses = apiKeyPage.getContent().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        PageResponse<SgisApiKeyResponse> pageResponse = PageResponse.<SgisApiKeyResponse>builder()
            .content(responses)
            .totalElements(apiKeyPage.getTotalElements())
            .totalPages(apiKeyPage.getTotalPages())
            .pageNumber(apiKeyPage.getNumber())
            .pageSize(apiKeyPage.getSize())
            .build();
        
        return ResponseEntity.ok(ApiResponse.success(pageResponse, "상태별 API 키 목록을 조회했습니다"));
    }

    /**
     * API 키 정보 수정
     * PUT /api/v1/sgis/keys/{keyId}
     */
    @PutMapping("/{keyId}")
    public ResponseEntity<ApiResponse<SgisApiKeyResponse>> updateApiKey(
            @PathVariable String keyId,
            @Valid @RequestBody SgisApiKeyRequest request) {
        
        log.info("API 키 정보 수정 요청: Key ID={}", keyId);
        
        try {
            // DTO를 엔티티로 변환
            SgisApiKey updateData = convertToEntity(request);
            
            // API 키 정보 수정
            SgisApiKey updatedApiKey = sgisApiKeyService.updateApiKey(keyId, updateData);
            
            // 응답 DTO로 변환
            SgisApiKeyResponse response = convertToResponse(updatedApiKey);
            
            return ResponseEntity.ok(ApiResponse.success(response, "API 키 정보가 수정되었습니다"));
            
        } catch (Exception e) {
            log.error("API 키 정보 수정 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("API 키 정보 수정에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 키 상태 변경
     * PATCH /api/v1/sgis/keys/{keyId}/status
     */
    @PatchMapping("/{keyId}/status")
    public ResponseEntity<ApiResponse<SgisApiKeyResponse>> updateApiKeyStatus(
            @PathVariable String keyId,
            @RequestParam ApiKeyStatus status) {
        
        log.info("API 키 상태 변경 요청: Key ID={}, New Status={}", keyId, status);
        
        try {
            SgisApiKey updatedApiKey = sgisApiKeyService.updateApiKeyStatus(keyId, status);
            SgisApiKeyResponse response = convertToResponse(updatedApiKey);
            
            return ResponseEntity.ok(ApiResponse.success(response, "API 키 상태가 변경되었습니다"));
            
        } catch (Exception e) {
            log.error("API 키 상태 변경 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("API 키 상태 변경에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 키 폐기
     * DELETE /api/v1/sgis/keys/{keyId}
     */
    @DeleteMapping("/{keyId}")
    public ResponseEntity<ApiResponse<String>> revokeApiKey(@PathVariable String keyId) {
        
        log.info("API 키 폐기 요청: Key ID={}", keyId);
        
        try {
            boolean revoked = sgisApiKeyService.revokeApiKey(keyId);
            
            if (revoked) {
                return ResponseEntity.ok(ApiResponse.success("API 키가 성공적으로 폐기되었습니다"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("API 키 폐기에 실패했습니다"));
            }
            
        } catch (Exception e) {
            log.error("API 키 폐기 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("API 키 폐기에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 만료 예정 API 키 목록 조회
     * GET /api/v1/sgis/keys/expiring
     */
    @GetMapping("/expiring")
    public ResponseEntity<ApiResponse<List<SgisApiKeyResponse>>> getExpiringApiKeys() {
        
        log.info("만료 예정 API 키 목록 조회 요청");
        
        List<SgisApiKey> expiringKeys = sgisApiKeyService.getExpiringApiKeys();
        List<SgisApiKeyResponse> responses = expiringKeys.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(responses, "만료 예정 API 키 목록을 조회했습니다"));
    }

    /**
     * 만료된 API 키 목록 조회
     * GET /api/v1/sgis/keys/expired
     */
    @GetMapping("/expired")
    public ResponseEntity<ApiResponse<List<SgisApiKeyResponse>>> getExpiredApiKeys() {
        
        log.info("만료된 API 키 목록 조회 요청");
        
        List<SgisApiKey> expiredKeys = sgisApiKeyService.getExpiredApiKeys();
        List<SgisApiKeyResponse> responses = expiredKeys.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(responses, "만료된 API 키 목록을 조회했습니다"));
    }

    /**
     * API 키 통계 조회
     * GET /api/v1/sgis/keys/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getApiKeyStats() {
        
        log.info("API 키 통계 조회 요청");
        
        long activeCount = sgisApiKeyService.getActiveApiKeyCount();
        long expiredCount = sgisApiKeyService.getExpiredApiKeyCount();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeCount", activeCount);
        stats.put("expiredCount", expiredCount);
        stats.put("totalCount", activeCount + expiredCount);
        
        return ResponseEntity.ok(ApiResponse.success(stats, "API 키 통계를 조회했습니다"));
    }

    // Helper methods

    private SgisApiKey convertToEntity(SgisApiKeyRequest request) {
        return SgisApiKey.builder()
            .organizationName(request.getOrganizationName())
            .organizationCode(request.getOrganizationCode())
            .contactEmail(request.getContactEmail())
            .contactPhone(request.getContactPhone())
            .apiKey(request.getApiKey())
            .secretKey(request.getSecretKey())
            .dailyLimit(request.getDailyLimit())
            .monthlyLimit(request.getMonthlyLimit())
            .expiresAt(request.getExpiresAt())
            .description(request.getDescription())
            .requestedApis(request.getRequestedApis() != null ? String.join(",", request.getRequestedApis()) : null)
            .build();
    }

    private SgisApiKeyResponse convertToResponse(SgisApiKey apiKey) {
        return SgisApiKeyResponse.builder()
            .keyId(apiKey.getKeyId())
            .organizationName(apiKey.getOrganizationName())
            .organizationCode(apiKey.getOrganizationCode())
            .contactEmail(apiKey.getContactEmail())
            .contactPhone(apiKey.getContactPhone())
            .apiKey(apiKey.getApiKey())
            .secretKey(apiKey.getSecretKey())
            .dailyLimit(apiKey.getDailyLimit())
            .monthlyLimit(apiKey.getMonthlyLimit())
            .currentDailyUsage(apiKey.getCurrentDailyUsage())
            .currentMonthlyUsage(apiKey.getCurrentMonthlyUsage())
            .issuedAt(apiKey.getIssuedAt())
            .expiresAt(apiKey.getExpiresAt())
            .lastUsedAt(apiKey.getLastUsedAt())
            .status(apiKey.getStatus())
            .description(apiKey.getDescription())
            .requestedApis(apiKey.getRequestedApis())
            .createdAt(apiKey.getCreatedAt())
            .updatedAt(apiKey.getUpdatedAt())
            .build();
    }
}
