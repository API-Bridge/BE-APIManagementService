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
import org.example.APIManagementSvc.dto.externalapi.ApiStatisticsResponse;
import org.example.APIManagementSvc.repository.ApiParameterRepository;
import org.example.APIManagementSvc.repository.ExternalApiRepository;
import org.example.APIManagementSvc.service.ApiHealthCheckService;
import org.example.APIManagementSvc.service.ApiKeyService;
import org.example.APIManagementSvc.service.ApiParameterService;
import org.example.APIManagementSvc.service.ApiTokenRefreshService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Optional;
import org.example.APIManagementSvc.domain.Entity.ApiKey;
import org.example.APIManagementSvc.dto.externalapi.ExternalApiRegisterRequest;
import org.example.APIManagementSvc.dto.externalapi.ApiParameterRegisterRequest;
import java.util.stream.Collectors;

/**
 * API 메타데이터 통합 관리 서비스
 * ExternalApi와 ApiParameter를 함께 관리하며, ExternalApiService의 모든 기능을 포함합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiManagementService {

    private final ApiParameterService apiParameterService;
    private final ExternalApiRepository externalApiRepository;
    private final ApiParameterRepository apiParameterRepository;
    private final AiClassificationService aiClassificationService;
    private final ApiHealthCheckService apiHealthCheckService;
    private final ApiTokenRefreshService apiTokenRefreshService;
    private final ApiKeyService apiKeyService;

    // === ExternalApiService 기능 통합 ===

    /**
     * API 등록 (기본)
     */
    @Transactional
    public ExternalApi registerApi(ExternalApi api) {
        log.info("Registering API: {}", api.getApiName());
        
        // 입력값 검증
        validateApiInput(api);
        
        // 중복 확인
        if (externalApiRepository.existsByApiName(api.getApiName())) {
            throw new IllegalArgumentException("API with name '" + api.getApiName() + "' already exists");
        }
        
        // 기본값 설정
        api.setApiEffectiveness(true);
        api.setDeleted(false);
        api.setCreatedAt(LocalDateTime.now());
        api.setUpdatedAt(LocalDateTime.now());
        
        return externalApiRepository.save(api);
    }

    /**
     * API 등록 (인증 정보 포함)
     */
    @Transactional
    public ExternalApi registerApiWithAuth(ExternalApi api, String apiKey, String apiToken) {
        log.info("Registering API with auth: {}", api.getApiName());
        
        // 입력값 검증
        validateApiInput(api);
        
        // 중복 확인
        if (externalApiRepository.existsByApiName(api.getApiName())) {
            throw new IllegalArgumentException("API with name '" + api.getApiName() + "' already exists");
        }
        
        // 기본값 설정
        api.setApiEffectiveness(true);
        api.setDeleted(false);
        api.setCreatedAt(LocalDateTime.now());
        api.setUpdatedAt(LocalDateTime.now());
        
                    // API 키와 토큰 설정
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                // ApiKey 엔티티 조회 및 연결
                Optional<ApiKey> foundApiKey = apiKeyService.getApiKey(apiKey);
                if (foundApiKey.isPresent()) {
                    api.setApiKey(foundApiKey.get());
                } else {
                    log.warn("API Key not found: {}", apiKey);
                }
            }
        
        if (apiToken != null && !apiToken.trim().isEmpty()) {
            api.setApiToken(apiToken);
        }
        
        return externalApiRepository.save(api);
    }

    /**
     * API 입력값 검증
     */
    private void validateApiInput(ExternalApi api) {
        if (api.getApiName() == null || api.getApiName().trim().isEmpty()) {
            throw new IllegalArgumentException("API name is required");
        }
        
        if (api.getApiUrl() == null || api.getApiUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("API URL is required");
        }
        
        if (api.getHttpMethod() == null || api.getHttpMethod().trim().isEmpty()) {
            throw new IllegalArgumentException("HTTP method is required");
        }
        
        if (api.getApiDomain() == null) {
            throw new IllegalArgumentException("API domain is required");
        }
        
        if (api.getApiKeyword() == null) {
            throw new IllegalArgumentException("API keyword is required");
        }
        
        if (api.getApiIssuer() == null || api.getApiIssuer().trim().isEmpty()) {
            throw new IllegalArgumentException("API issuer is required");
        }
    }

    /**
     * 모든 활성 API 조회 (페이징)
     */
    public Page<ExternalApi> getApisWithPaging(Pageable pageable) {
        log.debug("Getting APIs with paging: {}", pageable);
        return externalApiRepository.findValidApis(pageable);
    }

    /**
     * API 검색 (이름, 설명, 제공기관으로 검색)
     */
    public List<ExternalApi> searchApis(String query) {
        log.debug("Searching APIs with query: {}", query);
        return externalApiRepository.findBySearchTermAndDeletedFalse(query);
    }

    /**
     * API 통계 조회
     */
    public ApiStatisticsResponse getApiStatistics() {
        log.debug("Getting API statistics");
        
        long totalApis = externalApiRepository.countByDeletedFalse();
        long activeApis = externalApiRepository.countByApiEffectivenessTrueAndDeletedFalse();
        long inactiveApis = totalApis - activeApis;
        
        return ApiStatisticsResponse.builder()
                .totalApis(totalApis)
                .activeApis(activeApis)
                .inactiveApis(inactiveApis)
                .build();
    }

    /**
     * API ID로 조회
     */
    public Optional<ExternalApi> getApiById(String apiId) {
        log.debug("Getting API by ID: {}", apiId);
        return externalApiRepository.findByApiId(apiId)
                .filter(api -> !api.getDeleted());
    }

    /**
     * API 이름으로 조회
     */
    public Optional<ExternalApi> getApiByName(String apiName) {
        log.debug("Getting API by name: {}", apiName);
        return externalApiRepository.findByApiName(apiName)
                .filter(api -> !api.getDeleted());
    }

    /**
     * 활성 API 목록 조회 (삭제되지 않은 API)
     */
    public List<ExternalApi> getAllActiveApis() {
        log.debug("Getting all active APIs");
        return externalApiRepository.findByDeletedFalse();
    }

    /**
     * 중요 API 목록 조회 (높은 우선순위를 가진 API들)
     */
    public List<ExternalApi> getImportantApis() {
        log.debug("Getting important APIs");
        // 중요 API 기준: 정부/공공 도메인, 금융 도메인, 또는 높은 사용 빈도를 가진 API
        List<ExternalApi> allActiveApis = getAllActiveApis();
        return allActiveApis.stream()
            .filter(api -> api.getApiDomain() == ApiDomain.GOVERNMENT || 
                          api.getApiDomain() == ApiDomain.FINANCE || 
                          api.getApiDomain() == ApiDomain.TRANSPORTATION)
            .toList();
    }

    /**
     * 모든 API 목록 조회 (삭제된 API 포함)
     */
    public List<ExternalApi> getAllApis() {
        log.debug("Getting all APIs (including deleted ones)");
        return externalApiRepository.findAll();
    }

    /**
     * API 수정 (기본)
     */
    @Transactional
    public ExternalApi updateApi(String apiId, ExternalApi updateData) {
        log.info("Updating API: {}", apiId);
        
        ExternalApi existingApi = externalApiRepository.findByApiId(apiId)
                .filter(api -> !api.getDeleted())
                .orElseThrow(() -> new IllegalArgumentException("API not found: " + apiId));

        // 업데이트 가능한 필드들 수정
        if (updateData.getApiName() != null) {
            existingApi.setApiName(updateData.getApiName());
        }
        if (updateData.getApiDescription() != null) {
            existingApi.setApiDescription(updateData.getApiDescription());
        }
        if (updateData.getApiUrl() != null) {
            existingApi.setApiUrl(updateData.getApiUrl());
        }
        if (updateData.getHttpMethod() != null) {
            existingApi.setHttpMethod(updateData.getHttpMethod());
        }
        if (updateData.getApiDomain() != null) {
            existingApi.setApiDomain(updateData.getApiDomain());
        }
        if (updateData.getApiKeyword() != null) {
            existingApi.setApiKeyword(updateData.getApiKeyword());
        }
        if (updateData.getApiIssuer() != null) {
            existingApi.setApiIssuer(updateData.getApiIssuer());
        }
        if (updateData.getApiEffectiveness() != null) {
            existingApi.setApiEffectiveness(updateData.getApiEffectiveness());
        }

        existingApi.setUpdatedAt(LocalDateTime.now());
        
        return externalApiRepository.save(existingApi);
    }





    /**
     * API 유효성 업데이트
     */
    @Transactional
    public ExternalApi updateApiEffectiveness(String apiId, boolean effectiveness) {
        log.info("Updating API effectiveness: {} to {}", apiId, effectiveness);
        
        ExternalApi api = externalApiRepository.findByApiId(apiId)
                .filter(a -> !a.getDeleted())
                .orElseThrow(() -> new IllegalArgumentException("API not found: " + apiId));

        api.setApiEffectiveness(effectiveness);
        api.setUpdatedAt(LocalDateTime.now());
        
        return externalApiRepository.save(api);
    }

    // === 기존 ApiManagementService 기능 ===

    /**
     * ExternalApiRegisterRequest를 받아서 API와 파라미터를 함께 등록
     */
    @Transactional
    public ExternalApi registerApiWithParametersAndAuth(ExternalApiRegisterRequest request) {
        log.info("Registering API from request: {}", request.getApiName());
        
        try {
            // 1. DTO를 Entity로 변환
            ExternalApi api = convertToEntity(request);
            List<ApiParameter> parameters = convertToParameters(request.getParameters());
            
            // 2. 기존 메서드 호출
            return registerApiWithParametersAndAuth(api, parameters, null, request.getApiToken());
            
        } catch (Exception e) {
            log.error("Failed to register API from request: {}", e.getMessage(), e);
            throw new RuntimeException("API registration from request failed: " + e.getMessage(), e);
        }
    }

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
                try {
                    var classification = aiClassificationService.classifyApi(api.getApiId(), api.getApiName(), api.getApiDescription(), api.getApiUrl(), null);
                    if (classification != null) {
                        if (api.getApiDomain() == null && classification.getClassifiedDomain() != null) {
                            api.setApiDomain(classification.getClassifiedDomain());
                        }
                        if (api.getApiKeyword() == null && classification.getClassifiedKeyword() != null) {
                            api.setApiKeyword(classification.getClassifiedKeyword());
                        }
                    }
                } catch (Exception e) {
                    log.warn("AI 분류 실패, 기본값 사용: {} - {}", api.getApiName(), e.getMessage());
                    // AI 분류 실패 시 기본값 설정
                    if (api.getApiDomain() == null) {
                        api.setApiDomain(ApiDomain.OTHERS);
                    }
                    if (api.getApiKeyword() == null) {
                        api.setApiKeyword(ApiKeyword.API_DOCUMENT);
                    }
                }
            }
            
            // 3. API 등록 (인증 정보 포함)
            ExternalApi registeredApi = registerApiWithAuth(api, apiKey, apiToken);
            
            // 4. 파라미터 등록
            if (parameters != null && !parameters.isEmpty()) {
                for (ApiParameter parameter : parameters) {
                    parameter.setApiId(registeredApi.getApiId());
                    apiParameterService.saveParameter(parameter);
                }
            }
            
            // 6. 초기 헬스체크는 스케줄러에서 자동으로 수행됨
            
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
                try {
                    var classification = aiClassificationService.classifyApi(api.getApiId(), api.getApiName(), api.getApiDescription(), api.getApiUrl(), null);
                    if (classification != null) {
                        if (api.getApiDomain() == null && classification.getClassifiedDomain() != null) {
                            api.setApiDomain(classification.getClassifiedDomain());
                        }
                        if (api.getApiKeyword() == null && classification.getClassifiedKeyword() != null) {
                            api.setApiKeyword(classification.getClassifiedKeyword());
                        }
                    }
                } catch (Exception e) {
                    log.warn("AI 분류 실패, 기본값 사용: {} - {}", api.getApiName(), e.getMessage());
                    // AI 분류 실패 시 기본값 설정
                    if (api.getApiDomain() == null) {
                        api.setApiDomain(ApiDomain.OTHERS);
                    }
                    if (api.getApiKeyword() == null) {
                        api.setApiKeyword(ApiKeyword.API_DOCUMENT);
                    }
                }
            }
            
            // 2. API 등록
            ExternalApi registeredApi = registerApi(api);
            
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

    // === DTO 변환 헬퍼 메서드들 ===

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
        api.setAutoTokenRefresh(request.getAutoTokenRefresh());
        
        return api;
    }

    /**
     * ApiParameterRegisterRequest 리스트를 ApiParameter Entity 리스트로 변환
     */
    private List<ApiParameter> convertToParameters(List<ApiParameterRegisterRequest> parameterRequests) {
        if (parameterRequests == null || parameterRequests.isEmpty()) {
            return new ArrayList<>();
        }
        
        return parameterRequests.stream()
            .map(this::convertToParameter)
            .collect(Collectors.toList());
    }

    /**
     * ApiParameterRegisterRequest를 ApiParameter Entity로 변환
     */
    private ApiParameter convertToParameter(ApiParameterRegisterRequest request) {
        ApiParameter parameter = new ApiParameter();
        parameter.setParamName(request.getParamName());
        parameter.setParamType(request.getParamType());
        parameter.setParamDescription(request.getDescription());
        parameter.setIsRequired(request.getIsRequired());
        parameter.setDefaultValue(request.getDefaultValue());
        parameter.setDeleted(false);
        parameter.setCreatedAt(LocalDateTime.now());
        parameter.setUpdatedAt(LocalDateTime.now());
        
        return parameter;
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
        
        ExternalApi api = getApiById(apiId)
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
            ExternalApi existingApi = externalApiRepository.findByApiId(apiId)
                    .filter(api -> !api.getDeleted())
                    .orElseThrow(() -> new IllegalArgumentException("API not found: " + apiId));

            // 업데이트 가능한 필드들 수정
            if (updateData.getApiName() != null) {
                existingApi.setApiName(updateData.getApiName());
            }
            if (updateData.getApiDescription() != null) {
                existingApi.setApiDescription(updateData.getApiDescription());
            }
            if (updateData.getApiUrl() != null) {
                existingApi.setApiUrl(updateData.getApiUrl());
            }
            if (updateData.getHttpMethod() != null) {
                existingApi.setHttpMethod(updateData.getHttpMethod());
            }
            if (updateData.getApiIssuer() != null) {
                existingApi.setApiIssuer(updateData.getApiIssuer());
            }

            existingApi.setUpdatedAt(LocalDateTime.now());
            ExternalApi updatedApi = externalApiRepository.save(existingApi);

            // 2. 파라미터 수정
            if (parameters != null && !parameters.isEmpty()) {
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
     * API와 파라미터를 함께 삭제
     */
    @Transactional
    public void deleteApiWithParameters(String apiId) {
        log.info("Deleting API with parameters: {}", apiId);
        
        try {
            // 1. API 삭제 (소프트 삭제)
            deleteApi(apiId);
            
            // 2. 파라미터 삭제 (소프트 삭제)
            List<ApiParameter> parameters = apiParameterService.getParametersByApiId(apiId);
            for (ApiParameter parameter : parameters) {
                apiParameterService.deleteParameter(parameter.getParameterId());
            }
            
            log.info("Successfully deleted API and {} parameters", parameters.size());
            
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
            ExternalApi api = getApiById(apiId)
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
    public ExternalApi copyApi(String originalApiId) {
        return copyApi(originalApiId, "복사본_" + System.currentTimeMillis());
    }

    /**
     * API 복사 (새로운 API 생성)
     */
    @Transactional
    public ExternalApi copyApi(String originalApiId, String newApiName) {
        log.info("Copying API: {} to {}", originalApiId, newApiName);
        
        try {
            // 1. 원본 API 조회
            ExternalApi originalApi = getApiById(originalApiId)
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
            
            ExternalApi copiedApi = registerApi(newApi);
            
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
     * API 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteApi(String apiId) {
        log.info("Deleting API: {}", apiId);
        
        try {
            // 1. 파라미터 삭제
            List<ApiParameter> parameters = apiParameterService.getParametersByApiId(apiId);
            for (ApiParameter parameter : parameters) {
                apiParameterService.deleteParameter(parameter.getParameterId());
            }
            
            // 2. API 삭제
            deleteApi(apiId);
            
            log.info("Successfully deleted API: {}", apiId);
            
        } catch (Exception e) {
            log.error("Failed to delete API: {}", e.getMessage(), e);
            throw new RuntimeException("API deletion failed: " + e.getMessage(), e);
        }
    }

    /**
     * API 하드 삭제 (완전 삭제)
     */
    @Transactional
    public void hardDeleteApi(String apiId) {
        log.info("Hard deleting API: {}", apiId);
        
        try {
            // 1. 파라미터 하드 삭제 (개별 파라미터를 하나씩 하드 삭제)
            List<ApiParameter> parameters = apiParameterService.getParametersByApiId(apiId);
            for (ApiParameter parameter : parameters) {
                apiParameterService.hardDeleteParameter(parameter.getParameterId());
            }
            
            // 2. API 하드 삭제
            hardDeleteApi(apiId);
            
            log.info("Successfully hard deleted API: {}", apiId);
            
        } catch (Exception e) {
            log.error("Failed to hard delete API: {}", e.getMessage(), e);
            throw new RuntimeException("API hard deletion failed: " + e.getMessage(), e);
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
            apis = searchApis(searchTerm);
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
            ExternalApi api = getApiById(apiId)
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
            ExternalApi api = getApiById(apiId)
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
