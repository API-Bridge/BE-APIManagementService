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
import org.example.APIManagementSvc.repository.ApiParameterRepository;
import org.example.APIManagementSvc.repository.ExternalApiRepository;
import org.example.APIManagementSvc.service.ApiHealthCheckService;
import org.example.APIManagementSvc.service.ApiKeyService;
import org.example.APIManagementSvc.service.ApiParameterService;
import org.example.APIManagementSvc.service.ApiTokenRefreshService;
import org.example.APIManagementSvc.service.ExternalApiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import org.example.APIManagementSvc.domain.Entity.ApiKey;

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
    private final ExternalApiRepository externalApiRepository;
    private final ApiParameterRepository apiParameterRepository;
    private final AiClassificationService aiClassificationService;
    private final ApiHealthCheckService apiHealthCheckService;
    private final ApiTokenRefreshService apiTokenRefreshService;
    private final ApiKeyService apiKeyService;

    /**
     * API와 파라미터를 함께 등록 (인증 정보 포함)
     */
    @Transactional
    public ExternalApi registerApiWithParametersAndAuth(ExternalApi api, List<ApiParameter> parameters, 
                                                      String apiKey, String apiToken) {
        log.info("Registering API with parameters and auth: {}", api.getApiName());
        
        try {
            // 1. API ID 설정 (AI 분류를 위해 필요)
            if (api.getApiId() == null || api.getApiId().trim().isEmpty()) {
                String apiId = "api_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                api.setApiId(apiId);
                log.info("API ID 자동 생성: {}", apiId);
            }
            
            // 2. AI 자동 분류 수행 (도메인이나 키워드가 설정되지 않은 경우)
            if (api.getApiDomain() == null || api.getApiKeyword() == null) {
                var classification = aiClassificationService.classifyApi(api.getApiId(), api.getApiName(), api.getApiDescription(), api.getApiUrl(), null);
                if (api.getApiDomain() == null) {
                    api.setApiDomain(classification.getClassifiedDomain());
                }
                if (api.getApiKeyword() == null) {
                    api.setApiKeyword(classification.getClassifiedKeyword());
                }
            }
            
            // 3. 토큰 만료 시간 설정 (토큰이 제공된 경우)
            if (apiToken != null && !apiToken.trim().isEmpty()) {
                // 토큰 만료 시간을 4시간 후로 설정
                LocalDateTime tokenExpiresAt = LocalDateTime.now().plusHours(4);
                api.setTokenExpiresAt(tokenExpiresAt);
                log.info("API 토큰 설정 완료 - 만료 시간: {}", tokenExpiresAt);
            }
            
            // 4. API 등록 (인증 정보 포함)
            ExternalApi registeredApi = externalApiService.registerApiWithAuth(api, apiKey, apiToken);
            
            // 5. 파라미터 등록
            if (parameters != null && !parameters.isEmpty()) {
                for (ApiParameter parameter : parameters) {
                    parameter.setApiId(registeredApi.getApiId());
                    apiParameterService.saveParameter(parameter);
                }
            }
            
            // 6. 초기 헬스체크 수행
            try {
                apiHealthCheckService.checkApiHealth(registeredApi);
            } catch (Exception e) {
                log.warn("초기 헬스체크 실패: {} - {}", api.getApiName(), e.getMessage());
            }
            
            log.info("Successfully registered API with {} parameters and auth", parameters != null ? parameters.size() : 0);
            return registeredApi;
            
        } catch (Exception e) {
            log.error("Failed to register API with parameters and auth: {}", e.getMessage(), e);
            throw new RuntimeException("API registration failed: " + e.getMessage(), e);
        }
    }

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
    public Object getApiWithParameters(String apiId) {
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
     * API 복사 (새로운 API 생성)
     */
    @Transactional
    public ExternalApi copyApi(String originalApiId, String newApiName) {
        log.info("Copying API: {} to {}", originalApiId, newApiName);
        
        try {
            // 1. 원본 API 조회
            ExternalApi originalApi = externalApiService.getApiById(originalApiId)
                    .orElseThrow(() -> new IllegalArgumentException("Original API not found: " + originalApiId));
            
            // 2. 새 API 생성
            ExternalApi newApi = new ExternalApi();
            newApi.setApiId(UUID.randomUUID().toString());
            newApi.setApiName(newApiName);
            newApi.setApiDescription(originalApi.getApiDescription() + " (복사본)");
            newApi.setApiUrl(originalApi.getApiUrl());
            newApi.setHttpMethod(originalApi.getHttpMethod());
            newApi.setApiDomain(originalApi.getApiDomain());
            newApi.setApiKeyword(originalApi.getApiKeyword());
            newApi.setApiIssuer(originalApi.getApiIssuer());
            newApi.setApiEffectiveness(originalApi.getApiEffectiveness());
            newApi.setDeleted(false);
            newApi.setCreatedAt(LocalDateTime.now());
            newApi.setUpdatedAt(LocalDateTime.now());
            
            ExternalApi copiedApi = externalApiService.registerApi(newApi);
            
            // 3. 파라미터 복사
            List<ApiParameter> originalParameters = apiParameterService.getParametersByApiId(originalApiId);
            for (ApiParameter originalParam : originalParameters) {
                ApiParameter newParam = new ApiParameter();
                newParam.setParameterId(UUID.randomUUID().toString());
                newParam.setApiId(copiedApi.getApiId());
                newParam.setParamName(originalParam.getParamName());
                newParam.setParamType(originalParam.getParamType());
                newParam.setIsRequired(originalParam.getIsRequired());
                newParam.setDefaultValue(originalParam.getDefaultValue());
                // ApiParameter 엔티티에는 description과 example 필드가 없으므로 제거
                newParam.setCreatedAt(LocalDateTime.now());
                newParam.setUpdatedAt(LocalDateTime.now());
                
                apiParameterService.saveParameter(newParam);
            }
            
            log.info("Successfully copied API with {} parameters", originalParameters.size());
            return copiedApi;
            
        } catch (Exception e) {
            log.error("Failed to copy API: {}", e.getMessage(), e);
            throw new RuntimeException("API copy failed: " + e.getMessage(), e);
        }
    }

    /**
     * 복합 검색: 도메인, 키워드, 검색어를 조합하여 API 검색
     */
    public List<ApiWithParameters> searchApisWithParameters(String domain, String keyword, String searchTerm) {
        log.debug("Searching APIs with domain: {}, keyword: {}, searchTerm: {}", domain, keyword, searchTerm);
        
        List<ExternalApi> apis = new ArrayList<>();
        
        if (domain != null && keyword != null) {
            // 도메인 + 키워드 조합 검색
            ApiDomain apiDomain = ApiDomain.valueOf(domain.toUpperCase());
            ApiKeyword apiKeyword = ApiKeyword.valueOf(keyword.toUpperCase());
            apis = externalApiRepository.findByApiDomainAndApiKeywordAndDeletedFalse(apiDomain, apiKeyword);
        } else if (domain != null) {
            // 도메인만으로 검색
            ApiDomain apiDomain = ApiDomain.valueOf(domain.toUpperCase());
            apis = externalApiRepository.findByApiDomainAndDeletedFalse(apiDomain);
        } else if (keyword != null) {
            // 키워드만으로 검색
            ApiKeyword apiKeyword = ApiKeyword.valueOf(keyword.toUpperCase());
            apis = externalApiRepository.findByApiKeywordAndDeletedFalse(apiKeyword);
        } else {
            // 검색어만으로 검색 (전체 API에서 검색)
            apis = externalApiService.searchApis(searchTerm);
        }
        
        // 검색어가 있는 경우 추가 필터링
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            apis = apis.stream()
                    .filter(api -> 
                        api.getApiName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                        api.getApiDescription().toLowerCase().contains(searchTerm.toLowerCase())
                    )
                    .toList();
        }
        
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
     * API 키와 External API 연결
     */
    @Transactional
    public ExternalApi linkApiKey(String apiId, String apiKeyId) {
        log.info("API 키 연결: API={}, API Key={}", apiId, apiKeyId);
        
        try {
            // API 존재 여부 확인
            ExternalApi api = externalApiService.getApiById(apiId)
                    .orElseThrow(() -> new IllegalArgumentException("API not found: " + apiId));
            
            // API 키 존재 여부 확인
            ApiKey apiKey = apiKeyService.getApiKey(apiKeyId)
                    .orElseThrow(() -> new IllegalArgumentException("API key not found: " + apiKeyId));
            
            // API 키가 활성 상태인지 확인
            if (!apiKey.isActive()) {
                throw new IllegalArgumentException("API key is not active: " + apiKeyId);
            }
            
            // API와 API 키 연결
            api.setApiKey(apiKey);
            ExternalApi updatedApi = externalApiRepository.save(api);
            
            log.info("API 키 연결 완료: {} -> {}", apiId, apiKeyId);
            return updatedApi;
            
        } catch (Exception e) {
            log.error("API 키 연결 실패: {} - {}", apiId, e.getMessage(), e);
            throw new RuntimeException("API 키 연결 실패: " + e.getMessage());
        }
    }

    /**
     * API 키 연결 해제
     */
    @Transactional
    public ExternalApi unlinkApiKey(String apiId) {
        log.info("API 키 연결 해제: API={}", apiId);
        
        try {
            ExternalApi api = externalApiService.getApiById(apiId)
                    .orElseThrow(() -> new IllegalArgumentException("API not found: " + apiId));
            
            api.setApiKey(null);
            ExternalApi updatedApi = externalApiRepository.save(api);
            
            log.info("API 키 연결 해제 완료: {}", apiId);
            return updatedApi;
            
        } catch (Exception e) {
            log.error("API 키 연결 해제 실패: {} - {}", apiId, e.getMessage(), e);
            throw new RuntimeException("API 키 연결 해제 실패: " + e.getMessage());
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
}
