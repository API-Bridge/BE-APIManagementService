package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.*;
import org.example.APIManagementSvc.dto.*;
import org.example.APIManagementSvc.dto.response.ExternalApiSpecRequestDto;
import org.example.APIManagementSvc.dto.response.ExternalApiSpecResponseDto;
import org.example.APIManagementSvc.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 외부 API 명세 관리 서비스
 * 
 * 관리자가 외부 API를 등록, 조회, 수정, 삭제할 수 있는 비즈니스 로직을 제공
 * 각 API의 파라미터는 유동적으로 관리되며, 관련 엔티티들과의 관계도 함께 처리
 * 
 * 주요 기능:
 * - 외부 API 명세 CRUD 작업
 * - 파라미터 유동적 관리 (API마다 다른 구조 지원)
 * - 관련 엔티티 자동 검증 (자격증명, 도메인, 키워드)
 * - 트랜잭션 관리로 데이터 일관성 보장
 * - 성능 최적화된 조회 (Fetch Join 활용)
 * 
 * 트랜잭션 설정:
 * - 기본: readOnly = true (조회 성능 최적화)
 * - 변경 작업: @Transactional 개별 적용
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExternalApiSpecService {

    private final ExternalApiSpecRepository externalApiSpecRepository;
    private final ApiParameterRepository apiParameterRepository;
    private final ApiCredentialRepository apiCredentialRepository;
    private final ApiDomainRepository apiDomainRepository;
    private final ApiKeywordRepository apiKeywordRepository;

    /**
     * 새로운 외부 API 명세 등록
     * 
     * 관리자가 새로운 외부 API를 시스템에 등록할 때 사용
     * 필수 관련 엔티티들의 존재 여부를 검증하고, 파라미터도 함께 저장
     * 
     * @param requestDto 등록할 API 명세 정보
     * @return 등록된 API 명세 정보 (관련 정보 포함)
     * @throws IllegalArgumentException 존재하지 않는 관련 엔티티 ID 전달 시
     */
    @Transactional
    public ExternalApiSpecResponseDto createExternalApiSpec(ExternalApiSpecRequestDto requestDto) {
        log.info("Creating external API spec with id: {}", requestDto.getApiId());
        
        // 필수 관련 엔티티 조회
        ApiCredential credential = apiCredentialRepository.findById(requestDto.getCredentialId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자격증명 ID: " + requestDto.getCredentialId()));
        
        ApiDomain domain = null;
        if (requestDto.getDomainId() != null) {
            domain = apiDomainRepository.findById(requestDto.getDomainId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도메인 ID: " + requestDto.getDomainId()));
        }
        
        ApiKeyword keyword = null;
        if (requestDto.getKeywordId() != null) {
            keyword = apiKeywordRepository.findById(requestDto.getKeywordId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 키워드 ID: " + requestDto.getKeywordId()));
        }

        // ExternalApiSpec 생성
        ExternalApiSpec apiSpec = ExternalApiSpec.builder()
                .apiId(requestDto.getApiId())
                .apiName(requestDto.getApiName())
                .apiDescription(requestDto.getApiDescription())
                .apiIssuer(requestDto.getApiIssuer())
                .apiUrl(requestDto.getApiUrl())
                .httpMethod(requestDto.getHttpMethod())
                .isActive(requestDto.getIsActive())
                .credential(credential)
                .domain(domain)
                .keyword(keyword)
                .build();

        ExternalApiSpec savedApiSpec = externalApiSpecRepository.save(apiSpec);

        // 파라미터 저장
        if (requestDto.getParameters() != null && !requestDto.getParameters().isEmpty()) {
            List<ApiParameter> parameters = requestDto.getParameters().stream()
                    .map(paramDto -> ApiParameter.builder()
                            .parameterId(UUID.randomUUID().toString())
                            .paramName(paramDto.getParamName())
                            .paramType(paramDto.getParamType())
                            .isRequired(paramDto.getIsRequired())
                            .paramDescription(paramDto.getParamDescription())
                            .defaultValue(paramDto.getDefaultValue())
                            .additionalFields(paramDto.getAdditionalFields())
                            .apiSpec(savedApiSpec)
                            .build())
                    .collect(Collectors.toList());
            
            apiParameterRepository.saveAll(parameters);
        }

        return convertToResponseDto(savedApiSpec);
    }

    /**
     * 외부 API 명세 단건 상세 조회
     * 
     * @param apiId 조회할 API 고유 식별자
     * @return API 명세 상세 정보
     * @throws IllegalArgumentException 존재하지 않는 API ID인 경우
     */
    public ExternalApiSpecResponseDto getExternalApiSpec(String apiId) {
        log.info("Getting external API spec with id: {}", apiId);
        
        ExternalApiSpec apiSpec = externalApiSpecRepository.findByIdWithAllRelations(apiId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 API ID: " + apiId));
        
        return convertToResponseDto(apiSpec);
    }

    /**
     * 전체 외부 API 명세 페이징 조회
     * 
     * @param pageable 페이징 정보
     * @return 페이징된 API 명세 목록
     */
    public Page<ExternalApiSpecResponseDto> getAllExternalApiSpecs(Pageable pageable) {
        log.info("Getting all external API specs");
        
        return externalApiSpecRepository.findAll(pageable)
                .map(this::convertToResponseDto);
    }

    /**
     * 활성화된 외부 API 명세 전체 조회
     * 
     * @return 활성화된 API 명세 목록
     */
    public List<ExternalApiSpecResponseDto> getAllActiveExternalApiSpecs() {
        log.info("Getting all active external API specs");
        
        return externalApiSpecRepository.findAllActiveWithRelations().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 외부 API 명세 정보 수정
     * 
     * @param apiId 수정할 API 고유 식별자
     * @param updateDto 수정할 정보
     * @return 수정된 API 명세 정보
     * @throws IllegalArgumentException 존재하지 않는 API ID인 경우
     */
    @Transactional
    public ExternalApiSpecResponseDto updateExternalApiSpec(String apiId, ExternalApiSpecUpdateDto updateDto) {
        log.info("Updating external API spec with id: {}", apiId);
        
        ExternalApiSpec apiSpec = externalApiSpecRepository.findById(apiId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 API ID: " + apiId));

        // 기본 정보 업데이트
        if (updateDto.getApiName() != null) {
            apiSpec.setApiName(updateDto.getApiName());
        }
        if (updateDto.getApiDescription() != null) {
            apiSpec.setApiDescription(updateDto.getApiDescription());
        }
        if (updateDto.getApiIssuer() != null) {
            apiSpec.setApiIssuer(updateDto.getApiIssuer());
        }
        if (updateDto.getApiUrl() != null) {
            apiSpec.setApiUrl(updateDto.getApiUrl());
        }
        if (updateDto.getHttpMethod() != null) {
            apiSpec.setHttpMethod(updateDto.getHttpMethod());
        }
        if (updateDto.getIsActive() != null) {
            apiSpec.setIsActive(updateDto.getIsActive());
        }

        // 관련 엔티티 업데이트
        if (updateDto.getCredentialId() != null) {
            ApiCredential credential = apiCredentialRepository.findById(updateDto.getCredentialId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자격증명 ID: " + updateDto.getCredentialId()));
            apiSpec.setCredential(credential);
        }

        if (updateDto.getDomainId() != null) {
            ApiDomain domain = apiDomainRepository.findById(updateDto.getDomainId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도메인 ID: " + updateDto.getDomainId()));
            apiSpec.setDomain(domain);
        }

        if (updateDto.getKeywordId() != null) {
            ApiKeyword keyword = apiKeywordRepository.findById(updateDto.getKeywordId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 키워드 ID: " + updateDto.getKeywordId()));
            apiSpec.setKeyword(keyword);
        }

        // 파라미터 업데이트 (기존 파라미터 삭제 후 새로 생성)
        if (updateDto.getParameters() != null) {
            apiParameterRepository.deleteByApiSpec_ApiId(apiId);
            
            if (!updateDto.getParameters().isEmpty()) {
                List<ApiParameter> parameters = updateDto.getParameters().stream()
                        .map(paramDto -> ApiParameter.builder()
                                .parameterId(UUID.randomUUID().toString())
                                .paramName(paramDto.getParamName())
                                .paramType(paramDto.getParamType())
                                .isRequired(paramDto.getIsRequired())
                                .paramDescription(paramDto.getParamDescription())
                                .defaultValue(paramDto.getDefaultValue())
                                .additionalFields(paramDto.getAdditionalFields())
                                .apiSpec(apiSpec)
                                .build())
                        .collect(Collectors.toList());
                
                apiParameterRepository.saveAll(parameters);
            }
        }

        ExternalApiSpec updatedApiSpec = externalApiSpecRepository.save(apiSpec);
        return convertToResponseDto(updatedApiSpec);
    }

    /**
     * 외부 API 명세 삭제
     * 
     * @param apiId 삭제할 API 고유 식별자
     * @throws IllegalArgumentException 존재하지 않는 API ID인 경우
     */
    @Transactional
    public void deleteExternalApiSpec(String apiId) {
        log.info("Deleting external API spec with id: {}", apiId);
        
        if (!externalApiSpecRepository.existsById(apiId)) {
            throw new IllegalArgumentException("존재하지 않는 API ID: " + apiId);
        }
        
        externalApiSpecRepository.deleteById(apiId);
    }

    /**
     * 특정 자격증명으로 등록된 외부 API 명세들 조회
     * 
     * @param credentialId 자격증명 ID
     * @return 해당 자격증명을 사용하는 API 명세 목록
     */
    public List<ExternalApiSpecResponseDto> getExternalApiSpecsByCredential(String credentialId) {
        return externalApiSpecRepository.findByCredential_CredentialId(credentialId).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 도메인에 속한 외부 API 명세들 조회
     * 
     * @param domainId 도메인 ID
     * @return 해당 도메인의 API 명세 목록
     */
    public List<ExternalApiSpecResponseDto> getExternalApiSpecsByDomain(Integer domainId) {
        return externalApiSpecRepository.findByDomain_DomainId(domainId).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 키워드에 속한 외부 API 명세들 조회
     * 
     * @param keywordId 키워드 ID
     * @return 해당 키워드의 API 명세 목록
     */
    public List<ExternalApiSpecResponseDto> getExternalApiSpecsByKeyword(Integer keywordId) {
        return externalApiSpecRepository.findByKeyword_KeywordId(keywordId).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Entity를 ResponseDTO로 변환하는 내부 메서드
     * 
     * @param apiSpec 변환할 엔티티
     * @return 클라이언트용 응답 DTO
     */
    private ExternalApiSpecResponseDto convertToResponseDto(ExternalApiSpec apiSpec) {
        List<ApiParameterDto> parameterDtos = apiSpec.getParameters() != null ? 
                apiSpec.getParameters().stream()
                        .map(param -> ApiParameterDto.builder()
                                .parameterId(param.getParameterId())
                                .paramName(param.getParamName())
                                .paramType(param.getParamType())
                                .isRequired(param.getIsRequired())
                                .paramDescription(param.getParamDescription())
                                .defaultValue(param.getDefaultValue())
                                .additionalFields(param.getAdditionalFields())
                                .build())
                        .collect(Collectors.toList()) : List.of();

        return ExternalApiSpecResponseDto.builder()
                .apiId(apiSpec.getApiId())
                .apiName(apiSpec.getApiName())
                .apiDescription(apiSpec.getApiDescription())
                .apiIssuer(apiSpec.getApiIssuer())
                .apiUrl(apiSpec.getApiUrl())
                .httpMethod(apiSpec.getHttpMethod())
                .isActive(apiSpec.getIsActive())
                .createdAt(apiSpec.getCreatedAt())
                .updatedAt(apiSpec.getUpdatedAt())
                .credentialId(apiSpec.getCredential() != null ? apiSpec.getCredential().getCredentialId() : null)
                .organizationName(apiSpec.getCredential() != null ? apiSpec.getCredential().getOrganizationName() : null)
                .domainId(apiSpec.getDomain() != null ? apiSpec.getDomain().getDomainId() : null)
                .domainName(apiSpec.getDomain() != null ? apiSpec.getDomain().getDomainName() : null)
                .keywordId(apiSpec.getKeyword() != null ? apiSpec.getKeyword().getKeywordId() : null)
                .keywordName(apiSpec.getKeyword() != null ? apiSpec.getKeyword().getKeywordName() : null)
                .parameters(parameterDtos)
                .build();
    }
}