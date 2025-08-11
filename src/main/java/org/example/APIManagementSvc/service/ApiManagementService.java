package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.ExternalApi;
import org.example.APIManagementSvc.domain.ApiParameter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    /**
     * API와 파라미터를 함께 생성
     */
    @Transactional
    public ExternalApi createApiWithParameters(ExternalApi api, List<ApiParameter> parameters) {
        log.info("Creating API with {} parameters: {}", 
                parameters != null ? parameters.size() : 0, api.getApiName());
        
        // API 생성
        ExternalApi savedApi = externalApiService.createApi(api);
        
        // 파라미터들 생성
        if (parameters != null && !parameters.isEmpty()) {
            for (ApiParameter parameter : parameters) {
                parameter.setApiId(savedApi.getApiId());
                apiParameterService.createParameter(parameter);
            }
            log.info("Created {} parameters for API: {}", parameters.size(), savedApi.getApiId());
        }
        
        return savedApi;
    }

    /**
     * API와 파라미터를 함께 조회
     */
    public ApiWithParameters getApiWithParameters(String apiId) {
        log.debug("Fetching API with parameters: {}", apiId);
        
        Optional<ExternalApi> api = externalApiService.getApiById(apiId);
        if (api.isEmpty()) {
            return null;
        }
        
        List<ApiParameter> parameters = apiParameterService.getParametersByApiId(apiId);
        
        return ApiWithParameters.builder()
                .api(api.get())
                .parameters(parameters)
                .build();
    }

    /**
     * API와 파라미터를 함께 업데이트
     */
    @Transactional
    public ExternalApi updateApiWithParameters(String apiId, ExternalApi updateData, List<ApiParameter> newParameters) {
        log.info("Updating API with parameters: {}", apiId);
        
        // API 업데이트
        ExternalApi updatedApi = externalApiService.updateApi(apiId, updateData);
        
        // 기존 파라미터들 삭제
        apiParameterService.deleteAllParametersByApiId(apiId);
        
        // 새로운 파라미터들 생성
        if (newParameters != null && !newParameters.isEmpty()) {
            for (ApiParameter parameter : newParameters) {
                parameter.setApiId(apiId);
                apiParameterService.createParameter(parameter);
            }
            log.info("Updated {} parameters for API: {}", newParameters.size(), apiId);
        }
        
        return updatedApi;
    }

    /**
     * API와 파라미터를 함께 삭제
     */
    @Transactional
    public void deleteApiWithParameters(String apiId) {
        log.info("Deleting API with parameters: {}", apiId);
        
        // 파라미터들 먼저 삭제
        apiParameterService.deleteAllParametersByApiId(apiId);
        
        // API 삭제
        externalApiService.deleteApi(apiId);
        
        log.info("API and parameters deleted: {}", apiId);
    }

    /**
     * 도메인별 API와 파라미터 조회
     */
    public List<ApiWithParameters> getApisWithParametersByDomain(String domain) {
        log.debug("Fetching APIs with parameters by domain: {}", domain);
        
        List<ExternalApi> apis = externalApiService.getApisByDomain(domain);
        
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
     * 키워드별 API와 파라미터 조회
     */
    public List<ApiWithParameters> getApisWithParametersByKeyword(String keyword) {
        log.debug("Fetching APIs with parameters by keyword: {}", keyword);
        
        List<ExternalApi> apis = externalApiService.getApisByKeyword(keyword);
        
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
     * API 검색 (이름, 설명, 도메인, 키워드로)
     */
    public List<ApiWithParameters> searchApisWithParameters(String searchTerm) {
        log.debug("Searching APIs with parameters: {}", searchTerm);
        
        List<ExternalApi> apis = externalApiService.searchApis(searchTerm);
        
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
     * API 복사 (새로운 이름으로)
     */
    @Transactional
    public ExternalApi copyApi(String sourceApiId, String newApiName) {
        log.info("Copying API: {} with new name: {}", sourceApiId, newApiName);
        
        Optional<ExternalApi> sourceApi = externalApiService.getApiById(sourceApiId);
        if (sourceApi.isEmpty()) {
            throw new IllegalArgumentException("Source API not found: " + sourceApiId);
        }
        
        // API 복사
        ExternalApi source = sourceApi.get();
        ExternalApi newApi = new ExternalApi();
        newApi.setApiName(newApiName);
        newApi.setApiUrl(source.getApiUrl());
        newApi.setApiIssuer(source.getApiIssuer());
        newApi.setApiOwner(source.getApiOwner());
        newApi.setApiDomain(source.getApiDomain());
        newApi.setApiKeyword(source.getApiKeyword());
        newApi.setHttpMethod(source.getHttpMethod());
        newApi.setApiDescription("Copy of " + source.getApiDescription());
        newApi.setApiEffectiveness(true); // 새로 복사된 API는 기본적으로 유효
        
        ExternalApi savedNewApi = externalApiService.createApi(newApi);
        
        // 파라미터들 복사
        List<ApiParameter> sourceParameters = apiParameterService.getParametersByApiId(sourceApiId);
        for (ApiParameter sourceParam : sourceParameters) {
            ApiParameter newParam = new ApiParameter();
            newParam.setApiId(savedNewApi.getApiId());
            newParam.setParamName(sourceParam.getParamName());
            newParam.setParamType(sourceParam.getParamType());
            newParam.setIsRequired(sourceParam.getIsRequired());
            newParam.setDefaultValue(sourceParam.getDefaultValue());
            
            apiParameterService.createParameter(newParam);
        }
        
        log.info("API copied successfully: {} -> {}", sourceApiId, savedNewApi.getApiId());
        return savedNewApi;
    }

    /**
     * API 유효성 검증
     */
    public ApiValidationResult validateApi(String apiId) {
        log.debug("Validating API: {}", apiId);
        
        Optional<ExternalApi> api = externalApiService.getApiById(apiId);
        if (api.isEmpty()) {
            return ApiValidationResult.builder()
                    .valid(false)
                    .errors(List.of("API not found"))
                    .build();
        }
        
        List<String> errors = new java.util.ArrayList<>();
        
        // API 기본 정보 검증
        ExternalApi apiEntity = api.get();
        if (apiEntity.getApiName() == null || apiEntity.getApiName().trim().isEmpty()) {
            errors.add("API name is required");
        }
        if (apiEntity.getApiUrl() == null || apiEntity.getApiUrl().trim().isEmpty()) {
            errors.add("API URL is required");
        }
        if (apiEntity.getHttpMethod() == null || apiEntity.getHttpMethod().trim().isEmpty()) {
            errors.add("HTTP method is required");
        }
        
        // 파라미터 검증
        List<ApiParameter> parameters = apiParameterService.getParametersByApiId(apiId);
        for (ApiParameter param : parameters) {
            if (param.getParamName() == null || param.getParamName().trim().isEmpty()) {
                errors.add("Parameter name is required for parameter: " + param.getParameterId());
            }
            if (param.getParamType() == null || param.getParamType().trim().isEmpty()) {
                errors.add("Parameter type is required for parameter: " + param.getParamName());
            }
        }
        
        boolean isValid = errors.isEmpty();
        
        return ApiValidationResult.builder()
                .valid(isValid)
                .errors(errors)
                .build();
    }

    /**
     * API와 파라미터 정보를 담는 DTO
     */
    public static class ApiWithParameters {
        private final ExternalApi api;
        private final List<ApiParameter> parameters;
        
        // Builder 패턴
        public static Builder builder() {
            return new Builder();
        }
        
        private ApiWithParameters(Builder builder) {
            this.api = builder.api;
            this.parameters = builder.parameters;
        }
        
        // Getters
        public ExternalApi getApi() { return api; }
        public List<ApiParameter> getParameters() { return parameters; }
        
        public static class Builder {
            private ExternalApi api;
            private List<ApiParameter> parameters;
            
            public Builder api(ExternalApi api) {
                this.api = api;
                return this;
            }
            
            public Builder parameters(List<ApiParameter> parameters) {
                this.parameters = parameters;
                return this;
            }
            
            public ApiWithParameters build() {
                return new ApiWithParameters(this);
            }
        }
    }

    /**
     * API 검증 결과를 담는 DTO
     */
    public static class ApiValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        // Builder 패턴
        public static Builder builder() {
            return new Builder();
        }
        
        private ApiValidationResult(Builder builder) {
            this.valid = builder.valid;
            this.errors = builder.errors;
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        
        public static class Builder {
            private boolean valid;
            private List<String> errors;
            
            public Builder valid(boolean valid) {
                this.valid = valid;
                return this;
            }
            
            public Builder errors(List<String> errors) {
                this.errors = errors;
                return this;
            }
            
            public ApiValidationResult build() {
                return new ApiValidationResult(this);
            }
        }
    }
}

