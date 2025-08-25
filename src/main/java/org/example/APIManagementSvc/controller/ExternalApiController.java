package org.example.APIManagementSvc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.annotation.RateLimit;
import org.example.APIManagementSvc.domain.Entity.ApiParameter;
import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.dto.common.PageResponse;
import org.example.APIManagementSvc.dto.externalapi.*;
import org.example.APIManagementSvc.service.ApiManagementService;
import org.example.APIManagementSvc.service.ApiParameterService;
import org.example.APIManagementSvc.service.ApiManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * External API 관리 Controller
 * 핵심 API 관리 기능만 제공하는 간소화된 컨트롤러
 * 
 * Rate Limiting 정책:
 * - API 등록/수정/삭제: 1시간에 최대 50회 (관리자 작업)
 * - API 조회/탐색: 1시간에 최대 1000회 (일반 사용자)
 */
@Slf4j
@RestController
@RequestMapping("/external-apis")
@RequiredArgsConstructor
public class ExternalApiController {

    private final ApiManagementService apiManagementService;
    private final ApiParameterService apiParameterService;

    /**
     * API 상세 조회 (ID 기반)
     * Rate Limit: 1시간에 최대 1000회 (일반 사용자)
     * GET /external-apis/detail/{apiId}
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
     * GET /external-apis/detail/name/{apiName}
     */
    @GetMapping("/detail/name/{apiName}")
    @RateLimit(value = 1000, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<Object>> getApiDetailByName(@PathVariable String apiName) {
        log.info("Getting API detail by name: {}", apiName);
        
        try {
            // API 이름으로 조회
            Optional<ExternalApi> apiOpt = apiManagementService.getApiByName(apiName);
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
     * GET /external-apis/list
     */
    @GetMapping("/list")
    @RateLimit(value = 1000, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<PageResponse<ExternalApiResponse>>> getApiList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.info("Getting API list: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<ExternalApi> apiPage = apiManagementService.getApisWithPaging(pageable);
            
            List<ExternalApiResponse> responses = apiPage.getContent().stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            PageResponse<ExternalApiResponse> pageResponse = new PageResponse<>(
                responses, page, size, apiPage.getTotalElements(), apiPage.getTotalPages()
            );
            
            return ResponseEntity.ok(ApiResponse.success(pageResponse, 
                String.format("API 목록 %d개를 조회했습니다.", responses.size())));
            
        } catch (Exception e) {
            log.error("Failed to get API list: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 검색
     * Rate Limit: 1시간에 최대 1000회 (일반 사용자)
     * GET /external-apis/search
     */
    @GetMapping("/search")
    @RateLimit(value = 1000, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<List<ApiManagementService.ApiWithParameters>>> searchApis(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String searchTerm) {
        
        log.info("Searching APIs: domain={}, keyword={}, searchTerm={}", domain, keyword, searchTerm);
        
        try {
            if (domain == null && keyword == null && searchTerm == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("검색 조건을 하나 이상 입력해주세요."));
            }
            
            var searchResults = apiManagementService.searchApisWithParameters(domain, keyword, searchTerm);
            
            return ResponseEntity.ok(ApiResponse.success(searchResults, 
                String.format("검색 결과 %d개를 찾았습니다.", searchResults.size())));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid search parameters: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("검색 조건이 올바르지 않습니다: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to search APIs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 검색에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 등록
     * Rate Limit: 1시간에 최대 50회 (관리자 작업)
     * POST /external-apis/register
     */
    @PostMapping("/register")
    @RateLimit(value = 50, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<ExternalApiResponse>> registerApi(
            @Valid @RequestBody ExternalApiRegisterRequest request) {
        
        log.info("Registering new API: {}", request.getApiName());
        
        try {
            ExternalApi registeredApi = apiManagementService.registerApiWithParametersAndAuth(request);
            ExternalApiResponse response = convertToResponse(registeredApi);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "API가 성공적으로 등록되었습니다."));
                    
        } catch (IllegalArgumentException e) {
            log.warn("Invalid API registration request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("API 등록 요청이 올바르지 않습니다: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to register API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 등록에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 수정
     * Rate Limit: 1시간에 최대 50회 (관리자 작업)
     * PUT /external-apis/update/{apiId}
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
            
            ExternalApi updatedApi = apiManagementService.updateApiWithParameters(apiId, request, parameters);
            ExternalApiResponse response = convertToResponse(updatedApi);
            
            return ResponseEntity.ok(ApiResponse.success(response, "API가 성공적으로 수정되었습니다."));
            
        } catch (IllegalArgumentException e) {
            log.warn("API not found or invalid update request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("API를 찾을 수 없거나 수정 요청이 올바르지 않습니다: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("API 수정에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 삭제 (Soft Delete)
     * Rate Limit: 1시간에 최대 50회 (관리자 작업)
     * DELETE /external-apis/delete/{apiId}
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
     * API의 모든 파라미터 조회
     * Rate Limit: 1시간에 최대 1000회 (일반 사용자)
     * GET /external-apis/{apiId}/parameters
     */
    @GetMapping("/{apiId}/parameters")
    @RateLimit(value = 1000, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<List<ApiParameterResponse>>> getParameters(@PathVariable String apiId) {
        log.info("Fetching all parameters for API: {}", apiId);
        
        try {
            List<ApiParameter> parameters = apiParameterService.getParametersByApiId(apiId);
            
            List<ApiParameterResponse> responses = parameters.stream()
                    .map(this::convertToParameterResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(responses, 
                String.format("API 파라미터 %d개를 조회했습니다.", responses.size())));
            
        } catch (Exception e) {
            log.error("Failed to fetch parameters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파라미터 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 파라미터 수정 또는 추가
     * Rate Limit: 1시간에 최대 50회 (관리자 작업)
     * PUT /external-apis/{apiId}/parameters/{parameterId}
     * 
     * 기존 파라미터가 있으면 수정, 없으면 새로 추가
     */
    @PutMapping("/{apiId}/parameters/{parameterId}")
    @RateLimit(value = 50, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<ApiParameterResponse>> updateOrCreateParameter(
            @PathVariable String apiId,
            @PathVariable String parameterId,
            @Valid @RequestBody ApiParameterRegisterRequest request) {
        
        log.info("Updating or creating parameter: {} for API: {}", parameterId, apiId);
        
        try {
            // 기존 파라미터 조회
            var existingParameter = apiParameterService.getParameterById(parameterId);
            
            ApiParameter resultParameter;
            
            if (existingParameter.isPresent()) {
                // 기존 파라미터가 있으면 수정
                log.info("Updating existing parameter: {}", parameterId);
                
                // API ID 검증
                if (!apiId.equals(existingParameter.get().getApiId())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("API ID가 일치하지 않습니다."));
                }
                
                // 파라미터 수정
                ApiParameter updateData = convertToParameterEntity(request, apiId);
                resultParameter = apiParameterService.updateParameter(parameterId, updateData);
                
            } else {
                // 기존 파라미터가 없으면 새로 생성
                log.info("Creating new parameter with ID: {}", parameterId);
                
                // 파라미터 생성
                ApiParameter newParameter = convertToParameterEntity(request, apiId);
                newParameter.setParameterId(parameterId);
                resultParameter = apiParameterService.saveParameter(newParameter);
            }
            
            ApiParameterResponse response = convertToParameterResponse(resultParameter);
            String message = existingParameter.isPresent() ? "파라미터가 성공적으로 수정되었습니다." : "파라미터가 성공적으로 추가되었습니다.";
            
            return ResponseEntity.ok(ApiResponse.success(response, message));
            
        } catch (Exception e) {
            log.error("Failed to update or create parameter: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파라미터 수정/추가에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 다중 도메인과 키워드로 벌크 검색
     * POST 방식으로 복잡한 검색 조건을 받아서 처리
     * Rate Limit: 1시간에 최대 500회 (벌크 검색은 제한적)
     * POST /external-apis/bulk-search
     */
    @PostMapping("/bulk-search")
    @RateLimit(value = 500, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<BulkSearchResponse>> bulkSearch(
            @Valid @RequestBody BulkSearchRequest request) {
        
        log.info("Bulk search request: domains={}, keywords={}", request.getDomains(), request.getKeywords());
        
        try {
            // 서비스 레이어에서 벌크 검색 수행
            ApiManagementService.BulkSearchResult result = apiManagementService
                    .performBulkSearch(request.getDomains(), request.getKeywords());
            
            // 서비스 결과를 컨트롤러 응답 DTO로 변환
            Map<String, List<ApiWithParametersResponse>> responseResults = new HashMap<>();
            
            result.getResults().forEach((key, apisWithParameters) -> {
                List<ApiWithParametersResponse> responses = apisWithParameters.stream()
                        .map(this::convertToApiWithParametersResponse)
                        .collect(Collectors.toList());
                responseResults.put(key, responses);
            });
            
            // 응답 구성
            BulkSearchResponse.SearchSummary summary = BulkSearchResponse.SearchSummary.builder()
                    .requestedDomains(result.getSummary().getRequestedDomains())
                    .requestedKeywords(result.getSummary().getRequestedKeywords())
                    .matchedCombinations(result.getSummary().getMatchedCombinations())
                    .totalApis(result.getSummary().getTotalApis())
                    .build();
            
            BulkSearchResponse response = BulkSearchResponse.builder()
                    .totalCount(result.getTotalCount())
                    .summary(summary)
                    .results(responseResults)
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(response, 
                    String.format("벌크 검색 완료: %d개 조합에서 총 %d개의 API를 찾았습니다.", 
                    result.getSummary().getMatchedCombinations(), result.getTotalCount())));
                    
        } catch (Exception e) {
            log.error("Failed to perform bulk search: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("벌크 검색에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 파라미터 삭제 (Soft Delete)
     * Rate Limit: 1시간에 최대 50회 (관리자 작업)
     * DELETE /external-apis/{apiId}/parameters/{parameterId}
     */
    @DeleteMapping("/{apiId}/parameters/{parameterId}")
    @RateLimit(value = 50, timeUnit = TimeUnit.HOURS, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<String>> deleteParameter(
            @PathVariable String apiId,
            @PathVariable String parameterId) {
        
        log.info("Deleting parameter: {} for API: {}", parameterId, apiId);
        
        try {
            // 기존 파라미터 조회
            var existingParameter = apiParameterService.getParameterById(parameterId);
            
            if (existingParameter.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("파라미터를 찾을 수 없습니다."));
            }
            
            // API ID 검증
            if (!apiId.equals(existingParameter.get().getApiId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("API ID가 일치하지 않습니다."));
            }
            
            // 파라미터 삭제
            apiParameterService.deleteParameter(parameterId);
            
            return ResponseEntity.ok(ApiResponse.success("파라미터가 성공적으로 삭제되었습니다.", "파라미터가 성공적으로 삭제되었습니다."));
            
        } catch (Exception e) {
            log.error("Failed to delete parameter: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파라미터 삭제에 실패했습니다: " + e.getMessage()));
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

    /**
     * ApiParameter Entity를 ApiParameterResponse DTO로 변환
     */
    private ApiParameterResponse convertToParameterResponse(ApiParameter parameter) {
        return ApiParameterResponse.builder()
                .parameterId(parameter.getParameterId())
                .apiId(parameter.getApiId())
                .paramName(parameter.getParamName())
                .paramType(parameter.getParamType())
                .isRequired(parameter.getIsRequired())
                .defaultValue(parameter.getDefaultValue())
                .createdAt(parameter.getCreatedAt())
                .updatedAt(parameter.getUpdatedAt())
                .build();
    }

    /**
     * ApiWithParameters를 ApiWithParametersResponse로 변환
     */
    private ApiWithParametersResponse convertToApiWithParametersResponse(ApiManagementService.ApiWithParameters apiWithParameters) {
        // 파라미터를 간소화된 버전으로 변환
        List<ApiParameterSimpleResponse> simpleParameters = apiWithParameters.getParameters().stream()
                .map(param -> ApiParameterSimpleResponse.builder()
                        .apiId(param.getApiId())
                        .paramName(param.getParamName())
                        .paramType(param.getParamType())
                        .isRequired(param.getIsRequired())
                        .defaultValue(param.getDefaultValue())
                        .paramDescription(param.getParamDescription())
                        .build())
                .collect(Collectors.toList());

        // ExternalApi Entity를 간소화된 DTO로 변환
        ExternalApiSimpleResponse simpleApi = ExternalApiSimpleResponse.builder()
                .apiId(apiWithParameters.getApi().getApiId())
                .apiName(apiWithParameters.getApi().getApiName())
                .apiUrl(apiWithParameters.getApi().getApiUrl())
                .apiIssuer(apiWithParameters.getApi().getApiIssuer())
                .apiOwner(apiWithParameters.getApi().getApiOwner())
                .apiDomain(apiWithParameters.getApi().getApiDomain())
                .apiKeyword(apiWithParameters.getApi().getApiKeyword())
                .httpMethod(apiWithParameters.getApi().getHttpMethod())
                .apiDescription(apiWithParameters.getApi().getApiDescription())
                .apiEffectiveness(apiWithParameters.getApi().getApiEffectiveness())
                .createdAt(apiWithParameters.getApi().getCreatedAt())
                .updatedAt(apiWithParameters.getApi().getUpdatedAt())
                .build();

        return ApiWithParametersResponse.builder()
                .api(simpleApi)
                .parameters(simpleParameters)
                .build();
    }
}
