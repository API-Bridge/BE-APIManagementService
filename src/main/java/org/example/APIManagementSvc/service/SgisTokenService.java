package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ApiCredential;
import org.example.APIManagementSvc.domain.Entity.ApiParameter;
import org.example.APIManagementSvc.domain.Entity.ApiToken;
import org.example.APIManagementSvc.repository.ApiCredentialRepository;
import org.example.APIManagementSvc.repository.ApiParameterRepository;
import org.example.APIManagementSvc.repository.ApiTokenRepository;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SGIS API 토큰 관리 서비스
 * 
 * 통계청 SGIS(Statistical Geographic Information Service) API의 액세스 토큰을 
 * 자동으로 발급받고 관리하는 비즈니스 로직을 제공
 * 
 * 주요 기능:
 * - SGIS API를 통한 액세스 토큰 발급
 * - 토큰 만료 시간 관리 및 자동 갱신
 * - 토큰 상태 조회 및 검증
 * - DB에 토큰 저장 및 업데이트
 * 
 * SGIS API 인증 방식:
 * - URL: https://sgisapi.kostat.go.kr/OpenAPI3/auth/authentication.json
 * - Method: GET
 * - Parameters: consumer_key, consumer_secret
 * - Response: JSON 형태로 accessToken 반환
 * 
 * 토큰 만료 정책:
 * - SGIS 토큰 유효시간: 약 4시간
 * - 갱신 주기: 3시간 30분 (안전 여유시간 확보)
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SgisTokenService {

    /** SGIS 자격증명 고정 ID */
    private static final String SGIS_CREDENTIAL_ID = "SGIS";
    
    /** SGIS 인증 API 엔드포인트 */
    private static final String SGIS_AUTH_URL = "https://sgisapi.kostat.go.kr/OpenAPI3/auth/authentication.json";
    
    /** 토큰 만료 시간: 4시간 (SGIS 정책) */
    private static final int TOKEN_EXPIRY_HOURS = 4;

    private final ApiCredentialRepository apiCredentialRepository;
    private final ApiTokenRepository apiTokenRepository;
    private final ApiParameterRepository apiParameterRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * SGIS 액세스 토큰 발급 및 저장
     * 
     * SGIS 자격증명을 사용하여 새로운 액세스 토큰을 발급받고 DB에 저장
     * 기존 토큰이 있는 경우 업데이트, 없는 경우 신규 생성
     * 
     * 처리 과정:
     * 1. SGIS 자격증명 조회 및 검증
     * 2. SGIS API 호출하여 토큰 발급
     * 3. 응답 JSON 파싱하여 accessToken 추출
     * 4. 토큰 만료시간 계산 (현재시간 + 4시간)
     * 5. DB에 토큰 저장 또는 업데이트
     * 
     * @return ApiToken 발급받은 토큰 정보
     * @throws IllegalStateException SGIS 자격증명이 없거나 비활성화된 경우
     * @throws RuntimeException 토큰 발급 실패 시
     */
    @Transactional
    public ApiToken issueToken() {
        log.info("Starting SGIS token issuance process");
        
        // SGIS 자격증명 조회
        ApiCredential sgisCredential = getSgisCredential();
        
        // SGIS API 호출하여 토큰 발급
        String accessToken = requestAccessTokenFromSgis(sgisCredential);
        
        // 토큰 만료시간 계산
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);
        
        // 기존 토큰 확인 및 처리
        Optional<ApiToken> existingToken = apiTokenRepository.findByCredentialId(SGIS_CREDENTIAL_ID);
        
        ApiToken tokenToSave;
        if (existingToken.isPresent()) {
            // 기존 토큰 업데이트
            tokenToSave = existingToken.get();
            tokenToSave.setAccessToken(accessToken);
            tokenToSave.setExpiresAt(expiresAt);
            tokenToSave.setTokenType("Bearer");
            
            log.info("Updated existing SGIS token with new access token");
        } else {
            // 새 토큰 생성
            tokenToSave = ApiToken.builder()
                    .credential(sgisCredential)
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .expiresAt(expiresAt)
                    .build();
                    
            log.info("Created new SGIS token");
        }
        
        // DB 저장
        ApiToken savedToken = apiTokenRepository.save(tokenToSave);
        
        // SGIS API들의 accessToken 파라미터들 업데이트
        updateAccessTokenParameters(accessToken);
        
        log.info("Successfully issued SGIS token. Expires at: {}", expiresAt);
        return savedToken;
    }

    /**
     * SGIS 토큰 상태 조회
     * 
     * 현재 저장된 SGIS 토큰의 상태를 조회하여 반환
     * 토큰 존재 여부, 만료 여부, 남은 유효시간 등을 확인할 수 있음
     * 
     * @return Optional<ApiToken> SGIS 토큰 정보 (없으면 Optional.empty())
     */
    public Optional<ApiToken> getTokenStatus() {
        log.debug("Retrieving SGIS token status");
        
        Optional<ApiToken> token = apiTokenRepository.findByCredentialId(SGIS_CREDENTIAL_ID);
        
        if (token.isPresent()) {
            ApiToken tokenInfo = token.get();
            boolean isExpired = tokenInfo.getExpiresAt().isBefore(LocalDateTime.now());
            
            log.info("SGIS token found - Expires at: {}, Is expired: {}", 
                    tokenInfo.getExpiresAt(), isExpired);
        } else {
            log.info("No SGIS token found in database");
        }
        
        return token;
    }

    /**
     * SGIS 토큰 유효성 검증
     * 
     * 현재 저장된 SGIS 토큰이 유효한지 확인
     * 토큰이 존재하고 만료되지 않았는지를 검증
     * 
     * @return boolean 토큰 유효 여부 (true: 유효, false: 만료되었거나 없음)
     */
    public boolean isTokenValid() {
        Optional<ApiToken> token = apiTokenRepository.findValidTokenByCredentialId(
                SGIS_CREDENTIAL_ID, LocalDateTime.now());
        
        boolean isValid = token.isPresent();
        log.debug("SGIS token validation result: {}", isValid);
        
        return isValid;
    }

    /**
     * SGIS 토큰 강제 갱신
     * 
     * 현재 토큰의 만료 여부와 관계없이 새로운 토큰을 강제로 발급
     * 관리자가 수동으로 토큰을 갱신할 때 사용
     * 
     * @return ApiToken 새로 발급된 토큰 정보
     */
    @Transactional
    public ApiToken forceRefreshToken() {
        log.info("Force refreshing SGIS token");
        return issueToken();
    }

    /**
     * SGIS 자격증명 조회 및 검증
     * 
     * DB에서 SGIS 자격증명을 조회하고 유효성을 검증
     * 자격증명이 존재하지 않거나 비활성화된 경우 예외 발생
     * 
     * @return ApiCredential 검증된 SGIS 자격증명
     * @throws IllegalStateException 자격증명이 없거나 비활성화된 경우
     */
    private ApiCredential getSgisCredential() {
        Optional<ApiCredential> credential = apiCredentialRepository.findById(SGIS_CREDENTIAL_ID);
        
        if (credential.isEmpty()) {
            throw new IllegalStateException("SGIS 자격증명이 등록되지 않았습니다. credential_id: " + SGIS_CREDENTIAL_ID);
        }
        
        ApiCredential sgisCredential = credential.get();
        if (sgisCredential.getStatus() != ApiCredential.CredentialStatus.ACTIVE) {
            throw new IllegalStateException("SGIS 자격증명이 비활성화 상태입니다. 상태: " + sgisCredential.getStatus());
        }
        
        if (sgisCredential.getApiKey() == null || sgisCredential.getSecretKey() == null) {
            throw new IllegalStateException("SGIS 자격증명의 API 키 또는 Secret 키가 누락되었습니다.");
        }
        
        log.debug("SGIS credential validation successful");
        return sgisCredential;
    }

    /**
     * SGIS API 호출하여 액세스 토큰 요청
     * 
     * SGIS 인증 API에 HTTP GET 요청을 보내 액세스 토큰을 발급받음
     * JavaScript 코드와 동일한 방식으로 consumer_key, consumer_secret을 파라미터로 전송
     * 
     * @param credential SGIS 자격증명 정보
     * @return String 발급받은 액세스 토큰
     * @throws RuntimeException API 호출 실패 또는 응답 파싱 실패 시
     */
    private String requestAccessTokenFromSgis(ApiCredential credential) {
        try {
            log.debug("Requesting access token from SGIS API");
            
            // URL 빌드 (쿼리 파라미터 포함)
            String requestUrl = UriComponentsBuilder.fromHttpUrl(SGIS_AUTH_URL)
                    .queryParam("consumer_key", credential.getApiKey())
                    .queryParam("consumer_secret", credential.getSecretKey())
                    .build()
                    .toUriString();
            
            log.debug("SGIS API request URL: {}", requestUrl.replace(credential.getSecretKey(), "***"));
            
            // HTTP GET 요청 실행
            ResponseEntity<String> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    null,
                    String.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("SGIS API 호출 실패. HTTP 상태: " + response.getStatusCode());
            }
            
            // JSON 응답 파싱
            String responseBody = response.getBody();
            log.debug("SGIS API response received (length: {})", responseBody != null ? responseBody.length() : 0);
            
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            // 응답 구조 검증
            if (!jsonResponse.has("result")) {
                log.error("Invalid SGIS API response structure: {}", responseBody);
                throw new RuntimeException("SGIS API 응답에 result 필드가 없습니다.");
            }
            
            JsonNode result = jsonResponse.get("result");
            if (!result.has("accessToken")) {
                log.error("Missing accessToken in SGIS API response: {}", responseBody);
                throw new RuntimeException("SGIS API 응답에 accessToken이 없습니다.");
            }
            
            String accessToken = result.get("accessToken").asText();
            
            if (accessToken == null || accessToken.trim().isEmpty()) {
                throw new RuntimeException("SGIS API에서 빈 액세스 토큰을 반환했습니다.");
            }
            
            log.info("Successfully received access token from SGIS API (length: {})", accessToken.length());
            return accessToken;
            
        } catch (Exception e) {
            log.error("Failed to request access token from SGIS API", e);
            throw new RuntimeException("SGIS 액세스 토큰 발급 실패: " + e.getMessage(), e);
        }
    }

    /**
     * SGIS 토큰 만료 임박 확인
     * 
     * 현재 토큰이 지정된 시간 내에 만료되는지 확인
     * 스케줄러에서 토큰 갱신이 필요한지 판단하는 데 사용
     * 
     * @param minutesBeforeExpiry 만료 전 확인할 시간 (분)
     * @return boolean 만료 임박 여부
     */
    public boolean isTokenExpiringWithin(int minutesBeforeExpiry) {
        Optional<ApiToken> token = apiTokenRepository.findByCredentialId(SGIS_CREDENTIAL_ID);
        
        if (token.isEmpty()) {
            log.debug("No SGIS token found - considering as expiring");
            return true;
        }
        
        LocalDateTime threshold = LocalDateTime.now().plusMinutes(minutesBeforeExpiry);
        boolean isExpiring = token.get().getExpiresAt().isBefore(threshold);
        
        log.debug("SGIS token expiring within {} minutes: {}", minutesBeforeExpiry, isExpiring);
        return isExpiring;
    }

    /**
     * SGIS API들의 accessToken 파라미터들의 default_value 업데이트
     * 
     * SGIS 자격증명을 사용하는 모든 외부 API들 중에서 param_name이 "accessToken"인 
     * 파라미터들을 찾아 default_value를 새로운 토큰값으로 업데이트
     * 
     * 처리 과정:
     * 1. SGIS 자격증명을 사용하는 API들의 accessToken 파라미터 조회
     * 2. 각 파라미터의 default_value를 새로운 토큰값으로 업데이트
     * 3. 업데이트된 파라미터들을 DB에 저장
     * 
     * @param accessToken 새로 발급받은 SGIS 액세스 토큰
     */
    private void updateAccessTokenParameters(String accessToken) {
        try {
            log.debug("Updating accessToken parameters for SGIS APIs");
            
            // SGIS 자격증명을 사용하는 API들의 accessToken 파라미터들 조회
            List<ApiParameter> accessTokenParams = apiParameterRepository
                    .findByApiSpec_Credential_CredentialIdAndParamName(SGIS_CREDENTIAL_ID, "accessToken");
            
            if (accessTokenParams.isEmpty()) {
                log.info("No accessToken parameters found for SGIS APIs");
                return;
            }
            
            log.info("Found {} accessToken parameters for SGIS APIs", accessTokenParams.size());
            
            // 각 파라미터의 default_value를 새로운 토큰값으로 업데이트
            for (ApiParameter parameter : accessTokenParams) {
                String oldValue = parameter.getDefaultValue();
                parameter.setDefaultValue(accessToken);
                
                log.debug("Updated accessToken parameter - API: {}, Parameter ID: {}, " +
                        "Old value length: {}, New value length: {}", 
                        parameter.getApiSpec().getApiId(),
                        parameter.getParameterId(),
                        oldValue != null ? oldValue.length() : 0,
                        accessToken.length());
            }
            
            // 업데이트된 파라미터들 일괄 저장
            apiParameterRepository.saveAll(accessTokenParams);
            
            log.info("Successfully updated {} accessToken parameters with new SGIS token", 
                    accessTokenParams.size());
            
        } catch (Exception e) {
            log.error("Failed to update accessToken parameters for SGIS APIs", e);
            // 파라미터 업데이트 실패가 토큰 발급 자체를 중단시키지는 않도록 예외를 던지지 않음
        }
    }
}