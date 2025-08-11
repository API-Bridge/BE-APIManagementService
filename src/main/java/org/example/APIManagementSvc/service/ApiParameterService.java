package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.ApiParameter;
import org.example.APIManagementSvc.domain.ExternalApi;
import org.example.APIManagementSvc.repository.ApiParameterRepository;
import org.example.APIManagementSvc.repository.ExternalApiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API 파라미터 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiParameterService {

    private final ApiParameterRepository apiParameterRepository;
    private final ExternalApiRepository externalApiRepository;

    /**
     * 파라미터 생성
     */
    @Transactional
    public ApiParameter createParameter(ApiParameter parameter) {
        log.info("Creating new parameter: {} for API: {}", parameter.getParamName(), parameter.getApiId());
        
        // API 존재 여부 확인
        Optional<ExternalApi> api = externalApiRepository.findById(parameter.getApiId());
        if (api.isEmpty()) {
            throw new IllegalArgumentException("API not found with ID: " + parameter.getApiId());
        }
        
        // 파라미터 ID 자동 생성
        parameter.setParameterId(UUID.randomUUID().toString());
        parameter.setCreatedAt(LocalDateTime.now());
        parameter.setUpdatedAt(LocalDateTime.now());
        parameter.setDeleted(false);
        
        ApiParameter savedParameter = apiParameterRepository.save(parameter);
        log.info("Parameter created successfully with ID: {}", savedParameter.getParameterId());
        
        return savedParameter;
    }

    /**
     * 파라미터 조회 (ID로)
     */
    public Optional<ApiParameter> getParameterById(String parameterId) {
        log.debug("Fetching parameter by ID: {}", parameterId);
        return apiParameterRepository.findById(parameterId);
    }

    /**
     * API의 모든 파라미터 조회
     */
    public List<ApiParameter> getParametersByApiId(String apiId) {
        log.debug("Fetching parameters for API: {}", apiId);
        return apiParameterRepository.findByApiIdAndDeletedFalse(apiId);
    }

    /**
     * 필수 파라미터만 조회
     */
    public List<ApiParameter> getRequiredParametersByApiId(String apiId) {
        log.debug("Fetching required parameters for API: {}", apiId);
        return apiParameterRepository.findByApiIdAndIsRequiredTrueAndDeletedFalse(apiId);
    }

    /**
     * 파라미터 타입별 조회
     */
    public List<ApiParameter> getParametersByType(String apiId, String paramType) {
        log.debug("Fetching parameters by type: {} for API: {}", paramType, apiId);
        return apiParameterRepository.findByApiIdAndParamTypeAndDeletedFalse(apiId, paramType);
    }

    /**
     * 파라미터 업데이트
     */
    @Transactional
    public ApiParameter updateParameter(String parameterId, ApiParameter updateData) {
        log.info("Updating parameter: {}", parameterId);
        
        Optional<ApiParameter> existingParameter = apiParameterRepository.findById(parameterId);
        if (existingParameter.isEmpty()) {
            throw new IllegalArgumentException("Parameter not found with ID: " + parameterId);
        }
        
        ApiParameter parameter = existingParameter.get();
        
        // 업데이트 가능한 필드들만 수정
        if (updateData.getParamName() != null) {
            parameter.setParamName(updateData.getParamName());
        }
        if (updateData.getParamType() != null) {
            parameter.setParamType(updateData.getParamType());
        }
        if (updateData.getIsRequired() != null) {
            parameter.setIsRequired(updateData.getIsRequired());
        }
        if (updateData.getDefaultValue() != null) {
            parameter.setDefaultValue(updateData.getDefaultValue());
        }
        
        parameter.setUpdatedAt(LocalDateTime.now());
        
        ApiParameter updatedParameter = apiParameterRepository.save(parameter);
        log.info("Parameter updated successfully: {}", parameterId);
        
        return updatedParameter;
    }

    /**
     * 파라미터 소프트 삭제
     */
    @Transactional
    public void deleteParameter(String parameterId) {
        log.info("Soft deleting parameter: {}", parameterId);
        
        Optional<ApiParameter> existingParameter = apiParameterRepository.findById(parameterId);
        if (existingParameter.isEmpty()) {
            throw new IllegalArgumentException("Parameter not found with ID: " + parameterId);
        }
        
        ApiParameter parameter = existingParameter.get();
        parameter.setDeleted(true);
        parameter.setUpdatedAt(LocalDateTime.now());
        
        apiParameterRepository.save(parameter);
        log.info("Parameter soft deleted: {}", parameterId);
    }

    /**
     * 파라미터 완전 삭제 (하드 삭제)
     */
    @Transactional
    public void hardDeleteParameter(String parameterId) {
        log.info("Hard deleting parameter: {}", parameterId);
        apiParameterRepository.deleteById(parameterId);
        log.info("Parameter hard deleted: {}", parameterId);
    }

    /**
     * API의 모든 파라미터 삭제
     */
    @Transactional
    public void deleteAllParametersByApiId(String apiId) {
        log.info("Deleting all parameters for API: {}", apiId);
        
        List<ApiParameter> parameters = apiParameterRepository.findByApiId(apiId);
        for (ApiParameter parameter : parameters) {
            parameter.setDeleted(true);
            parameter.setUpdatedAt(LocalDateTime.now());
        }
        
        apiParameterRepository.saveAll(parameters);
        log.info("All parameters deleted for API: {}", apiId);
    }

    /**
     * 파라미터 검색
     */
    public List<ApiParameter> searchParameters(String apiId, String searchTerm) {
        log.debug("Searching parameters with term: {} for API: {}", searchTerm, apiId);
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getParametersByApiId(apiId);
        }
        
        String term = searchTerm.toLowerCase().trim();
        return apiParameterRepository.findByApiIdAndParamNameContainingAndDeletedFalse(apiId, term);
    }

    /**
     * 파라미터 통계 정보
     */
    public ParameterStatistics getParameterStatistics(String apiId) {
        log.debug("Fetching parameter statistics for API: {}", apiId);
        
        List<ApiParameter> allParameters = apiParameterRepository.findByApiIdAndDeletedFalse(apiId);
        long totalParameters = allParameters.size();
        long requiredParameters = allParameters.stream()
                .filter(ApiParameter::getIsRequired)
                .count();
        long optionalParameters = totalParameters - requiredParameters;
        
        // 타입별 통계
        long stringParameters = allParameters.stream()
                .filter(p -> "string".equalsIgnoreCase(p.getParamType()))
                .count();
        long integerParameters = allParameters.stream()
                .filter(p -> "integer".equalsIgnoreCase(p.getParamType()))
                .count();
        long booleanParameters = allParameters.stream()
                .filter(p -> "boolean".equalsIgnoreCase(p.getParamType()))
                .count();
        
        return ParameterStatistics.builder()
                .totalParameters(totalParameters)
                .requiredParameters(requiredParameters)
                .optionalParameters(optionalParameters)
                .stringParameters(stringParameters)
                .integerParameters(integerParameters)
                .booleanParameters(booleanParameters)
                .build();
    }

    /**
     * 파라미터 통계 정보 DTO
     */
    public static class ParameterStatistics {
        private final long totalParameters;
        private final long requiredParameters;
        private final long optionalParameters;
        private final long stringParameters;
        private final long integerParameters;
        private final long booleanParameters;
        
        // Builder 패턴
        public static Builder builder() {
            return new Builder();
        }
        
        private ParameterStatistics(Builder builder) {
            this.totalParameters = builder.totalParameters;
            this.requiredParameters = builder.requiredParameters;
            this.optionalParameters = builder.optionalParameters;
            this.stringParameters = builder.stringParameters;
            this.integerParameters = builder.integerParameters;
            this.booleanParameters = builder.booleanParameters;
        }
        
        // Getters
        public long getTotalParameters() { return totalParameters; }
        public long getRequiredParameters() { return requiredParameters; }
        public long getOptionalParameters() { return optionalParameters; }
        public long getStringParameters() { return stringParameters; }
        public long getIntegerParameters() { return integerParameters; }
        public long getBooleanParameters() { return booleanParameters; }
        
        public static class Builder {
            private long totalParameters;
            private long requiredParameters;
            private long optionalParameters;
            private long stringParameters;
            private long integerParameters;
            private long booleanParameters;
            
            public Builder totalParameters(long totalParameters) {
                this.totalParameters = totalParameters;
                return this;
            }
            
            public Builder requiredParameters(long requiredParameters) {
                this.requiredParameters = requiredParameters;
                return this;
            }
            
            public Builder optionalParameters(long optionalParameters) {
                this.optionalParameters = optionalParameters;
                return this;
            }
            
            public Builder stringParameters(long stringParameters) {
                this.stringParameters = stringParameters;
                return this;
            }
            
            public Builder integerParameters(long integerParameters) {
                this.integerParameters = integerParameters;
                return this;
            }
            
            public Builder booleanParameters(long booleanParameters) {
                this.booleanParameters = booleanParameters;
                return this;
            }
            
            public ParameterStatistics build() {
                return new ParameterStatistics(this);
            }
        }
    }
}

