package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.repository.ExternalApiRepository;
import org.example.APIManagementSvc.repository.ApiKeyRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.APIManagementSvc.domain.Entity.ApiKey;

/**
 * API 토큰 자동 갱신 서비스
 * 4시간마다 만료된 토큰을 자동으로 재발급합니다.
 * 스케줄러 기반으로 동작하며 외부에서 수동 호출할 수 없습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiTokenRefreshService {

    private final ExternalApiRepository externalApiRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final RestTemplate restTemplate;

    // SGIS API 토큰 발급 URL
    private static final String SGIS_TOKEN_URL = "https://sgisapi.kostat.go.kr/OpenAPI3/auth/authentication.json";
    private static final int TOKEN_REFRESH_INTERVAL_HOURS = 4;
    private static final int TOKEN_WARNING_HOURS = 1; // 만료 1시간 전 경고

    /**
     * 4시간마다 토큰 갱신 스케줄링
     * 외부에서 호출할 수 없으며 스케줄러에 의해 자동 실행됩니다.
     */
    @Scheduled(fixedRate = TOKEN_REFRESH_INTERVAL_HOURS * 60 * 60 * 1000) // 4시간마다
    public void refreshExpiredTokens() {
        log.info("토큰 자동 갱신 작업 시작");
        
        try {
            // 토큰이 만료되었거나 곧 만료될 API들 조회
            List<ExternalApi> apisNeedingRefresh = findApisNeedingTokenRefresh();
            
            if (apisNeedingRefresh.isEmpty()) {
                log.info("갱신이 필요한 토큰이 없습니다.");
                return;
            }
            
            log.info("토큰 갱신이 필요한 API {}개 발견", apisNeedingRefresh.size());
            
            for (ExternalApi api : apisNeedingRefresh) {
                try {
                    refreshTokenForApi(api);
                } catch (Exception e) {
                    log.error("API {} 토큰 갱신 실패: {}", api.getApiName(), e.getMessage());
                }
            }
            
            log.info("토큰 자동 갱신 작업 완료");
            
        } catch (Exception e) {
            log.error("토큰 자동 갱신 작업 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 토큰 갱신이 필요한 API들 조회
     */
    private List<ExternalApi> findApisNeedingTokenRefresh() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningTime = now.plusHours(TOKEN_WARNING_HOURS);
        
        // 토큰이 만료되었거나 곧 만료될 API들 조회
        return externalApiRepository.findByTokenExpiresAtBeforeOrTokenExpiresAtIsNull(warningTime);
    }

    /**
     * 특정 API의 토큰 갱신
     */
    private void refreshTokenForApi(ExternalApi api) {
        log.info("API {} 토큰 갱신 시작", api.getApiName());
        
        if (!api.getAutoTokenRefresh()) {
            log.info("API {}는 자동 토큰 갱신이 비활성화되어 있습니다.", api.getApiName());
            return;
        }
        
        try {
            // API 서비스별로 다른 갱신 로직 적용
            if (api.isApiService("SGIS")) {
                refreshSgisToken(api);
            } else if (api.isApiService("KAKAO")) {
                refreshKakaoToken(api);
            } else if (api.isApiService("NAVER")) {
                refreshNaverToken(api);
            } else {
                // 일반적인 토큰 갱신 로직
                refreshGenericToken(api);
            }
            
            log.info("API {} 토큰 갱신 완료", api.getApiName());
            
        } catch (Exception e) {
            log.error("API {} 토큰 갱신 실패: {}", api.getApiName(), e.getMessage());
            throw e;
        }
    }

    /**
     * API 서비스별 토큰 갱신 로직 분기
     */
    private boolean isApiService(ExternalApi api, String serviceName) {
        return api.isApiService(serviceName);
    }

    /**
     * SGIS API 토큰 갱신
     */
    private void refreshSgisToken(ExternalApi api) {
        log.info("SGIS API 토큰 갱신: {}", api.getApiName());
        
        try {
            // API 키가 설정되어 있는지 확인
            if (!api.hasApiKey()) {
                log.warn("SGIS API {}에 API 키가 설정되어 있지 않습니다.", api.getApiName());
                throw new RuntimeException("API 키가 설정되지 않았습니다.");
            }
            
            ApiKey apiKey = api.getApiKey();
            
            // API 키가 활성 상태인지 확인
            if (!apiKey.isActive()) {
                log.warn("API 키가 비활성 상태입니다: {}", apiKey.getKeyId());
                throw new RuntimeException("API 키가 비활성 상태입니다.");
            }
            
            // 사용량 제한 확인
            if (apiKey.isDailyLimitExceeded()) {
                log.warn("일일 API 호출 제한에 도달했습니다: {}", apiKey.getKeyId());
                throw new RuntimeException("일일 API 호출 제한에 도달했습니다.");
            }
            
            if (apiKey.isMonthlyLimitExceeded()) {
                log.warn("월간 API 호출 제한에 도달했습니다: {}", apiKey.getKeyId());
                throw new RuntimeException("월간 API 호출 제한에 도달했습니다.");
            }
            
            // SGIS API 토큰 발급 요청
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey.getApiKey());
            
            Map<String, Object> requestBody = new HashMap<>();
            // SGIS API 요구사항에 맞는 요청 본문 구성
            // 실제 구현 시 SGIS API 문서를 참조하여 정확한 요청 형식 구성 필요
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(SGIS_TOKEN_URL, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // 응답에서 토큰 추출 (실제 응답 구조에 맞게 수정 필요)
                String newToken = extractTokenFromResponse(responseBody);
                LocalDateTime expiresAt = calculateTokenExpiry();
                
                if (newToken != null) {
                    // 토큰 업데이트
                    api.setApiToken(newToken, expiresAt);
                    externalApiRepository.save(api);
                    
                    // API 키 사용량 증가
                    apiKey.setCurrentDailyUsage(apiKey.getCurrentDailyUsage() + 1);
                    apiKey.setCurrentMonthlyUsage(apiKey.getCurrentMonthlyUsage() + 1);
                    apiKey.setLastUsedAt(LocalDateTime.now());
                    apiKeyRepository.save(apiKey);
                    
                    log.info("SGIS API {} 토큰 갱신 성공, 만료시간: {}", api.getApiName(), expiresAt);
                } else {
                    throw new RuntimeException("응답에서 토큰을 추출할 수 없습니다.");
                }
            } else {
                throw new RuntimeException("토큰 발급 요청 실패: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("SGIS API 토큰 갱신 실패: {}", e.getMessage(), e);
            throw new RuntimeException("SGIS API 토큰 갱신 실패: " + e.getMessage());
        }
    }

    /**
     * 카카오 API 토큰 갱신
     */
    private void refreshKakaoToken(ExternalApi api) {
        log.info("카카오 API 토큰 갱신: {}", api.getApiName());
        // 카카오 API 전용 토큰 갱신 로직 구현
        // 실제 구현 시 카카오 API 문서를 참조하여 구현 필요
        log.info("카카오 API {} 토큰 갱신 로직 구현 필요", api.getApiName());
    }

    /**
     * 네이버 API 토큰 갱신
     */
    private void refreshNaverToken(ExternalApi api) {
        log.info("네이버 API 토큰 갱신: {}", api.getApiName());
        // 네이버 API 전용 토큰 갱신 로직 구현
        // 실제 구현 시 네이버 API 문서를 참조하여 구현 필요
        log.info("네이버 API {} 토큰 갱신 로직 구현 필요", api.getApiName());
    }

    /**
     * 일반적인 API 토큰 갱신
     */
    private void refreshGenericToken(ExternalApi api) {
        log.info("일반 API 토큰 갱신: {}", api.getApiName());
        
        // 일반적인 토큰 갱신 로직
        // API별로 다른 갱식 방식이 필요할 수 있음
        
        // 예시: 기존 토큰을 사용하여 갱신 요청
        if (api.hasToken()) {
            // 토큰 갱신 엔드포인트 호출
            // 실제 구현 시 각 API의 토큰 갱신 방식에 맞게 구현 필요
            
            log.info("API {} 토큰 갱신 로직 구현 필요", api.getApiName());
        } else {
            log.warn("API {}에 토큰이 설정되어 있지 않습니다.", api.getApiName());
        }
    }

    /**
     * 응답에서 토큰 추출
     */
    private String extractTokenFromResponse(Map<String, Object> responseBody) {
        // SGIS API 응답 구조에 맞게 토큰 추출
        // 실제 응답 구조를 확인하여 정확한 경로로 수정 필요
        
        if (responseBody.containsKey("token")) {
            return (String) responseBody.get("token");
        } else if (responseBody.containsKey("access_token")) {
            return (String) responseBody.get("access_token");
        } else if (responseBody.containsKey("result") && responseBody.get("result") instanceof Map) {
            Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
            if (result.containsKey("token")) {
                return (String) result.get("token");
            }
        }
        
        return null;
    }

    /**
     * 토큰 만료 시간 계산 (4시간 후)
     */
    private LocalDateTime calculateTokenExpiry() {
        return LocalDateTime.now().plusHours(TOKEN_REFRESH_INTERVAL_HOURS);
    }
}
