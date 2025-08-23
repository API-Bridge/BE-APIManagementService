package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ApiKey;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;
import org.example.APIManagementSvc.repository.ApiKeyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API 키 관리 서비스
 * 
 * 이 서비스는 외부 API 서비스의 API 키를 체계적으로 관리하는 핵심 비즈니스 로직을 담당합니다.
 * 
 * 주요 기능:
 * - API 키 등록 및 관리
 * - API 키 정보 조회 및 검색
 * - API 키 상태 관리 (활성/비활성/만료/폐기)
 * - 사용량 모니터링 및 제한 관리
 * - API 키 갱신 및 폐기
 * - 통계 정보 제공
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    /**
     * API 키 등록
     */
    public ApiKey registerApiKey(String organizationName, String organizationCode,
                               String contactEmail, String contactPhone,
                               String apiServiceName, String apiServiceUrl,
                               String apiKey, String secretKey,
                               Integer dailyLimit, Integer monthlyLimit,
                               LocalDateTime expiresAt, String description,
                               String requestedApis) {
        
        log.info("새로운 API 키 등록 요청: Organization={}, Service={}", organizationName, apiServiceName);

        try {
            // 필수 필드 검증
            if (organizationName == null || organizationName.trim().isEmpty()) {
                throw new IllegalArgumentException("기관명은 필수입니다.");
            }
            if (contactEmail == null || contactEmail.trim().isEmpty()) {
                throw new IllegalArgumentException("연락처 이메일은 필수입니다.");
            }
            if (apiServiceName == null || apiServiceName.trim().isEmpty()) {
                throw new IllegalArgumentException("API 서비스명은 필수입니다.");
            }
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException("API 키는 필수입니다.");
            }

            // 고유한 keyId 생성
            String keyId = generateUniqueKeyId(organizationName, apiServiceName);

            // API 키 엔티티 생성
            ApiKey newApiKey = ApiKey.builder()
                    .keyId(keyId)
                    .organizationName(organizationName.trim())
                    .organizationCode(organizationCode != null ? organizationCode.trim() : null)
                    .contactEmail(contactEmail.trim())
                    .contactPhone(contactPhone != null ? contactPhone.trim() : null)
                    .apiServiceName(apiServiceName.trim())
                    .apiServiceUrl(apiServiceUrl != null ? apiServiceUrl.trim() : null)
                    .apiKey(apiKey.trim())
                    .secretKey(secretKey != null ? secretKey.trim() : null)
                    .dailyLimit(dailyLimit)
                    .monthlyLimit(monthlyLimit)
                    .expiresAt(expiresAt)
                    .status(ApiKeyStatus.ACTIVE)
                    .description(description != null ? description.trim() : null)
                    .requestedApis(requestedApis != null ? requestedApis.trim() : null)
                    .build();

            ApiKey savedApiKey = apiKeyRepository.save(newApiKey);
            log.info("API 키 등록 완료: Key ID={}, Organization={}, Service={}", 
                    savedApiKey.getKeyId(), savedApiKey.getOrganizationName(), savedApiKey.getApiServiceName());

            return savedApiKey;

        } catch (Exception e) {
            log.error("API 키 등록 실패: {}", e.getMessage(), e);
            throw new RuntimeException("API 키 등록에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 고유한 keyId 생성
     */
    private String generateUniqueKeyId(String organizationName, String apiServiceName) {
        String baseKeyId = organizationName.replaceAll("[^A-Za-z0-9]", "").toUpperCase() + "_" + 
                          apiServiceName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        String uniqueKeyId = baseKeyId + "_" + UUID.randomUUID().toString().substring(0, 8);
        
        // 중복 확인
        int counter = 1;
        while (apiKeyRepository.findByKeyIdAndDeletedFalse(uniqueKeyId).isPresent()) {
            uniqueKeyId = baseKeyId + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + counter++;
        }
        
        return uniqueKeyId;
    }

    /**
     * API 키 조회
     */
    @Transactional(readOnly = true)
    public Optional<ApiKey> getApiKey(String keyId) {
        return apiKeyRepository.findByKeyIdAndDeletedFalse(keyId);
    }

    /**
     * 조직명으로 API 키 목록 조회
     */
    public List<ApiKey> getApiKeysByOrganization(String organizationName) {
        log.info("Getting API keys by organization: {}", organizationName);
        return apiKeyRepository.findByOrganizationNameAndDeletedFalse(organizationName);
    }

    /**
     * API 키 값으로 조회
     */
    @Transactional(readOnly = true)
    public Optional<ApiKey> getApiKeyByApiKey(String apiKey) {
        return apiKeyRepository.findByApiKeyAndDeletedFalse(apiKey);
    }

    /**
     * 모든 활성 API 키 조회
     */
    @Transactional(readOnly = true)
    public List<ApiKey> getAllActiveApiKeys() {
        return apiKeyRepository.findByStatusAndDeletedFalse(ApiKeyStatus.ACTIVE);
    }

    /**
     * 모든 API 키 조회 (페이지네이션)
     */
    public Page<ApiKey> getAllApiKeys(Pageable pageable) {
        log.info("Getting all API keys with pagination: page {}, size {}", pageable.getPageNumber(), pageable.getPageSize());
        return apiKeyRepository.findByDeletedFalse(pageable);
    }

    /**
     * API 키 수정
     */
    @Transactional
    public ApiKey updateApiKey(String keyId, ApiKey updateData) {
        log.info("API 키 수정: {}", keyId);
        
        ApiKey existingApiKey = apiKeyRepository.findByKeyIdAndDeletedFalse(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));

        // 업데이트 가능한 필드들 수정
        if (updateData.getOrganizationName() != null) {
            existingApiKey.setOrganizationName(updateData.getOrganizationName());
        }
        if (updateData.getContactEmail() != null) {
            existingApiKey.setContactEmail(updateData.getContactEmail());
        }
        if (updateData.getContactPhone() != null) {
            existingApiKey.setContactPhone(updateData.getContactPhone());
        }
        if (updateData.getApiServiceUrl() != null) {
            existingApiKey.setApiServiceUrl(updateData.getApiServiceUrl());
        }
        if (updateData.getDailyLimit() != null) {
            existingApiKey.setDailyLimit(updateData.getDailyLimit());
        }
        if (updateData.getMonthlyLimit() != null) {
            existingApiKey.setMonthlyLimit(updateData.getMonthlyLimit());
        }
        if (updateData.getExpiresAt() != null) {
            existingApiKey.setExpiresAt(updateData.getExpiresAt());
        }
        if (updateData.getDescription() != null) {
            existingApiKey.setDescription(updateData.getDescription());
        }
        if (updateData.getRequestedApis() != null) {
            existingApiKey.setRequestedApis(updateData.getRequestedApis());
        }

        existingApiKey.setUpdatedAt(LocalDateTime.now());
        
        return apiKeyRepository.save(existingApiKey);
    }

    /**
     * API 키 상태 변경
     */
    @Transactional
    public ApiKey updateApiKeyStatus(String keyId, ApiKeyStatus newStatus) {
        log.info("API 키 상태 변경: {} -> {}", keyId, newStatus);
        
        ApiKey apiKey = apiKeyRepository.findByKeyIdAndDeletedFalse(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));

        apiKey.setStatus(newStatus);
        apiKey.setUpdatedAt(LocalDateTime.now());
        
        return apiKeyRepository.save(apiKey);
    }

    /**
     * API 키 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteApiKey(String keyId) {
        log.info("API 키 삭제 (soft): {}", keyId);
        
        ApiKey apiKey = apiKeyRepository.findByKeyIdAndDeletedFalse(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));

        apiKey.setDeleted(true);
        apiKey.setUpdatedAt(LocalDateTime.now());
        
        apiKeyRepository.save(apiKey);
    }

    /**
     * API 키 사용량 증가
     */
    @Transactional
    public void incrementUsage(String keyId) {
        ApiKey apiKey = apiKeyRepository.findByKeyIdAndDeletedFalse(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));

        // 일일 사용량 증가
        if (apiKey.getCurrentDailyUsage() != null) {
            apiKey.setCurrentDailyUsage(apiKey.getCurrentDailyUsage() + 1);
        }
        
        // 월간 사용량 증가
        if (apiKey.getCurrentMonthlyUsage() != null) {
            apiKey.setCurrentMonthlyUsage(apiKey.getCurrentMonthlyUsage() + 1);
        }
        
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKey.setUpdatedAt(LocalDateTime.now());
        
        apiKeyRepository.save(apiKey);
    }

    /**
     * 매일 자정에 일일 사용량 초기화
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyUsage() {
        log.info("일일 사용량 초기화 시작");
        
        List<ApiKey> apiKeys = apiKeyRepository.findAll().stream()
                .filter(key -> !key.getDeleted())
                .toList();
        for (ApiKey apiKey : apiKeys) {
            if (apiKey.getCurrentDailyUsage() != null && apiKey.getCurrentDailyUsage() > 0) {
                apiKey.setCurrentDailyUsage(0);
                apiKey.setUpdatedAt(LocalDateTime.now());
                apiKeyRepository.save(apiKey);
            }
        }
        
        log.info("일일 사용량 초기화 완료");
    }

    /**
     * 매월 1일 자정에 월간 사용량 초기화
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void resetMonthlyUsage() {
        log.info("월간 사용량 초기화 시작");
        
        List<ApiKey> apiKeys = apiKeyRepository.findAll().stream()
                .filter(key -> !key.getDeleted())
                .toList();
        for (ApiKey apiKey : apiKeys) {
            if (apiKey.getCurrentMonthlyUsage() != null && apiKey.getCurrentMonthlyUsage() > 0) {
                apiKey.setCurrentMonthlyUsage(0);
                apiKey.setUpdatedAt(LocalDateTime.now());
                apiKeyRepository.save(apiKey);
            }
        }
        
        log.info("월간 사용량 초기화 완료");
    }
}
