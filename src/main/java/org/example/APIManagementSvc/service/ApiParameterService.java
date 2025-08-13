package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.ApiParameter;
import org.example.APIManagementSvc.domain.ExternalApi;
import org.example.APIManagementSvc.dto.externalapi.ApiParameterRegisterRequest;
import org.example.APIManagementSvc.dto.externalapi.ApiParameterResponse;
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
 * 외부 서비스에서 생성된 API 파라미터를 받아서 관리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiParameterService {

    private final ApiParameterRepository apiParameterRepository;
    private final ExternalApiRepository externalApiRepository;

    /**
     * DTO를 받아서 파라미터 등록
     */
    @Transactional
    public ApiParameterResponse saveParameter(ApiParameterRegisterRequest request, String apiId) {
        log.info("Registering parameter from DTO: {} for API: {}", request.getParamName(), apiId);
        
        // API 존재 여부 확인
        Optional<ExternalApi> api = externalApiRepository.findById(apiId);
        if (api.isEmpty()) {
            throw new IllegalArgumentException("API not found with ID: " + apiId);
        }
        
        // DTO를 Entity로 변환
        ApiParameter parameter = new ApiParameter();
        parameter.setParameterId(UUID.randomUUID().toString());
        parameter.setApiId(apiId);
        parameter.setParamName(request.getParamName());
        parameter.setParamType(request.getParamType());
        parameter.setIsRequired(request.getIsRequired());
        parameter.setDefaultValue(request.getDefaultValue());
        parameter.setCreatedAt(LocalDateTime.now());
        parameter.setUpdatedAt(LocalDateTime.now());
        parameter.setDeleted(false);
        
        ApiParameter savedParameter = apiParameterRepository.save(parameter);
        log.info("Parameter registered successfully with ID: {}", savedParameter.getParameterId());
        
        return convertToResponse(savedParameter);
    }

    /**
     * 외부에서 전달받은 파라미터 저장
     * 파라미터는 다른 서비스에서 생성되어 전달됨
     */
    @Transactional
    public ApiParameter saveParameter(ApiParameter parameter) {
        log.info("Saving parameter from external service: {} for API: {}", parameter.getParamName(), parameter.getApiId());
        
        // API 존재 여부 확인
        Optional<ExternalApi> api = externalApiRepository.findById(parameter.getApiId());
        if (api.isEmpty()) {
            throw new IllegalArgumentException("API not found with ID: " + parameter.getApiId());
        }
        
        // 파라미터 ID가 없는 경우에만 자동 생성
        if (parameter.getParameterId() == null || parameter.getParameterId().isEmpty()) {
            parameter.setParameterId(UUID.randomUUID().toString());
        }
        
        // 타임스탬프 설정
        if (parameter.getCreatedAt() == null) {
            parameter.setCreatedAt(LocalDateTime.now());
        }
        parameter.setUpdatedAt(LocalDateTime.now());
        parameter.setDeleted(false);
        
        ApiParameter savedParameter = apiParameterRepository.save(parameter);
        log.info("Parameter saved successfully with ID: {}", savedParameter.getParameterId());
        
        return savedParameter;
    }



    /**
     * 외부에서 전달받은 파라미터 목록 일괄 저장
     */
    @Transactional
    public List<ApiParameter> saveParameters(List<ApiParameter> parameters) {
        log.info("Saving {} parameters from external service", parameters.size());
        
        for (ApiParameter parameter : parameters) {
            // API 존재 여부 확인
            Optional<ExternalApi> api = externalApiRepository.findById(parameter.getApiId());
            if (api.isEmpty()) {
                throw new IllegalArgumentException("API not found with ID: " + parameter.getApiId());
            }
            
            // 파라미터 ID가 없는 경우에만 자동 생성
            if (parameter.getParameterId() == null || parameter.getParameterId().isEmpty()) {
                parameter.setParameterId(UUID.randomUUID().toString());
            }
            
            // 타임스탬프 설정
            if (parameter.getCreatedAt() == null) {
                parameter.setCreatedAt(LocalDateTime.now());
            }
            parameter.setUpdatedAt(LocalDateTime.now());
            parameter.setDeleted(false);
        }
        
        List<ApiParameter> savedParameters = apiParameterRepository.saveAll(parameters);
        log.info("{} parameters saved successfully", savedParameters.size());
        
        return savedParameters;
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
     * 외부에서 전달받은 파라미터 업데이트
     */
    @Transactional
    public ApiParameter updateParameter(String parameterId, ApiParameter updateData) {
        log.info("Updating parameter from external service: {}", parameterId);
        
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
        
        // 업데이트 시간 설정
        parameter.setUpdatedAt(LocalDateTime.now());
        
        ApiParameter updatedParameter = apiParameterRepository.save(parameter);
        log.info("Parameter updated successfully: {}", parameterId);
        
        return updatedParameter;
    }



    /**
     * 파라미터 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteParameter(String parameterId) {
        log.info("Soft deleting parameter: {}", parameterId);
        
        Optional<ApiParameter> parameter = apiParameterRepository.findById(parameterId);
        if (parameter.isPresent()) {
            ApiParameter entity = parameter.get();
            entity.setDeleted(true);
            entity.setUpdatedAt(LocalDateTime.now());
            apiParameterRepository.save(entity);
            log.info("Parameter soft deleted: {}", parameterId);
        } else {
            log.warn("Parameter not found for deletion: {}", parameterId);
        }
    }

    /**
     * 파라미터 완전 삭제 (Hard Delete)
     */
    @Transactional
    public void hardDeleteParameter(String parameterId) {
        log.info("Hard deleting parameter: {}", parameterId);
        apiParameterRepository.deleteById(parameterId);
        log.info("Parameter hard deleted: {}", parameterId);
    }

    /**
     * API의 모든 파라미터 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteAllParametersByApiId(String apiId) {
        log.info("Soft deleting all parameters for API: {}", apiId);
        
        List<ApiParameter> parameters = apiParameterRepository.findByApiIdAndDeletedFalse(apiId);
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
        log.debug("Searching parameters for API: {} with term: {}", apiId, searchTerm);
        return apiParameterRepository.findByApiIdAndParamNameContainingAndDeletedFalse(apiId, searchTerm);
    }

    /**
     * 파라미터 통계 조회
     */
    public ParameterStatistics getParameterStatistics(String apiId) {
        log.debug("Getting parameter statistics for API: {}", apiId);
        
        List<ApiParameter> parameters = apiParameterRepository.findByApiIdAndDeletedFalse(apiId);
        
        long totalParameters = parameters.size();
        long requiredParameters = parameters.stream().filter(ApiParameter::getIsRequired).count();
        long optionalParameters = totalParameters - requiredParameters;
        
        long stringParameters = parameters.stream()
            .filter(p -> "string".equalsIgnoreCase(p.getParamType()))
            .count();
        long integerParameters = parameters.stream()
            .filter(p -> "integer".equalsIgnoreCase(p.getParamType()) || "int".equalsIgnoreCase(p.getParamType()))
            .count();
        long booleanParameters = parameters.stream()
            .filter(p -> "boolean".equalsIgnoreCase(p.getParamType()) || "bool".equalsIgnoreCase(p.getParamType()))
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
     * 파라미터 통계 DTO
     */
    public static class ParameterStatistics {
        private final long totalParameters;
        private final long requiredParameters;
        private final long optionalParameters;
        private final long stringParameters;
        private final long integerParameters;
        private final long booleanParameters;

        private ParameterStatistics(Builder builder) {
            this.totalParameters = builder.totalParameters;
            this.requiredParameters = builder.requiredParameters;
            this.optionalParameters = builder.optionalParameters;
            this.stringParameters = builder.stringParameters;
            this.integerParameters = builder.integerParameters;
            this.booleanParameters = builder.booleanParameters;
        }

        public static Builder builder() {
            return new Builder();
        }

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

    /**
     * ApiParameter Entity를 ApiParameterResponse DTO로 변환
     */
    private ApiParameterResponse convertToResponse(ApiParameter parameter) {
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
}

