package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.ExternalApi;
import org.example.APIManagementSvc.domain.ApiParameter;
import org.example.APIManagementSvc.repository.ExternalApiRepository;
import org.example.APIManagementSvc.repository.ApiParameterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 외부 API 메타데이터 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExternalApiService {

    private final ExternalApiRepository externalApiRepository;
    private final ApiParameterRepository apiParameterRepository;

    /**
     * API 생성
     */
    @Transactional
    public ExternalApi createApi(ExternalApi api) {
        log.info("Creating new API: {}", api.getApiName());
        
        // API ID 자동 생성
        api.setApiId(UUID.randomUUID().toString());
        api.setCreatedAt(LocalDateTime.now());
        api.setUpdatedAt(LocalDateTime.now());
        api.setDeleted(false);
        
        ExternalApi savedApi = externalApiRepository.save(api);
        log.info("API created successfully with ID: {}", savedApi.getApiId());
        
        return savedApi;
    }

    /**
     * API 조회 (ID로)
     */
    public Optional<ExternalApi> getApiById(String apiId) {
        log.debug("Fetching API by ID: {}", apiId);
        return externalApiRepository.findById(apiId);
    }

    /**
     * API 조회 (이름으로)
     */
    public Optional<ExternalApi> getApiByName(String apiName) {
        log.debug("Fetching API by name: {}", apiName);
        return externalApiRepository.findByApiName(apiName);
    }

    /**
     * 모든 활성 API 조회
     */
    public List<ExternalApi> getAllActiveApis() {
        log.debug("Fetching all active APIs");
        return externalApiRepository.findByDeletedFalse();
    }

    /**
     * 도메인별 API 조회
     */
    public List<ExternalApi> getApisByDomain(String domain) {
        log.debug("Fetching APIs by domain: {}", domain);
        return externalApiRepository.findByApiDomainAndDeletedFalse(domain);
    }

    /**
     * 키워드별 API 조회
     */
    public List<ExternalApi> getApisByKeyword(String keyword) {
        log.debug("Fetching APIs by keyword: {}", keyword);
        return externalApiRepository.findByApiKeywordAndDeletedFalse(keyword);
    }

    /**
     * 소유자별 API 조회
     */
    public List<ExternalApi> getApisByOwner(String ownerId) {
        log.debug("Fetching APIs by owner: {}", ownerId);
        return externalApiRepository.findByApiOwnerAndDeletedFalse(ownerId);
    }

    /**
     * API 업데이트
     */
    @Transactional
    public ExternalApi updateApi(String apiId, ExternalApi updateData) {
        log.info("Updating API: {}", apiId);
        
        Optional<ExternalApi> existingApi = externalApiRepository.findById(apiId);
        if (existingApi.isEmpty()) {
            throw new IllegalArgumentException("API not found with ID: " + apiId);
        }
        
        ExternalApi api = existingApi.get();
        
        // 업데이트 가능한 필드들만 수정
        if (updateData.getApiName() != null) {
            api.setApiName(updateData.getApiName());
        }
        if (updateData.getApiUrl() != null) {
            api.setApiUrl(updateData.getApiUrl());
        }
        if (updateData.getApiIssuer() != null) {
            api.setApiIssuer(updateData.getApiIssuer());
        }
        if (updateData.getApiDescription() != null) {
            api.setApiDescription(updateData.getApiDescription());
        }
        if (updateData.getApiDomain() != null) {
            api.setApiDomain(updateData.getApiDomain());
        }
        if (updateData.getApiKeyword() != null) {
            api.setApiKeyword(updateData.getApiKeyword());
        }
        if (updateData.getHttpMethod() != null) {
            api.setHttpMethod(updateData.getHttpMethod());
        }
        if (updateData.getApiEffectiveness() != null) {
            api.setApiEffectiveness(updateData.getApiEffectiveness());
        }
        
        api.setUpdatedAt(LocalDateTime.now());
        
        ExternalApi updatedApi = externalApiRepository.save(api);
        log.info("API updated successfully: {}", apiId);
        
        return updatedApi;
    }

    /**
     * API 상태 업데이트 (유효성)
     */
    @Transactional
    public ExternalApi updateApiEffectiveness(String apiId, boolean effectiveness) {
        log.info("Updating API effectiveness: {} to {}", apiId, effectiveness);
        
        Optional<ExternalApi> existingApi = externalApiRepository.findById(apiId);
        if (existingApi.isEmpty()) {
            throw new IllegalArgumentException("API not found with ID: " + apiId);
        }
        
        ExternalApi api = existingApi.get();
        api.setApiEffectiveness(effectiveness);
        api.setUpdatedAt(LocalDateTime.now());
        
        ExternalApi updatedApi = externalApiRepository.save(api);
        log.info("API effectiveness updated: {} -> {}", apiId, effectiveness);
        
        return updatedApi;
    }

    /**
     * API 소프트 삭제
     */
    @Transactional
    public void deleteApi(String apiId) {
        log.info("Soft deleting API: {}", apiId);
        
        Optional<ExternalApi> existingApi = externalApiRepository.findById(apiId);
        if (existingApi.isEmpty()) {
            throw new IllegalArgumentException("API not found with ID: " + apiId);
        }
        
        ExternalApi api = existingApi.get();
        api.setDeleted(true);
        api.setUpdatedAt(LocalDateTime.now());
        
        externalApiRepository.save(api);
        log.info("API soft deleted: {}", apiId);
    }

    /**
     * API 완전 삭제 (하드 삭제)
     */
    @Transactional
    public void hardDeleteApi(String apiId) {
        log.info("Hard deleting API: {}", apiId);
        
        // API 파라미터들도 함께 삭제
        List<ApiParameter> parameters = apiParameterRepository.findByApiId(apiId);
        apiParameterRepository.deleteAll(parameters);
        
        externalApiRepository.deleteById(apiId);
        log.info("API hard deleted: {}", apiId);
    }

    /**
     * API 검색 (이름, 설명, 도메인, 키워드로)
     */
    public List<ExternalApi> searchApis(String searchTerm) {
        log.debug("Searching APIs with term: {}", searchTerm);
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllActiveApis();
        }
        
        String term = searchTerm.toLowerCase().trim();
        return externalApiRepository.findBySearchTermAndDeletedFalse(term);
    }

    /**
     * API 통계 정보
     */
    public ApiStatistics getApiStatistics() {
        log.debug("Fetching API statistics");
        
        long totalApis = externalApiRepository.count();
        long activeApis = externalApiRepository.countByDeletedFalse();
        long effectiveApis = externalApiRepository.countByApiEffectivenessTrueAndDeletedFalse();
        
        return ApiStatistics.builder()
                .totalApis(totalApis)
                .activeApis(activeApis)
                .effectiveApis(effectiveApis)
                .inactiveApis(totalApis - activeApis)
                .ineffectiveApis(activeApis - effectiveApis)
                .build();
    }

    /**
     * API 통계 정보 DTO
     */
    public static class ApiStatistics {
        private final long totalApis;
        private final long activeApis;
        private final long effectiveApis;
        private final long inactiveApis;
        private final long ineffectiveApis;
        
        // Builder 패턴
        public static Builder builder() {
            return new Builder();
        }
        
        private ApiStatistics(Builder builder) {
            this.totalApis = builder.totalApis;
            this.activeApis = builder.activeApis;
            this.effectiveApis = builder.effectiveApis;
            this.inactiveApis = builder.inactiveApis;
            this.ineffectiveApis = builder.ineffectiveApis;
        }
        
        // Getters
        public long getTotalApis() { return totalApis; }
        public long getActiveApis() { return activeApis; }
        public long getEffectiveApis() { return effectiveApis; }
        public long getInactiveApis() { return inactiveApis; }
        public long getIneffectiveApis() { return ineffectiveApis; }
        
        public static class Builder {
            private long totalApis;
            private long activeApis;
            private long effectiveApis;
            private long inactiveApis;
            private long ineffectiveApis;
            
            public Builder totalApis(long totalApis) {
                this.totalApis = totalApis;
                return this;
            }
            
            public Builder activeApis(long activeApis) {
                this.activeApis = activeApis;
                return this;
            }
            
            public Builder effectiveApis(long effectiveApis) {
                this.effectiveApis = effectiveApis;
                return this;
            }
            
            public Builder inactiveApis(long inactiveApis) {
                this.inactiveApis = inactiveApis;
                return this;
            }
            
            public Builder ineffectiveApis(long ineffectiveApis) {
                this.ineffectiveApis = ineffectiveApis;
                return this;
            }
            
            public ApiStatistics build() {
                return new ApiStatistics(this);
            }
        }
    }
}

