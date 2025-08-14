package org.example.APIManagementSvc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

/**
 * External API 관리 Controller
 * 프로덕션용 REST API 엔드포인트 제공
 */
@Slf4j
@RestController
@RequestMapping("/external-apis")
@RequiredArgsConstructor
public class ExternalApiController {

    private final ExternalApiService externalApiService;
    private final ApiManagementService apiManagementService;

    /**
     * 새로운 External API 등록
     * @param request API 등록 요청 데이터
     * @return 등록된 API 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ExternalApiResponse>> registerApi(
            @Valid @RequestBody ExternalApiRegisterRequest request) {
        log.info("Registering new API: {}", request.getApiName());
        
        try {
            // DTO를 Entity로 변환
            ExternalApi api = convertToEntity(request);
            
            // 파라미터 변환
            List<ApiParameter> parameters = request.getParameters() != null ? 
                request.getParameters().stream()
                    .map(param -> convertToParameterEntity(param, api.getApiId()))
                    .collect(Collectors.toList()) : null;
            
            // API와 파라미터 함께 등록
            ExternalApi registeredApi = apiManagementService.registerApiWithParameters(api, parameters);
            
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
     * API 상세 조회
     * GET /api/v1/external-apis/{apiId}
     */
    @GetMapping("/{apiId}")
    public ResponseEntity<ApiResponse<Object>> getApiDetail(@PathVariable String apiId) {
        log.info("Getting API detail: {}", apiId);
        
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
     * API 목록 조회 (페이징)
     * GET /api/v1/external-apis
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExternalApiResponse>>> getApis(
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
     * API 수정
     * PUT /api/v1/external-apis/{apiId}
     */
    @PutMapping("/{apiId}")
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
     * API 삭제
     * DELETE /api/v1/external-apis/{apiId}
     */
    @DeleteMapping("/{apiId}")
    public ResponseEntity<ApiResponse<Void>> deleteApi(@PathVariable String apiId) {
        log.info("Deleting API: {}", apiId);
        
        try {
            // API와 파라미터 함께 삭제
            apiManagementService.deleteApiWithParameters(apiId);
            
            return ResponseEntity.ok(ApiResponse.success(null, "API가 성공적으로 삭제되었습니다."));
            
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
     * API 검색
     * GET /api/v1/external-apis/search
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ExternalApiResponse>>> searchApis(
            @RequestParam String q) {
        
        log.info("Searching APIs with query: {}", q);
        
        try {
            // API 검색
            List<ExternalApi> apis = externalApiService.searchApis(q);
            
            // Entity를 Response DTO로 변환
            List<ExternalApiResponse> responses = apis.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(responses, 
                    String.format("검색 결과 %d개의 API를 찾았습니다.", responses.size())));
                    
        } catch (Exception e) {
            log.error("Failed to search APIs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 검색에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 도메인별 API 조회
     * GET /api/v1/external-apis/domain/{domain}
     */
    @GetMapping("/domain/{domain}")
    public ResponseEntity<ApiResponse<List<ExternalApiResponse>>> getApisByDomain(
            @PathVariable ApiDomain domain) {
        
        log.info("Getting APIs by domain: {}", domain);
        
        try {
            // 도메인별 API 조회
            List<ExternalApi> apis = externalApiService.getApisByDomain(domain);
            
            // Entity를 Response DTO로 변환
            List<ExternalApiResponse> responses = apis.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(responses, 
                    String.format("%s 도메인에서 %d개의 API를 찾았습니다.", domain, responses.size())));
                    
        } catch (Exception e) {
            log.error("Failed to get APIs by domain: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("도메인별 API 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 키워드별 API 조회
     * GET /api/v1/external-apis/keyword/{keyword}
     */
    @GetMapping("/keyword/{keyword}")
    public ResponseEntity<ApiResponse<List<ExternalApiResponse>>> getApisByKeyword(
            @PathVariable ApiKeyword keyword) {
        
        log.info("Getting APIs by keyword: {}", keyword);
        
        try {
            // 키워드별 API 조회
            List<ExternalApi> apis = externalApiService.getApisByKeyword(keyword);
            
            // Entity를 Response DTO로 변환
            List<ExternalApiResponse> responses = apis.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(responses, 
                    String.format("%s 키워드에서 %d개의 API를 찾았습니다.", keyword, responses.size())));
                    
        } catch (Exception e) {
            log.error("Failed to get APIs by keyword: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("키워드별 API 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 통계 조회
     * GET /api/v1/external-apis/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<ApiStatisticsResponse>> getApiStatistics() {
        log.info("Getting API statistics");
        
        try {
            var statistics = externalApiService.getApiStatistics();
            
            return ResponseEntity.ok(ApiResponse.success(statistics, "API 통계를 성공적으로 조회했습니다."));
            
        } catch (Exception e) {
            log.error("Failed to get API statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 통계 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 유효성 검증
     * POST /api/v1/external-apis/{apiId}/validate
     */
    @PostMapping("/{apiId}/validate")
    public ResponseEntity<ApiResponse<Object>> validateApi(@PathVariable String apiId) {
        log.info("Validating API: {}", apiId);
        
        try {
            var validationResult = apiManagementService.validateApi(apiId);
            
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
     * API 복사
     * POST /api/v1/external-apis/{apiId}/copy
     */
    @PostMapping("/{apiId}/copy")
    public ResponseEntity<ApiResponse<ExternalApiResponse>> copyApi(
            @PathVariable String apiId,
            @RequestParam String newName) {
        
        log.info("Copying API: {} to {}", apiId, newName);
        
        try {
            ExternalApi copiedApi = apiManagementService.copyApi(apiId, newName);
            
            // Entity를 Response DTO로 변환
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
