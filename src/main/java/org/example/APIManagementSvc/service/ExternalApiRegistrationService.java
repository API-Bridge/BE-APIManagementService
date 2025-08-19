package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.domain.Entity.ApiParameter;
import org.example.APIManagementSvc.domain.Entity.AiClassification;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import org.example.APIManagementSvc.dto.externalapi.ExternalApiRegistrationRequest;
import org.example.APIManagementSvc.dto.externalapi.ExternalApiRegistrationResponse;
import org.example.APIManagementSvc.repository.ExternalApiRepository;
import org.example.APIManagementSvc.repository.ApiParameterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 🔥 API 등록 서비스
 * 관리자가 API를 등록할 때 AI 분류, 저장, 파라미터 관리를 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiRegistrationService {

    private final ExternalApiRepository externalApiRepository;
    private final ApiParameterRepository apiParameterRepository;
    private final AiClassificationService aiClassificationService;

    /**
     * 🔥 API 등록 메인 프로세스
     * 1. AI 분류 요청
     * 2. ExternalApi 엔티티 생성 및 저장
     * 3. API 파라미터 생성 및 저장
     * 4. 응답 생성
     */
    @Transactional
    public ExternalApiRegistrationResponse registerApi(ExternalApiRegistrationRequest request) {
        log.info("API 등록 시작: {}", request.getApiName());

        try {
            // 1. 🔥 API ID 생성
            String apiId = generateApiId();

            // 2. 🔥 AI 분류 요청 (신뢰도/프롬프트 미사용)
            String classificationPrompt = buildClassificationPrompt(request);
            AiClassification classification = aiClassificationService.classifyApi(
                apiId,
                request.getApiName(),
                request.getApiDescription(),
                request.getApiUrl(),
                classificationPrompt
            );

            // 3. 🔥 ExternalApi 엔티티 생성 및 저장
            ExternalApi externalApi = createExternalApi(request, apiId, classification);
            ExternalApi savedApi = externalApiRepository.save(externalApi);

            // 4. 🔥 API 파라미터 생성 및 저장
            List<ApiParameter> parameters = createApiParameters(request.getParameters(), apiId);
            List<ApiParameter> savedParameters = apiParameterRepository.saveAll(parameters);

            // 5. 🔥 응답 생성
            ExternalApiRegistrationResponse response = buildResponse(savedApi, savedParameters, classification);
            
            log.info("API 등록 완료: {} (ID: {})", savedApi.getApiName(), savedApi.getApiId());
            return response;

        } catch (Exception e) {
            log.error("API 등록 중 오류 발생: {}", request.getApiName(), e);
            throw new RuntimeException("API 등록에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 🔥 AI 분류를 위한 프롬프트 생성
     */
    private String buildClassificationPrompt(ExternalApiRegistrationRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 API 정보를 분석하여 도메인과 키워드를 분류해주세요:\n\n");
        prompt.append("API 이름: ").append(request.getApiName()).append("\n");
        prompt.append("API 설명: ").append(request.getApiDescription()).append("\n");
        prompt.append("API URL: ").append(request.getApiUrl()).append("\n");
        prompt.append("HTTP 메소드: ").append(request.getHttpMethod()).append("\n");
        prompt.append("파라미터 개수: ").append(request.getParameters().size()).append("\n");
        
        // 파라미터 정보 추가
        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            prompt.append("주요 파라미터:\n");
            request.getParameters().stream()
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
     * 🔥 ExternalApi 엔티티 생성
     */
    private ExternalApi createExternalApi(ExternalApiRegistrationRequest request, String apiId, 
                                        AiClassification classification) {
        ExternalApi externalApi = new ExternalApi();
        
        // 기본 정보 설정
        externalApi.initializeApi(
            apiId,
            request.getApiName(),
            request.getApiUrl(),
            request.getApiIssuer(),
            request.getApiOwner(),
            request.getHttpMethod(),
            request.getApiDescription()
        );

        // AI 분류 결과 설정
        externalApi.setAiClassification(
            classification.getClassifiedDomain(),
            classification.getClassifiedKeyword()
        );

        return externalApi;
    }

    /**
     * 🔥 API 파라미터 엔티티 목록 생성
     */
    private List<ApiParameter> createApiParameters(List<ExternalApiRegistrationRequest.ApiParameterRequest> paramRequests, String apiId) {
        return paramRequests.stream()
            .map(paramRequest -> {
                ApiParameter parameter = new ApiParameter();
                
                // 기본 필드 설정
                parameter.setParameterId(generateParameterId());
                parameter.setApiId(apiId);
                parameter.setParamName(paramRequest.getParamName());
                parameter.setParamType(paramRequest.getParamType());
                parameter.setIsRequired(paramRequest.getIsRequired());
                parameter.setDefaultValue(paramRequest.getDefaultValue());
                parameter.setParamDescription(paramRequest.getParamDescription());
                
                // 🔥 동적 추가 필드 설정
                if (paramRequest.getAdditionalFields() != null && !paramRequest.getAdditionalFields().isEmpty()) {
                    parameter.setAdditionalFieldsMap(paramRequest.getAdditionalFields());
                }
                
                return parameter;
            })
            .collect(Collectors.toList());
    }

    /**
     * 🔥 응답 객체 생성
     */
    private ExternalApiRegistrationResponse buildResponse(ExternalApi savedApi, List<ApiParameter> savedParameters,
                                                        AiClassification classification) {
        return ExternalApiRegistrationResponse.builder()
            .success(true)
            .apiId(savedApi.getApiId())
            .apiName(savedApi.getApiName())
            .apiUrl(savedApi.getApiUrl())
            .classifiedDomain(savedApi.getApiDomain())
            .classifiedKeyword(savedApi.getApiKeyword())
            .parameterCount(savedParameters.size())
            .parameters(savedParameters.stream()
                .map(this::buildParameterResponse)
                .collect(Collectors.toList()))
            .registeredAt(savedApi.getCreatedAt())
            .message("API가 성공적으로 등록되었습니다. AI 분류 결과: " + 
                    savedApi.getApiDomain() + " - " + savedApi.getApiKeyword())
            .build();
    }

    /**
     * 🔥 파라미터 응답 객체 생성
     */
    private ExternalApiRegistrationResponse.ApiParameterResponse buildParameterResponse(ApiParameter parameter) {
        return ExternalApiRegistrationResponse.ApiParameterResponse.builder()
            .parameterId(parameter.getParameterId())
            .paramName(parameter.getParamName())
            .paramType(parameter.getParamType())
            .isRequired(parameter.getIsRequired())
            .defaultValue(parameter.getDefaultValue())
            .paramDescription(parameter.getParamDescription())
            .additionalFields(parameter.getAdditionalFieldsMap())
            .build();
    }

    /**
     * 🔥 API ID 생성
     */
    private String generateApiId() {
        return "API_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * 🔥 파라미터 ID 생성
     */
    private String generateParameterId() {
        return "PARAM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
