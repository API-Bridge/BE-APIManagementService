package org.example.APIManagementSvc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.annotation.RateLimit;
import org.example.APIManagementSvc.domain.Entity.ApiParameter;
import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.dto.common.PageResponse;
import org.example.APIManagementSvc.dto.externalapi.*;
import org.example.APIManagementSvc.service.ApiManagementService;
import org.example.APIManagementSvc.service.ExternalApiService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * External API 관리 Controller
 * 프로덕션용 REST API 엔드포인트 제공
 * 
 * Rate Limiting 정책:
 * - API 등록/수정/삭제/복사: 1시간에 최대 50회 (관리자 작업)
 * - API 조회/탐색: 1시간에 최대 1000회 (일반 사용자)
 * - API 통계: 1시간에 최대 500회 (관리자/분석가)
 * - API 유효성 검증: 1시간에 최대 200회 (개발자 도구)
 * 
 * API 탐색 구조:
 * - 전체 목록 조회: 페이징을 지원하는 모든 API 목록
 * - 도메인별 조회: 업무 영역별 체계적 탐색 (금융/날씨/뉴스/교통 등)
 * - 키워드별 조회: 세부 기능별 세밀한 분류 (주가/환율/날씨예보/지하철정보 등)
 * - 상세 조회: 특정 API의 완전한 정보와 파라미터
 */
@Slf4j
@RestController
@RequestMapping("/external-apis")
@RequiredArgsConstructor
public class ExternalApiController {

    private final ExternalApiService externalApiService;
    private final ApiManagementService apiManagementService;

