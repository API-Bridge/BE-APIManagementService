package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.*;
import org.example.APIManagementSvc.dto.*;
import org.example.APIManagementSvc.dto.response.ExternalApiSpecRequestDto;
import org.example.APIManagementSvc.dto.response.ExternalApiSpecResponseDto;
import org.example.APIManagementSvc.event.model.ExternalApiDeletedEvent;
import org.example.APIManagementSvc.event.model.ExternalApiRegisteredEvent;
import org.example.APIManagementSvc.event.publisher.EventPublisher;
import org.example.APIManagementSvc.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final GeminiService geminiService;
    private final EventPublisher eventPublisher;
    private final SgisTokenService sgisTokenService;

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
        ApiKeyword keyword = null;
        
        if (requestDto.getDomainId() != null) {
            domain = apiDomainRepository.findById(requestDto.getDomainId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도메인 ID: " + requestDto.getDomainId()));
        }
        
        if (requestDto.getKeywordId() != null) {
            keyword = apiKeywordRepository.findById(requestDto.getKeywordId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 키워드 ID: " + requestDto.getKeywordId()));
        }
        
        // Gemini AI를 이용한 자동 분류 (domain 또는 keyword가 null인 경우)
        if (domain == null || keyword == null) {
            try {
                // 기존 도메인과 키워드 목록 조회
                List<ApiDomain> existingDomains = apiDomainRepository.findAll();
                List<ApiKeyword> existingKeywords = apiKeywordRepository.findAll();
                
                String domainNames = existingDomains.stream()
                    .map(ApiDomain::getDomainName)
                    .collect(Collectors.joining(", "));
                
                String keywordNames = existingKeywords.stream()
                    .map(ApiKeyword::getKeywordName)
                    .collect(Collectors.joining(", "));
                
                String classificationResult = geminiService.classifyApiWithContext(
                    requestDto.getApiName(),
                    requestDto.getApiDescription(),
                    requestDto.getApiUrl(),
                    domainNames,
                    keywordNames
                );
                
                log.info("Gemini classification result: {}", classificationResult);
                
                // 분류 결과 파싱 및 domain/keyword 찾기/생성
                if (domain == null) {
                    domain = findOrCreateDomain(classificationResult, existingDomains);
                }
                
                if (keyword == null) {
                    keyword = findOrCreateKeyword(classificationResult, existingKeywords, domain);
                }
                
            } catch (Exception e) {
                log.warn("Failed to classify API using Gemini AI: {}", e.getMessage());
                // AI 분류 실패 시 기본값 설정 또는 null로 유지
            }
        }

        // API ID 자동 생성 (사용자가 제공하지 않은 경우 또는 항상)
        String generatedApiId = (requestDto.getApiId() != null && !requestDto.getApiId().trim().isEmpty()) 
            ? requestDto.getApiId() 
            : UUID.randomUUID().toString();
        
        // 중복 체크
        while (externalApiSpecRepository.existsById(generatedApiId)) {
            generatedApiId = UUID.randomUUID().toString();
        }

        // ExternalApiSpec 생성
        ExternalApiSpec apiSpec = ExternalApiSpec.builder()
                .apiId(generatedApiId)
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
            
            // SGIS 자격증명을 사용하는 API인 경우, accessToken 파라미터에 현재 토큰값 설정
            if ("SGIS".equals(credential.getCredentialId())) {
                updateSgisAccessTokenParameters(parameters);
            }
        }

        // 외부 API 등록 이벤트 발행
        ExternalApiRegisteredEvent event = new ExternalApiRegisteredEvent(
                savedApiSpec.getApiId(),
                savedApiSpec.getApiName(),
                savedApiSpec.getApiUrl(),
                savedApiSpec.getApiDescription(),
                domain != null ? domain.getDomainName() : null,
                "1.0", // 버전 기본값
                "system" // 등록자 기본값
        );
        eventPublisher.publishEvent("external_api_events", event);
        log.info("Published External API Registered Event for API: {}", savedApiSpec.getApiName());

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
        log.info("활성화된 모든 외부 API 명세 조회");
        
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
        log.info("ID로 외부API 명세 수정: {}", apiId);
        
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
        
        ExternalApiSpec apiSpec = externalApiSpecRepository.findById(apiId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 API ID: " + apiId));
        
        // 외부 API 삭제 이벤트 발행
        ExternalApiDeletedEvent event = new ExternalApiDeletedEvent(
                apiSpec.getApiId(),
                apiSpec.getApiName(),
                apiSpec.getApiUrl(),
                "system", // 삭제자 기본값
                "Manual deletion" // 삭제 사유 기본값
        );
        eventPublisher.publishEvent("external_api_events", event);
        log.info("Published External API Deleted Event for API: {}", apiSpec.getApiName());
        
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
     * 도메인 이름으로 외부 API 명세들 조회
     *
     * @param domainName 도메인 이름 (예: "finance", "weather", "news")
     * @return 해당 도메인의 API 명세 목록
     */
    public List<ExternalApiSpecResponseDto> getExternalApiSpecsByDomainName(String domainName) {
        log.info("Getting external API specs by domain name: {}", domainName);

        return externalApiSpecRepository.findByDomain_DomainName(domainName).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 키워드 이름으로 외부 API 명세들 조회
     *
     * @param keywordName 키워드 이름 (예: "stock_price", "current_weather", "breaking_news")
     * @return 해당 키워드의 API 명세 목록
     */
    public List<ExternalApiSpecResponseDto> getExternalApiSpecsByKeywordName(String keywordName) {
        log.info("Getting external API specs by keyword name: {}", keywordName);

        return externalApiSpecRepository.findByKeyword_KeywordName(keywordName).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 도메인 이름과 키워드 이름을 함께 사용하여 외부 API 명세들 조회
     * 
     * 특정 도메인 내에서 특정 키워드에 해당하는 API들을 정확하게 필터링
     * 예시: finance + stock_price = 주식 관련 금융 API만 조회
     * 
     * @param domainName 도메인 이름 (예: "finance", "weather", "news")
     * @param keywordName 키워드 이름 (예: "stock_price", "current_weather", "breaking_news")
     * @return 해당 도메인과 키워드 조건에 매칭되는 API 명세 목록
     */
    public List<ExternalApiSpecResponseDto> getExternalApiSpecsByDomainAndKeywordName(String domainName, String keywordName) {
        log.info("Getting external API specs by domain name: {} and keyword name: {}", domainName, keywordName);
        
        return externalApiSpecRepository.findByDomainNameAndKeywordNameWithAllRelations(domainName, keywordName).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 다중 도메인과 키워드 이름을 사용하여 외부 API 명세들 검색
     * 
     * 여러 도메인과 키워드 조합을 통해 API를 검색
     * 도메인과 키워드가 모두 제공된 경우 AND 조건으로 필터링
     * 
     * @param domains 도메인 이름 목록 (선택사항)
     * @param keywords 키워드 이름 목록 (선택사항)
     * @return 검색 조건에 매칭되는 API 명세 목록
     */
    public List<ExternalApiSpecResponseDto> searchExternalApiSpecsByNames(List<String> domains, List<String> keywords) {
        log.info("Searching external API specs with domains: {} and keywords: {}", domains, keywords);
        
        List<ExternalApiSpecResponseDto> results = new ArrayList<>();
        
        // 도메인과 키워드 모두 제공된 경우
        if (domains != null && !domains.isEmpty() && keywords != null && !keywords.isEmpty()) {
            for (String domain : domains) {
                for (String keyword : keywords) {
                    List<ExternalApiSpecResponseDto> specs = getExternalApiSpecsByDomainAndKeywordName(domain, keyword);
                    results.addAll(specs);
                }
            }
        }
        // 도메인만 제공된 경우
        else if (domains != null && !domains.isEmpty()) {
            for (String domain : domains) {
                List<ExternalApiSpecResponseDto> specs = getExternalApiSpecsByDomainName(domain);
                results.addAll(specs);
            }
        }
        // 키워드만 제공된 경우
        else if (keywords != null && !keywords.isEmpty()) {
            for (String keyword : keywords) {
                List<ExternalApiSpecResponseDto> specs = getExternalApiSpecsByKeywordName(keyword);
                results.addAll(specs);
            }
        }
        // 조건이 없는 경우 전체 활성 API 반환
        else {
            results = getAllActiveExternalApiSpecs();
        }
        
        // 중복 제거 (API ID 기준)
        return results.stream()
                .collect(Collectors.toMap(
                    ExternalApiSpecResponseDto::getApiId,
                    dto -> dto,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    /**
     * Gemini AI 분류 결과를 파싱하여 도메인 찾기/생성
     * 
     * @param classificationResult Gemini AI 분류 결과
     * @param existingDomains 기존 도메인 목록
     * @return 찾거나 생성된 ApiDomain
     */
    private ApiDomain findOrCreateDomain(String classificationResult, List<ApiDomain> existingDomains) {
        // 분류 결과에서 DOMAIN: 부분 추출
        String domainName = extractDomainFromResult(classificationResult);
        
        // 기존 도메인에서 찾기
        Optional<ApiDomain> existingDomain = existingDomains.stream()
            .filter(d -> d.getDomainName().equalsIgnoreCase(domainName))
            .findFirst();
            
        if (existingDomain.isPresent()) {
            return existingDomain.get();
        }
        
        // 새 도메인 생성
        ApiDomain newDomain = ApiDomain.builder()
            .domainName(domainName)
            .description(domainName + " 도메인")
            .build();
        
        return apiDomainRepository.save(newDomain);
    }
    
    /**
     * Gemini AI 분류 결과를 파싱하여 키워드 찾기/생성
     * 
     * @param classificationResult Gemini AI 분류 결과
     * @param existingKeywords 기존 키워드 목록
     * @param domain 소속 도메인
     * @return 찾거나 생성된 ApiKeyword
     */
    private ApiKeyword findOrCreateKeyword(String classificationResult, List<ApiKeyword> existingKeywords, ApiDomain domain) {
        // 분류 결과에서 KEYWORD: 부분 추출
        String keywordName = extractKeywordFromResult(classificationResult);
        
        // 기존 키워드에서 찾기
        Optional<ApiKeyword> existingKeyword = existingKeywords.stream()
            .filter(k -> k.getKeywordName().equalsIgnoreCase(keywordName))
            .findFirst();
            
        if (existingKeyword.isPresent()) {
            return existingKeyword.get();
        }
        
        // 새 키워드 생성
        ApiKeyword newKeyword = ApiKeyword.builder()
            .keywordName(keywordName)
            .description(keywordName + " 키워드")
            .domain(domain)
            .build();
        
        return apiKeywordRepository.save(newKeyword);
    }
    
    /**
     * Gemini AI 응답에서 도메인명 추출
     * 
     * @param result Gemini AI 응답
     * @return 추출된 도메인명
     */
    private String extractDomainFromResult(String result) {
        String[] lines = result.split("\n");
        for (String line : lines) {
            if (line.startsWith("DOMAIN:")) {
                return line.substring("DOMAIN:".length()).trim();
            }
        }
        return "Other"; // 기본값
    }
    
    /**
     * Gemini AI 응답에서 키워드명 추출
     * 
     * @param result Gemini AI 응답
     * @return 추출된 키워드명
     */
    private String extractKeywordFromResult(String result) {
        String[] lines = result.split("\n");
        for (String line : lines) {
            if (line.startsWith("KEYWORD:")) {
                return line.substring("KEYWORD:".length()).trim();
            }
        }
        return "general"; // 기본값
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

    /**
     * 새로 등록된 SGIS API의 accessToken 파라미터에 현재 토큰값 설정
     * 
     * SGIS 자격증명을 사용하는 API가 새로 등록될 때, 해당 API의 accessToken 파라미터들에
     * 현재 유효한 SGIS 토큰값을 default_value로 설정
     * 
     * 처리 과정:
     * 1. 파라미터 중 param_name이 "accessToken"인 것들을 필터링
     * 2. 현재 유효한 SGIS 토큰 조회
     * 3. 해당 파라미터들의 default_value를 토큰값으로 업데이트
     * 
     * @param parameters 새로 등록된 API의 파라미터 목록
     */
    private void updateSgisAccessTokenParameters(List<ApiParameter> parameters) {
        try {
            log.debug("Updating accessToken parameters for newly registered SGIS API");
            
            // accessToken 파라미터들만 필터링
            List<ApiParameter> accessTokenParams = parameters.stream()
                    .filter(param -> "accessToken".equals(param.getParamName()))
                    .collect(Collectors.toList());
            
            if (accessTokenParams.isEmpty()) {
                log.debug("No accessToken parameters found in newly registered SGIS API");
                return;
            }
            
            log.info("Found {} accessToken parameters in newly registered SGIS API", accessTokenParams.size());
            
            // 현재 유효한 SGIS 토큰 조회
            Optional<org.example.APIManagementSvc.domain.Entity.ApiToken> currentToken = 
                    sgisTokenService.getTokenStatus();
            
            if (currentToken.isEmpty() || !sgisTokenService.isTokenValid()) {
                log.warn("No valid SGIS token found. Attempting to issue new token for newly registered API.");
                try {
                    currentToken = Optional.of(sgisTokenService.issueToken());
                } catch (Exception e) {
                    log.error("Failed to issue SGIS token for newly registered API", e);
                    return;
                }
            }
            
            String tokenValue = currentToken.get().getAccessToken();
            
            // 각 accessToken 파라미터의 default_value 업데이트
            for (ApiParameter parameter : accessTokenParams) {
                String oldValue = parameter.getDefaultValue();
                parameter.setDefaultValue(tokenValue);
                
                log.debug("Updated accessToken parameter - API: {}, Parameter ID: {}, " +
                        "Old value length: {}, New value length: {}", 
                        parameter.getApiSpec().getApiId(),
                        parameter.getParameterId(),
                        oldValue != null ? oldValue.length() : 0,
                        tokenValue.length());
            }
            
            // 업데이트된 파라미터들 저장
            apiParameterRepository.saveAll(accessTokenParams);
            
            log.info("Successfully updated {} accessToken parameters for newly registered SGIS API", 
                    accessTokenParams.size());
            
        } catch (Exception e) {
            log.error("Failed to update accessToken parameters for newly registered SGIS API", e);
            // 파라미터 업데이트 실패가 API 등록 자체를 중단시키지는 않도록 예외를 던지지 않음
        }
    }
}