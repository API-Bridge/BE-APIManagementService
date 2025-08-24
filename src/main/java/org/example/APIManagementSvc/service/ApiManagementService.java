package org.example.APIManagementSvc.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ApiParameter;
import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import org.example.APIManagementSvc.dto.externalapi.ExternalApiUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * API 메타데이터 통합 관리 서비스
 * ExternalApi와 ApiParameter를 함께 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiManagementService {

    private final ExternalApiService externalApiService;
    private final ApiParameterService apiParameterService;
    private final AiClassificationService aiClassificationService;

    /**
     * API와 파라미터를 함께 등록
     */
    @Transactional
    public ExternalApi registerApiWithParameters(ExternalApi api, List<ApiParameter> parameters) {
        log.info("Registering API with parameters: {}", api.getApiName());
        
        try {
            // 1. AI 자동 분류 수행
            if (api.getApiDomain() == null || api.getApiKeyword() == null) {
                var classification = aiClassificationService.classifyApi(api.getApiId(), api.getApiName(), api.getApiDescription(), api.getApiUrl(), null);
                if (api.getApiDomain() == null) {
                    api.setApiDomain(classification.getClassifiedDomain());
                }
                if (api.getApiKeyword() == null) {
                    api.setApiKeyword(classification.getClassifiedKeyword());
                }
            }
            
            // 2. API 등록
            ExternalApi registeredApi = externalApiService.registerApi(api);
            
            // 3. 파라미터 등록
            if (parameters != null && !parameters.isEmpty()) {
                for (ApiParameter parameter : parameters) {
                    parameter.setApiId(registeredApi.getApiId());
                    apiParameterService.saveParameter(parameter);
                }
            }
            
            log.info("Successfully registered API with {} parameters", parameters != null ? parameters.size() : 0);
            return registeredApi;
            
        } catch (Exception e) {
            log.error("Failed to register API with parameters: {}", e.getMessage(), e);
            throw new RuntimeException("API registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * 🔥 AI 분류를 위한 프롬프트 생성
     */
    private String buildClassificationPrompt(ExternalApi api, List<ApiParameter> parameters) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 API 정보를 분석하여 도메인과 키워드를 분류해주세요:\n\n");
        prompt.append("API 이름: ").append(api.getApiName()).append("\n");
        prompt.append("API 설명: ").append(api.getApiDescription()).append("\n");
        prompt.append("API URL: ").append(api.getApiUrl()).append("\n");
        prompt.append("HTTP 메소드: ").append(api.getHttpMethod()).append("\n");
        prompt.append("파라미터 개수: ").append(parameters != null ? parameters.size() : 0).append("\n");
        
        // 파라미터 정보 추가
        if (parameters != null && !parameters.isEmpty()) {
            prompt.append("주요 파라미터:\n");
            parameters.stream()
                .limit(5) // 최대 5개만 표시
                .forEach(param -> {
                    prompt.append("- ").append(param.getParamName())
                        .append(" (").append(param.getParamType()).append(")");
                    if (param.getParamDescription() != null && !param.getParamDescription().isEmpty()) {
                        prompt.append(": ").append(param.getParamDescription());
                    }
                    prompt.append("\n");
                });
        }
        
        prompt.append("\n위 정보를 바탕으로 가장 적절한 도메인과 키워드를 선택해주세요.");
        return prompt.toString();
    }

    /**
     * API와 파라미터 함께 조회
     */
    public ApiWithParameters getApiWithParameters(String apiId) {
        log.debug("Getting API with parameters: {}", apiId);
        
        ExternalApi api = externalApiService.getApiById(apiId)
                .orElseThrow(() -> new IllegalArgumentException("API not found: " + apiId));
        
        List<ApiParameter> parameters = apiParameterService.getParametersByApiId(apiId);
        
        return ApiWithParameters.builder()
                .api(api)
                .parameters(parameters)
                .build();
    }

    /**
     * API와 파라미터 함께 수정
     */
    @Transactional
    public ExternalApi updateApiWithParameters(String apiId, ExternalApiUpdateRequest updateData, List<ApiParameter> parameters) {
        log.info("Updating API with parameters: {}", apiId);
        
        try {
            // 1. API 수정
            ExternalApi updateEntity = new ExternalApi();
            updateEntity.setApiName(updateData.getApiName());
            updateEntity.setApiDescription(updateData.getApiDescription());
            updateEntity.setApiUrl(updateData.getApiUrl());
            updateEntity.setHttpMethod(updateData.getHttpMethod());
            updateEntity.setApiIssuer(updateData.getApiIssuer());
            updateEntity.setApiEffectiveness(updateData.getApiEffectiveness());
                    
            ExternalApi updatedApi = externalApiService.updateApi(apiId, updateEntity);
            
            // 2. 기존 파라미터 삭제 후 새 파라미터 추가
            if (parameters != null) {
                apiParameterService.deleteAllParametersByApiId(apiId);
                
                for (ApiParameter parameter : parameters) {
                    parameter.setApiId(apiId);
                    apiParameterService.saveParameter(parameter);
                }
            }
            
            log.info("Successfully updated API with {} parameters", parameters != null ? parameters.size() : 0);
            return updatedApi;
            
        } catch (Exception e) {
            log.error("Failed to update API with parameters: {}", e.getMessage(), e);
            throw new RuntimeException("API update failed: " + e.getMessage(), e);
        }
    }

    /**
     * API와 파라미터 함께 삭제
     */
    @Transactional
    public void deleteApiWithParameters(String apiId) {
        log.info("Deleting API with parameters: {}", apiId);
        
        try {
            // 1. 파라미터 삭제
            apiParameterService.deleteAllParametersByApiId(apiId);
            
            // 2. API 삭제
            externalApiService.deleteApi(apiId);
            
            log.info("Successfully deleted API with parameters: {}", apiId);
            
        } catch (Exception e) {
            log.error("Failed to delete API with parameters: {}", e.getMessage(), e);
            throw new RuntimeException("API deletion failed: " + e.getMessage(), e);
        }
    }

    /**
     * API 유효성 검증
     */
    public ApiValidationResult validateApi(String apiId) {
        log.debug("Validating API: {}", apiId);
        
        try {
            ExternalApi api = externalApiService.getApiById(apiId)
                    .orElseThrow(() -> new IllegalArgumentException("API not found: " + apiId));
            
            List<ApiParameter> parameters = apiParameterService.getParametersByApiId(apiId);
            
            // 기본적인 유효성 검증
            boolean isValid = true;
            StringBuilder errorMessage = new StringBuilder();
            
            // API 필수 필드 검증
            if (api.getApiUrl() == null || api.getApiUrl().trim().isEmpty()) {
                isValid = false;
                errorMessage.append("API URL is required. ");
            }
            
            if (api.getHttpMethod() == null || api.getHttpMethod().trim().isEmpty()) {
                isValid = false;
                errorMessage.append("HTTP Method is required. ");
            }
            
            // TODO: 실제 API 엔드포인트 헬스체크 추가
            // 여기서 실제 API에 요청을 보내 유효성을 확인할 수 있습니다.
            
            return ApiValidationResult.builder()
                    .isValid(isValid)
                    .errorMessage(errorMessage.toString().trim())
                    .api(api)
                    .parameters(parameters)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to validate API: {}", e.getMessage(), e);
            return ApiValidationResult.builder()
                    .isValid(false)
                    .errorMessage("Validation failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 도메인과 키워드 조합으로 API와 파라미터 조회
     */
    public List<ApiWithParameters> getApisWithParametersByDomainAndKeyword(String domain, String keyword) {
        log.debug("Getting APIs with parameters by domain: {} and keyword: {}", domain, keyword);
        
        ApiDomain apiDomain = ApiDomain.valueOf(domain.toUpperCase());
        ApiKeyword apiKeyword = ApiKeyword.valueOf(keyword.toUpperCase());
        
        // 도메인과 키워드 모두 일치하는 API 조회
        List<ExternalApi> apis = externalApiService.getApisByDomainAndKeyword(apiDomain, apiKeyword);
        
        return apis.stream()
                .map(api -> {
                    List<ApiParameter> parameters = apiParameterService.getParametersByApiId(api.getApiId());
                    return ApiWithParameters.builder()
                            .api(api)
                            .parameters(parameters)
                            .build();
                })
                .toList();
    }

    /**
     * 벌크 검색: 다중 도메인과 키워드로 API 검색
     */
    public BulkSearchResult performBulkSearch(List<String> domainCodes, List<String> keywordCodes) {
        log.info("Performing bulk search: domains={}, keywords={}", domainCodes, keywordCodes);
        
        Map<String, List<ApiWithParameters>> results = new HashMap<>();
        int totalCount = 0;
        int matchedCombinations = 0;
        
        // 각 도메인별로 처리
        for (String domainCode : domainCodes) {
            try {
                // 대소문자 구분 없이 도메인 찾기
                ApiDomain domain = findDomainByCode(domainCode);
                if (domain == null) {
                    log.warn("Invalid domain code: {}", domainCode);
                    continue;
                }
                
                // 해당 도메인의 키워드들만 필터링
                for (String keywordCode : keywordCodes) {
                    try {
                        // 대소문자 구분 없이 키워드 찾기
                        ApiKeyword keyword = findKeywordByCode(keywordCode);
                        if (keyword == null || !keyword.getDomain().equals(domain)) {
                            continue; // 도메인과 매칭되지 않는 키워드는 스킵
                        }
                        
                        // 도메인-키워드 조합으로 검색
                        List<ApiWithParameters> apisWithParameters = 
                                getApisWithParametersByDomainAndKeyword(domain.name(), keyword.name());
                        
                        if (!apisWithParameters.isEmpty()) {
                            String key = domain.getDisplayName() + " - " + keyword.getDisplayName();
                            results.put(key, apisWithParameters);
                            totalCount += apisWithParameters.size();
                            matchedCombinations++;
                            
                            log.debug("Found {} APIs for {}+{}", apisWithParameters.size(), domain, keyword);
                        }
                        
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid keyword code: {}", keywordCode);
                    }
                }
                
            } catch (IllegalArgumentException e) {
                log.warn("Invalid domain code: {}", domainCode);
            }
        }
        
        // 검색 요약 정보 생성
        BulkSearchSummary summary = BulkSearchSummary.builder()
                .requestedDomains(domainCodes.size())
                .requestedKeywords(keywordCodes.size())
                .matchedCombinations(matchedCombinations)
                .totalApis(totalCount)
                .build();
        
        return BulkSearchResult.builder()
                .totalCount(totalCount)
                .summary(summary)
                .results(results)
                .build();
    }

    /**
     * 대소문자 구분 없이 도메인 찾기 (코드 또는 enum name으로)
     */
    private ApiDomain findDomainByCode(String code) {
        if (code == null) return null;
        
        // 1. 소문자 코드로 검색 (예: "government")
        ApiDomain domain = ApiDomain.fromCode(code.toLowerCase());
        if (domain != null && domain != ApiDomain.OTHERS) {
            return domain;
        }
        
        // 2. enum name으로 검색 (예: "GOVERNMENT")
        try {
            return ApiDomain.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 대소문자 구분 없이 키워드 찾기 (코드 또는 enum name으로)
     */
    private ApiKeyword findKeywordByCode(String code) {
        if (code == null) return null;
        
        // 1. 소문자 코드로 검색 (예: "api_document")
        ApiKeyword keyword = ApiKeyword.fromCode(code.toLowerCase());
        if (keyword != null) {
            return keyword;
        }
        
        // 2. enum name으로 검색 (예: "API_DOCUMENT")
        try {
            return ApiKeyword.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Inner Classes for DTOs
    @Getter
    @Builder
    public static class ApiWithParameters {
        private ExternalApi api;
        private List<ApiParameter> parameters;
    }

    @Getter
    @Builder
    public static class ApiValidationResult {
        private boolean isValid;
        private String errorMessage;
        private ExternalApi api;
        private List<ApiParameter> parameters;
    }

    @Getter
    @Builder
    public static class BulkSearchResult {
        private int totalCount;
        private BulkSearchSummary summary;
        private Map<String, List<ApiWithParameters>> results;
    }

    @Getter
    @Builder
    public static class BulkSearchSummary {
        private int requestedDomains;
        private int requestedKeywords;
        private int matchedCombinations;
        private int totalApis;
    }
}