    /**
     * API 상세 조회 (ID 기반)
     * Rate Limit: 1시간에 최대 1000회 (일반 사용자)
     * GET /api/v1/external-apis/detail/{apiId}
     */
    @GetMapping("/detail/{apiId}")
    @RateLimit(value = 1000, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<Object>> getApiDetailById(@PathVariable String apiId) {
        log.info("Getting API detail by ID: {}", apiId);
        
        try {
            // API와 파라미터 함께 조회
            Object response = apiManagementService.getApiWithParameters(apiId);
            
            return ResponseEntity.ok(ApiResponse.success(response, "API 상세정보를 성공적으로 조회했습니다."));
            
        } catch (IllegalArgumentException e) {
            log.warn("API not found: {}", apiId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("API를 찾을 수 없습니다: " + apiId));
        } catch (Exception e) {
            log.error("Failed to get API detail: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 상세 조회 (이름 기반)
     * Rate Limit: 1시간에 최대 1000회 (일반 사용자)
     * GET /api/v1/external-apis/detail/name/{apiName}
     */
    @GetMapping("/detail/name/{apiName}")
    @RateLimit(value = 1000, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<Object>> getApiDetailByName(@PathVariable String apiName) {
        log.info("Getting API detail by name: {}", apiName);
        
        try {
            // API 이름으로 조회
            Optional<ExternalApi> apiOpt = externalApiService.getApiByName(apiName);
            if (apiOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("API를 찾을 수 없습니다: " + apiName));
            }
            
            // API와 파라미터 함께 조회
            Object response = apiManagementService.getApiWithParameters(apiOpt.get().getApiId());
            
            return ResponseEntity.ok(ApiResponse.success(response, "API 상세정보를 성공적으로 조회했습니다."));
            
        } catch (Exception e) {
            log.error("Failed to get API detail by name: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 목록 조회 (페이징)
     * Rate Limit: 1시간에 최대 1000회 (일반 사용자)
     * GET /api/v1/external-apis/list
     */
    @GetMapping("/list")
    @RateLimit(value = 1000, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<PageResponse<ExternalApiResponse>>> getApiList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        log.info("Getting API list: page={}, size={}, sort={}, direction={}", page, size, sort, direction);
        
        try {
            // 페이징 객체 생성 (정렬은 기본적으로 최신순으로 고정)
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            
            // API 목록 조회
            Page<ExternalApi> apiPage = externalApiService.getApisWithPaging(pageable);
            
            // Entity를 Response DTO로 변환
            List<ExternalApiResponse> responses = apiPage.getContent().stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            PageResponse<ExternalApiResponse> pageResponse = PageResponse.<ExternalApiResponse>builder()
                    .content(responses)
                    .pageNumber(apiPage.getNumber())
                    .pageSize(apiPage.getSize())
                    .totalElements(apiPage.getTotalElements())
                    .totalPages(apiPage.getTotalPages())
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(pageResponse, "API 목록을 성공적으로 조회했습니다."));
            
        } catch (Exception e) {
            log.error("Failed to fetch APIs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 검색 (통합 검색)
     * 도메인, 키워드, 검색어를 조합하여 API를 검색합니다.
     * Rate Limit: 1시간에 최대 1000회 (일반 사용자)
     * GET /api/v1/external-apis/search
     */
    @GetMapping("/search")
    @RateLimit(value = 1000, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<List<ApiManagementService.ApiWithParameters>>> searchApis(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String searchTerm) {
        
        log.info("Searching APIs with domain: {}, keyword: {}, searchTerm: {}", domain, keyword, searchTerm);
        
        try {
            // 검색 조건이 하나도 없는 경우 에러
            if ((domain == null || domain.trim().isEmpty()) && 
                (keyword == null || keyword.trim().isEmpty()) && 
                (searchTerm == null || searchTerm.trim().isEmpty())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("검색 조건을 하나 이상 입력해주세요. (domain, keyword, searchTerm 중 하나)"));
            }
            
            // 통합 검색 수행
            var searchResults = apiManagementService.searchApisWithParameters(domain, keyword, searchTerm);
            
            String message = String.format("검색 결과 %d개를 찾았습니다.", searchResults.size());
            if (domain != null) message += " (도메인: " + domain + ")";
            if (keyword != null) message += " (키워드: " + keyword + ")";
            if (searchTerm != null) message += " (검색어: " + searchTerm + ")";
            
            return ResponseEntity.ok(ApiResponse.success(searchResults, message));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid search parameters: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("잘못된 검색 파라미터입니다: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to search APIs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 검색에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 등록
     * Rate Limit: 1시간에 최대 50회 (관리자 작업)
     * POST /api/v1/external-apis/register
     */
    @PostMapping("/register")
    @RateLimit(value = 50, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<ExternalApiResponse>> registerApi(
            @Valid @RequestBody ExternalApiRegisterRequest request) {
        
        log.info("Registering new API: {}", request.getApiName());
        
        try {
            // API 등록 (파라미터와 인증 정보 포함)
            ExternalApi registeredApi = apiManagementService.registerApiWithParametersAndAuth(request);
            
            // Entity를 Response DTO로 변환
            ExternalApiResponse response = convertToResponse(registeredApi);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "API가 성공적으로 등록되었습니다."));
                    
        } catch (Exception e) {
            log.error("Failed to register API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 등록에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 수정
     * Rate Limit: 1시간에 최대 50회 (관리자 작업)
     * PUT /api/v1/external-apis/update/{apiId}
     */
    @PutMapping("/update/{apiId}")
    @RateLimit(value = 50, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<ExternalApiResponse>> updateApi(
            @PathVariable String apiId,
            @Valid @RequestBody ExternalApiUpdateRequest request) {
        
        log.info("Updating API: {}", apiId);
        
        try {
            // 파라미터 변환
            List<ApiParameter> parameters = request.getParameters() != null ? 
                request.getParameters().stream()
                    .map(param -> convertToParameterEntity(param, apiId))
                    .collect(Collectors.toList()) : null;
            
            // API 수정 (파라미터 포함)
            ExternalApi updatedApi = apiManagementService.updateApiWithParameters(apiId, request, parameters);
            
            // Entity를 Response DTO로 변환
            ExternalApiResponse response = convertToResponse(updatedApi);
            
            return ResponseEntity.ok(ApiResponse.success(response, "API가 성공적으로 수정되었습니다."));
            
        } catch (IllegalArgumentException e) {
            log.warn("API not found: {}", apiId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("API를 찾을 수 없습니다: " + apiId));
        } catch (Exception e) {
            log.error("Failed to update API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 수정에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 삭제 (소프트 삭제)
     * Rate Limit: 1시간에 최대 50회 (관리자 작업)
     * DELETE /api/v1/external-apis/delete/{apiId}
     */
    @DeleteMapping("/delete/{apiId}")
    @RateLimit(value = 50, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<String>> deleteApi(@PathVariable String apiId) {
        log.info("Deleting API: {}", apiId);
        
        try {
            apiManagementService.deleteApi(apiId);
            return ResponseEntity.ok(ApiResponse.success("API가 성공적으로 삭제되었습니다.", "API가 성공적으로 삭제되었습니다."));
            
        } catch (IllegalArgumentException e) {
            log.warn("API not found: {}", apiId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("API를 찾을 수 없습니다: " + apiId));
        } catch (Exception e) {
            log.error("Failed to delete API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 삭제에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 하드 삭제 (완전 삭제)
     * Rate Limit: 1시간에 최대 10회 (관리자 작업)
     * DELETE /api/v1/external-apis/delete/{apiId}/hard
     */
    @DeleteMapping("/delete/{apiId}/hard")
    @RateLimit(value = 10, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<String>> hardDeleteApi(@PathVariable String apiId) {
        log.info("Hard deleting API: {}", apiId);
        
        try {
            apiManagementService.hardDeleteApi(apiId);
            return ResponseEntity.ok(ApiResponse.success("API가 완전히 삭제되었습니다.", "API가 완전히 삭제되었습니다."));
            
        } catch (IllegalArgumentException e) {
            log.warn("API not found: {}", apiId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("API를 찾을 수 없습니다: " + apiId));
        } catch (Exception e) {
            log.error("Failed to hard delete API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 완전 삭제에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 효과성 업데이트
     * Rate Limit: 1시간에 최대 100회 (관리자 작업)
     * PATCH /api/v1/external-apis/update/{apiId}/effectiveness
     */
    @PatchMapping("/update/{apiId}/effectiveness")
    @RateLimit(value = 100, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<ExternalApiResponse>> updateApiEffectiveness(
            @PathVariable String apiId,
            @RequestBody Map<String, Boolean> request) {
        
        log.info("Updating API effectiveness: {} to {}", apiId, request.get("apiEffectiveness"));
        
        try {
            Boolean apiEffectiveness = request.get("apiEffectiveness");
            if (apiEffectiveness == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("apiEffectiveness 값이 필요합니다."));
            }
            
            ExternalApi updatedApi = externalApiService.updateApiEffectiveness(apiId, apiEffectiveness);
            ExternalApiResponse response = convertToResponse(updatedApi);
            
            return ResponseEntity.ok(ApiResponse.success(response, "API 효과성이 성공적으로 업데이트되었습니다."));
            
        } catch (IllegalArgumentException e) {
            log.warn("API not found: {}", apiId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("API를 찾을 수 없습니다: " + apiId));
        } catch (Exception e) {
            log.error("Failed to update API effectiveness: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 효과성 업데이트에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 키 연결
     * Rate Limit: 1시간에 최대 50회 (관리자 작업)
     * POST /api/v1/external-apis/link/{apiId}/api-key
     */
    @PostMapping("/link/{apiId}/api-key")
    @RateLimit(value = 50, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<String>> linkApiKey(
            @PathVariable String apiId,
            @RequestBody Map<String, String> request) {
        
        log.info("Linking API key to API: {}", apiId);
        
        try {
            String apiKeyId = request.get("apiKeyId");
            if (apiKeyId == null || apiKeyId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("apiKeyId 값이 필요합니다."));
            }
            
            apiManagementService.linkApiKey(apiId, apiKeyId);
            return ResponseEntity.ok(ApiResponse.success("API 키가 성공적으로 연결되었습니다.", "API 키가 성공적으로 연결되었습니다."));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("잘못된 요청입니다: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to link API key: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 키 연결에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 키 연결 해제
     * Rate Limit: 1시간에 최대 50회 (관리자 작업)
     * DELETE /api/v1/external-apis/unlink/{apiId}/api-key
     */
    @DeleteMapping("/unlink/{apiId}/api-key")
    @RateLimit(value = 50, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<String>> unlinkApiKey(@PathVariable String apiId) {
        log.info("Unlinking API key from API: {}", apiId);
        
        try {
            apiManagementService.unlinkApiKey(apiId);
            return ResponseEntity.ok(ApiResponse.success("API 키 연결이 해제되었습니다.", "API 키 연결이 해제되었습니다."));
            
        } catch (IllegalArgumentException e) {
            log.warn("API not found: {}", apiId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("API를 찾을 수 없습니다: " + apiId));
        } catch (Exception e) {
            log.error("Failed to unlink API key: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 키 연결 해제에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 복사
     * Rate Limit: 1시간에 최대 20회 (관리자 작업)
     * POST /api/v1/external-apis/copy/{apiId}
     */
    @PostMapping("/copy/{apiId}")
    @RateLimit(value = 20, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<ExternalApiResponse>> copyApi(@PathVariable String apiId) {
        log.info("Copying API: {}", apiId);
        
        try {
            ExternalApi copiedApi = apiManagementService.copyApi(apiId);
            ExternalApiResponse response = convertToResponse(copiedApi);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "API가 성공적으로 복사되었습니다."));
                    
        } catch (IllegalArgumentException e) {
            log.warn("API not found: {}", apiId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("API를 찾을 수 없습니다: " + apiId));
        } catch (Exception e) {
            log.error("Failed to copy API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 복사에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 유효성 검증
     * Rate Limit: 1시간에 최대 100회 (관리자 작업)
     * POST /api/v1/external-apis/validate/{apiId}
     */
    @PostMapping("/validate/{apiId}")
    @RateLimit(value = 100, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<Object>> validateApi(@PathVariable String apiId) {
        log.info("Validating API: {}", apiId);
        
        try {
            Object validationResult = apiManagementService.validateApi(apiId);
            return ResponseEntity.ok(ApiResponse.success(validationResult, "API 유효성 검증이 완료되었습니다."));
            
        } catch (IllegalArgumentException e) {
            log.warn("API not found: {}", apiId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("API를 찾을 수 없습니다: " + apiId));
        } catch (Exception e) {
            log.error("Failed to validate API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 유효성 검증에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 통계 조회
     * Rate Limit: 1시간에 최대 1000회 (일반 사용자)
     * GET /api/v1/external-apis/statistics
     */
    @GetMapping("/statistics")
    @RateLimit(value = 1000, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<ApiStatisticsResponse>> getApiStatistics() {
        log.info("Getting API statistics");
        
        try {
            ApiStatisticsResponse statistics = externalApiService.getApiStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics, "API 통계를 성공적으로 조회했습니다."));
            
        } catch (Exception e) {
            log.error("Failed to get API statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 통계 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 활성 API 목록 조회
     * Rate Limit: 1시간에 최대 1000회 (일반 사용자)
     * GET /api/v1/external-apis/active
     */
    @GetMapping("/active")
    @RateLimit(value = 1000, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<List<ExternalApiResponse>>> getActiveApis() {
        log.info("Getting active APIs");
        
        try {
            List<ExternalApi> activeApis = externalApiService.getAllActiveApis();
            List<ExternalApiResponse> responses = activeApis.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(responses, 
                String.format("활성 API %d개를 조회했습니다.", responses.size())));
            
        } catch (Exception e) {
            log.error("Failed to get active APIs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("활성 API 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    // === Helper Methods ===

    /**
     * ExternalApiRegisterRequest를 ExternalApi Entity로 변환
     */
    private ExternalApi convertToEntity(ExternalApiRegisterRequest request) {
        ExternalApi api = new ExternalApi();
        api.setApiName(request.getApiName());
        api.setApiUrl(request.getApiUrl());
        api.setApiIssuer(request.getApiIssuer());
        api.setApiOwner(request.getApiOwner());
        api.setApiDomain(request.getApiDomain());
        api.setApiKeyword(request.getApiKeyword());
        api.setHttpMethod(request.getHttpMethod());
        api.setApiDescription(request.getApiDescription());
        api.setApiEffectiveness(true);
        api.setDeleted(false);
        
        // 토큰 관련 설정
        if (request.getApiToken() != null && !request.getApiToken().trim().isEmpty()) {
            api.setApiToken(request.getApiToken().trim());
        }
        
        // 자동 토큰 갱신 설정
        if (request.getAutoTokenRefresh() != null) {
            api.setAutoTokenRefresh(request.getAutoTokenRefresh());
        }
        
        return api;
    }

    /**
     * ApiParameterRegisterRequest를 ApiParameter Entity로 변환
     */
    private ApiParameter convertToParameterEntity(ApiParameterRegisterRequest request, String apiId) {
        ApiParameter parameter = new ApiParameter();
        parameter.setApiId(apiId);
        parameter.setParamName(request.getParamName());
        parameter.setParamType(request.getParamType());
        parameter.setIsRequired(request.getIsRequired());
        parameter.setDefaultValue(request.getDefaultValue());
        parameter.setDeleted(false);
        return parameter;
    }

    /**
     * ExternalApi Entity를 ExternalApiResponse DTO로 변환
     */
    private ExternalApiResponse convertToResponse(ExternalApi api) {
        return ExternalApiResponse.builder()
                .apiId(api.getApiId())
                .apiName(api.getApiName())
                .apiUrl(api.getApiUrl())
                .apiIssuer(api.getApiIssuer())
                .apiOwner(api.getApiOwner())
                .apiDomain(api.getApiDomain())
                .apiKeyword(api.getApiKeyword())
                .httpMethod(api.getHttpMethod())
                .apiDescription(api.getApiDescription())
                .apiEffectiveness(api.getApiEffectiveness())
                .createdAt(api.getCreatedAt())
                .updatedAt(api.getUpdatedAt())
                .deleted(api.getDeleted())
                .build();
    }
}
