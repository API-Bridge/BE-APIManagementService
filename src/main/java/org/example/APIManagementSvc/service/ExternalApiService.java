package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import org.example.APIManagementSvc.dto.externalapi.ApiStatisticsResponse;
import org.example.APIManagementSvc.repository.ExternalApiRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 외부 API 메타데이터 관리 서비스
 * API 등록, 조회, 수정, 삭제 등의 기본 CRUD 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExternalApiService {

    private final ExternalApiRepository externalApiRepository;

    /**
     * API 등록 (기존 외부 API를 시스템에 등록)
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
        
        // API 키는 선택사항이지만, 설정된 경우 유효성 검증
        if (api.getApiKey() != null) {
            // ApiKey 엔티티가 설정되어 있는지 확인
            throw new IllegalArgumentException("API key validation not implemented for ApiKey entity");
        }
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
        
        // 인증 정보 설정
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            // API 키는 별도로 관리되므로 여기서는 설정하지 않음
            log.info("API key provided but not set in ExternalApi entity");
        }
        
        // 토큰 설정 (토큰 만료 시간이 이미 설정된 경우 그대로 사용)
        if (apiToken != null && !apiToken.trim().isEmpty()) {
            if (api.getTokenExpiresAt() == null) {
                // 토큰 만료 시간이 설정되지 않은 경우 4시간 후로 설정
                LocalDateTime tokenExpiresAt = LocalDateTime.now().plusHours(4);
                api.setTokenExpiresAt(tokenExpiresAt);
                log.info("토큰 만료 시간 자동 설정: {}", tokenExpiresAt);
            }
            log.info("API 토큰 설정 완료");
        }
        
        return externalApiRepository.save(api);
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
     * 모든 활성 API 조회
     */
    public List<ExternalApi> getAllActiveApis() {
        log.debug("Getting all active APIs");
        return externalApiRepository.findByDeletedFalse();
    }

    /**
     * API 수정
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
     * API 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteApi(String apiId) {
        log.info("Deleting API (soft): {}", apiId);
        
        ExternalApi api = externalApiRepository.findByApiId(apiId)
                .filter(a -> !a.getDeleted())
                .orElseThrow(() -> new IllegalArgumentException("API not found: " + apiId));

        api.setDeleted(true);
        api.setUpdatedAt(LocalDateTime.now());
        
        externalApiRepository.save(api);
    }

    /**
     * API 완전 삭제 (하드 삭제)
     */
    @Transactional
    public void hardDeleteApi(String apiId) {
        log.warn("Hard deleting API: {}", apiId);
        
        ExternalApi api = externalApiRepository.findByApiId(apiId)
                .orElseThrow(() -> new IllegalArgumentException("API not found: " + apiId));

        externalApiRepository.delete(api);
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
}
