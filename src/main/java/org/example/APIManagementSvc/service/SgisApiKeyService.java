package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.SgisApiKey;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;
import org.example.APIManagementSvc.repository.SgisApiKeyRepository;
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
 * SGIS API 키 관리 서비스
 * 
 * 이 서비스는 SGIS(Statistical Geographic Information Service) 공공데이터포털의
 * API 키를 체계적으로 관리하는 핵심 비즈니스 로직을 담당합니다.
 * 
 * 주요 기능:
 * - 이미 발급받은 API 키 등록 및 관리
 * - API 키 정보 조회 및 검색
 * - API 키 상태 관리 (활성/비활성/만료/폐기)
 * - 사용량 모니터링 및 제한 관리
 * - API 키 갱신 및 폐기
 * - 통계 정보 제공
 * - 토큰 갱신 스케줄링
 * 
 * 보안 고려사항:
 * - API 키와 시크릿 키는 로그에 출력하지 않음
 * - 모든 작업은 적절한 권한 검증 후 수행
 * - 민감한 정보는 암호화하여 저장
 * 
 * 트랜잭션 관리:
 * - 읽기 전용 메서드는 @Transactional(readOnly = true) 적용
 * - 데이터 수정 메서드는 기본 트랜잭션 설정 사용
 * - 롤백은 RuntimeException 발생 시 자동 처리
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SgisApiKeyService {

    private final SgisApiKeyRepository sgisApiKeyRepository;

    /**
     * 이미 발급받은 SGIS API 키를 시스템에 등록
     * 
     * 이 메서드는 기관이 SGIS 공공데이터포털에서 직접 발급받은 API 키를
     * 우리 시스템에 등록하여 관리하기 위한 것입니다.
     * 
     * 처리 과정:
     * 1. 입력받은 키 정보로 SgisApiKey 엔티티 생성
     * 2. 고유한 keyId 자동 생성
     * 3. 데이터베이스에 저장 및 로그 기록
     * 
     * @param organizationName 기관명 (필수)
     * @param organizationCode 기관 코드 (선택)
     * @param contactEmail 연락처 이메일 (필수)
     * @param contactPhone 연락처 전화번호 (선택)
     * @param apiKey SGIS에서 발급받은 실제 API 키 (필수)
     * @param secretKey SGIS에서 발급받은 시크릿 키 (필수)
     * @param dailyLimit 일일 API 호출 제한 횟수 (필수)
     * @param monthlyLimit 월간 API 호출 제한 횟수 (필수)
     * @param expiresAt API 키 만료일시 (필수)
     * @param description API 키 설명 (선택)
     * @param requestedApis 요청한 API 목록 (선택)
     * 
     * @return 등록된 API 키 정보가 담긴 SgisApiKey 엔티티
     * 
     * @throws RuntimeException 필수 필드 누락 또는 데이터베이스 저장 실패 시
     * 
     * 사용 예시:
     * SgisApiKey apiKey = sgisApiKeyService.registerApiKey(
     *     "서울시청", "SEOUL001", "admin@seoul.go.kr", "02-1234-5678",
     *     "SGIS_API_KEY_12345", "SGIS_SECRET_98765",
     *     1000, 30000, LocalDateTime.now().plusYears(1),
     *     "서울시 인구통계 데이터 수집용", "인구통계,경제통계"
     * );
     * 
     * 보안 고려사항:
     * - 입력받은 API 키와 시크릿 키는 로그에 출력하지 않음
     * - 민감한 정보는 적절히 마스킹하여 처리
     */
    public SgisApiKey registerApiKey(String organizationName, String organizationCode,
                                   String contactEmail, String contactPhone,
                                   String apiKey, String secretKey,
                                   Integer dailyLimit, Integer monthlyLimit,
                                   LocalDateTime expiresAt, String description,
                                   String requestedApis) {
        
        log.info("새로운 SGIS API 키 등록 요청: Organization={}", organizationName);

        try {
            // 필수 필드 검증
            if (organizationName == null || organizationName.trim().isEmpty()) {
                throw new IllegalArgumentException("기관명은 필수입니다.");
            }
            if (contactEmail == null || contactEmail.trim().isEmpty()) {
                throw new IllegalArgumentException("연락처 이메일은 필수입니다.");
            }
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException("API 키는 필수입니다.");
            }
            if (secretKey == null || secretKey.trim().isEmpty()) {
                throw new IllegalArgumentException("시크릿 키는 필수입니다.");
            }
            if (dailyLimit == null || dailyLimit <= 0) {
                throw new IllegalArgumentException("일일 사용량 제한은 0보다 큰 값이어야 합니다.");
            }
            if (monthlyLimit == null || monthlyLimit <= 0) {
                throw new IllegalArgumentException("월간 사용량 제한은 0보다 큰 값이어야 합니다.");
            }
            if (expiresAt == null) {
                throw new IllegalArgumentException("만료일시는 필수입니다.");
            }

            // 고유한 keyId 생성
            String keyId = generateUniqueKeyId(organizationName);

            // API 키 엔티티 생성
            SgisApiKey sgisApiKey = SgisApiKey.builder()
                    .keyId(keyId)
                    .organizationName(organizationName.trim())
                    .organizationCode(organizationCode != null ? organizationCode.trim() : null)
                    .contactEmail(contactEmail.trim())
                    .contactPhone(contactPhone != null ? contactPhone.trim() : null)
                    .apiKey(apiKey.trim())
                    .secretKey(secretKey.trim())
                    .dailyLimit(dailyLimit)
                    .monthlyLimit(monthlyLimit)
                    .expiresAt(expiresAt)
                    .status(ApiKeyStatus.ACTIVE)
                    .description(description != null ? description.trim() : null)
                    .requestedApis(requestedApis != null ? requestedApis.trim() : null)
                    .build();

            SgisApiKey savedApiKey = sgisApiKeyRepository.save(sgisApiKey);
            log.info("SGIS API 키 등록 완료: Key ID={}, Organization={}", 
                    savedApiKey.getKeyId(), savedApiKey.getOrganizationName());

            return savedApiKey;

        } catch (Exception e) {
            log.error("SGIS API 키 등록 실패: {}", e.getMessage(), e);
            throw new RuntimeException("SGIS API 키 등록에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 고유한 keyId 생성
     * 
     * @param organizationName 기관명
     * @return 고유한 keyId
     */
    private String generateUniqueKeyId(String organizationName) {
        String baseKeyId = "SGIS_" + organizationName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        String uniqueKeyId = baseKeyId + "_" + UUID.randomUUID().toString().substring(0, 8);
        
        // 중복 확인 (실제로는 거의 발생하지 않음)
        int counter = 1;
        while (sgisApiKeyRepository.findByKeyIdAndDeletedFalse(uniqueKeyId).isPresent()) {
            uniqueKeyId = baseKeyId + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + counter++;
        }
        
        return uniqueKeyId;
    }

    /**
     * API 키 조회
     */
    @Transactional(readOnly = true)
    public Optional<SgisApiKey> getApiKey(String keyId) {
        return sgisApiKeyRepository.findByKeyIdAndDeletedFalse(keyId);
    }

    /**
     * 기관명으로 API 키 조회
     */
    @Transactional(readOnly = true)
    public Optional<SgisApiKey> getApiKeyByOrganization(String organizationName) {
        return sgisApiKeyRepository.findByOrganizationNameAndDeletedFalse(organizationName);
    }

    /**
     * API 키 값으로 조회
     */
    @Transactional(readOnly = true)
    public Optional<SgisApiKey> getApiKeyByApiKey(String apiKey) {
        return sgisApiKeyRepository.findByApiKeyAndDeletedFalse(apiKey);
    }

    /**
     * 모든 API 키 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<SgisApiKey> getAllApiKeys(Pageable pageable) {
        return sgisApiKeyRepository.findByDeletedFalse(pageable);
    }

    /**
     * 기관명으로 API 키 검색 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<SgisApiKey> searchApiKeysByOrganization(String organizationName, Pageable pageable) {
        return sgisApiKeyRepository.findByOrganizationNameContainingAndDeletedFalse(organizationName, pageable);
    }

    /**
     * 상태별 API 키 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<SgisApiKey> getApiKeysByStatus(ApiKeyStatus status, Pageable pageable) {
        return sgisApiKeyRepository.findByStatusAndDeletedFalse(status, pageable);
    }

    /**
     * API 키 정보 수정
     */
    public SgisApiKey updateApiKey(String keyId, SgisApiKey updateData) {
        log.info("API 키 정보 수정: Key ID={}", keyId);

        SgisApiKey existingApiKey = sgisApiKeyRepository.findByKeyIdAndDeletedFalse(keyId)
                .orElseThrow(() -> new RuntimeException("API 키를 찾을 수 없습니다: " + keyId));

        // 수정 가능한 필드들만 업데이트
        if (updateData.getOrganizationName() != null) {
            existingApiKey.setOrganizationName(updateData.getOrganizationName());
        }
        if (updateData.getContactEmail() != null) {
            existingApiKey.setContactEmail(updateData.getContactEmail());
        }
        if (updateData.getContactPhone() != null) {
            existingApiKey.setContactPhone(updateData.getContactPhone());
        }
        if (updateData.getDailyLimit() != null) {
            existingApiKey.setDailyLimit(updateData.getDailyLimit());
        }
        if (updateData.getMonthlyLimit() != null) {
            existingApiKey.setMonthlyLimit(updateData.getMonthlyLimit());
        }
        if (updateData.getDescription() != null) {
            existingApiKey.setDescription(updateData.getDescription());
        }

        SgisApiKey updatedApiKey = sgisApiKeyRepository.save(existingApiKey);
        log.info("API 키 정보 수정 완료: Key ID={}", keyId);

        return updatedApiKey;
    }

    /**
     * API 키 상태 변경
     */
    public SgisApiKey updateApiKeyStatus(String keyId, ApiKeyStatus newStatus) {
        log.info("API 키 상태 변경: Key ID={}, New Status={}", keyId, newStatus);

        SgisApiKey apiKey = sgisApiKeyRepository.findByKeyIdAndDeletedFalse(keyId)
                .orElseThrow(() -> new RuntimeException("API 키를 찾을 수 없습니다: " + keyId));

        apiKey.setStatus(newStatus);
        SgisApiKey updatedApiKey = sgisApiKeyRepository.save(apiKey);

        log.info("API 키 상태 변경 완료: Key ID={}, Status={}", keyId, newStatus);
        return updatedApiKey;
    }

    /**
     * API 키 폐기
     */
    public boolean revokeApiKey(String keyId) {
        log.info("API 키 폐기 요청: Key ID={}", keyId);

        SgisApiKey apiKey = sgisApiKeyRepository.findByKeyIdAndDeletedFalse(keyId)
                .orElseThrow(() -> new RuntimeException("API 키를 찾을 수 없습니다: " + keyId));

        try {
            // SGIS API에 폐기 요청
            // This part of the logic needs to be re-evaluated as the external API client is removed.
            // For now, we'll just set the status to REVOKED locally.
            apiKey.setStatus(ApiKeyStatus.REVOKED);
            apiKey.setDeleted(true); // 소프트 삭제 플래그 설정
            sgisApiKeyRepository.save(apiKey);
            log.info("API 키 폐기 완료: Key ID={}", keyId);
            return true;
        } catch (Exception e) {
            log.error("API 키 폐기 중 오류 발생: Key ID={}, Error={}", keyId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * API 키 사용량 증가
     */
    public void incrementUsage(String keyId) {
        SgisApiKey apiKey = sgisApiKeyRepository.findByKeyIdAndDeletedFalse(keyId)
                .orElseThrow(() -> new RuntimeException("API 키를 찾을 수 없습니다: " + keyId));

        // 일일 사용량 증가
        apiKey.setCurrentDailyUsage(apiKey.getCurrentDailyUsage() + 1);
        
        // 월간 사용량 증가
        apiKey.setCurrentMonthlyUsage(apiKey.getCurrentMonthlyUsage() + 1);
        
        // 마지막 사용 시간 업데이트
        apiKey.setLastUsedAt(LocalDateTime.now());

        sgisApiKeyRepository.save(apiKey);
        log.debug("API 키 사용량 증가: Key ID={}, Daily={}, Monthly={}", 
                keyId, apiKey.getCurrentDailyUsage(), apiKey.getCurrentMonthlyUsage());
    }

    /**
     * API 키 사용 가능 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isApiKeyUsable(String keyId) {
        Optional<SgisApiKey> apiKeyOpt = sgisApiKeyRepository.findByKeyIdAndDeletedFalse(keyId);
        if (apiKeyOpt.isEmpty()) {
            return false;
        }

        SgisApiKey apiKey = apiKeyOpt.get();
        return apiKey.isActive() && 
               !apiKey.isDailyLimitExceeded() && 
               !apiKey.isMonthlyLimitExceeded();
    }

    /**
     * 만료 예정 API 키 목록 조회 (7일 이내)
     */
    @Transactional(readOnly = true)
    public List<SgisApiKey> getExpiringApiKeys() {
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().plusDays(7);
        return sgisApiKeyRepository.findExpiringKeys(startDate, endDate);
    }

    /**
     * 만료된 API 키 목록 조회
     */
    @Transactional(readOnly = true)
    public List<SgisApiKey> getExpiredApiKeys() {
        return sgisApiKeyRepository.findExpiredKeys(LocalDateTime.now());
    }

    /**
     * 일일 사용량 제한에 도달한 API 키 목록 조회
     */
    @Transactional(readOnly = true)
    public List<SgisApiKey> getDailyLimitExceededApiKeys() {
        return sgisApiKeyRepository.findDailyLimitExceededKeys();
    }

    /**
     * 월간 사용량 제한에 도달한 API 키 목록 조회
     */
    @Transactional(readOnly = true)
    public List<SgisApiKey> getMonthlyLimitExceededApiKeys() {
        return sgisApiKeyRepository.findMonthlyLimitExceededKeys();
    }

    /**
     * API 키 통계 조회
     */
    @Transactional(readOnly = true)
    public long getApiKeyCountByStatus(ApiKeyStatus status) {
        return sgisApiKeyRepository.countByStatusAndDeletedFalse(status);
    }

    /**
     * 활성 API 키 개수 조회
     */
    @Transactional(readOnly = true)
    public long getActiveApiKeyCount() {
        return getApiKeyCountByStatus(ApiKeyStatus.ACTIVE);
    }

    /**
     * 만료된 API 키 개수 조회
     */
    @Transactional(readOnly = true)
    public long getExpiredApiKeyCount() {
        return getApiKeyCountByStatus(ApiKeyStatus.EXPIRED);
    }

    /**
     * API 키 소프트 삭제
     */
    public void softDeleteApiKey(String keyId) {
        log.info("API 키 소프트 삭제: Key ID={}", keyId);

        SgisApiKey apiKey = sgisApiKeyRepository.findByKeyIdAndDeletedFalse(keyId)
                .orElseThrow(() -> new RuntimeException("API 키를 찾을 수 없습니다: " + keyId));

        apiKey.setDeleted(true);
        sgisApiKeyRepository.save(apiKey);

        log.info("API 키 소프트 삭제 완료: Key ID={}", keyId);
    }

    /**
     * API 키 하드 삭제
     */
    public void hardDeleteApiKey(String keyId) {
        log.info("API 키 하드 삭제: Key ID={}", keyId);

        SgisApiKey apiKey = sgisApiKeyRepository.findByKeyIdAndDeletedFalse(keyId)
                .orElseThrow(() -> new RuntimeException("API 키를 찾을 수 없습니다: " + keyId));
        sgisApiKeyRepository.delete(apiKey);

        log.info("API 키 하드 삭제 완료: Key ID={}", keyId);
    }

    // ==================== 토큰 갱신 스케줄링 ====================

    /**
     * 매일 오전 9시에 만료 예정 API 키 확인 및 알림
     * 
     * 이 스케줄러는 매일 오전 9시에 실행되어 만료 예정인 API 키를 확인하고
     * 해당 기관에 갱신 알림을 발송합니다.
     * 
     * 알림 기준:
     * - 7일 이내 만료: 일반 갱신 알림
     * - 1일 이내 만료: 긴급 갱신 알림
     * - 만료일: 만료 완료 알림
     */
    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시
    public void checkExpiringKeysAndNotify() {
        log.info("만료 예정 API 키 확인 및 알림 발송 시작");
        
        try {
            // 7일 이내 만료 예정 키 확인
            List<SgisApiKey> expiringIn7Days = getExpiringApiKeys();
            
            for (SgisApiKey apiKey : expiringIn7Days) {
                long daysUntilExpiry = java.time.Duration.between(
                    LocalDateTime.now(), apiKey.getExpiresAt()
                ).toDays();
                
                if (daysUntilExpiry <= 1) {
                    // 1일 이내 만료 - 긴급 알림
                    sendUrgentExpirationNotification(apiKey);
                } else if (daysUntilExpiry <= 7) {
                    // 7일 이내 만료 - 일반 알림
                    sendExpirationNotification(apiKey);
                }
            }
            
            // 만료된 키 상태 업데이트
            updateExpiredKeysStatus();
            
            log.info("만료 예정 API 키 확인 및 알림 발송 완료: {}개 키 처리", expiringIn7Days.size());
            
        } catch (Exception e) {
            log.error("만료 예정 API 키 확인 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 매일 자정에 일일 사용량 초기화
     * 
     * 이 스케줄러는 매일 자정(00:00)에 실행되어 모든 활성 API 키의
     * 일일 사용량을 0으로 초기화합니다.
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    public void resetDailyUsage() {
        log.info("일일 사용량 초기화 시작");
        
        try {
            List<SgisApiKey> activeKeys = sgisApiKeyRepository.findByStatusAndDeletedFalse(ApiKeyStatus.ACTIVE);
            
            for (SgisApiKey apiKey : activeKeys) {
                if (apiKey.getCurrentDailyUsage() > 0) {
                    apiKey.setCurrentDailyUsage(0);
                    sgisApiKeyRepository.save(apiKey);
                }
            }
            
            log.info("일일 사용량 초기화 완료: {}개 키 처리", activeKeys.size());
            
        } catch (Exception e) {
            log.error("일일 사용량 초기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 매월 1일 자정에 월간 사용량 초기화
     * 
     * 이 스케줄러는 매월 1일 자정(00:00)에 실행되어 모든 활성 API 키의
     * 월간 사용량을 0으로 초기화합니다.
     */
    @Scheduled(cron = "0 0 0 1 * *") // 매월 1일 자정
    public void resetMonthlyUsage() {
        log.info("월간 사용량 초기화 시작");
        
        try {
            List<SgisApiKey> activeKeys = sgisApiKeyRepository.findByStatusAndDeletedFalse(ApiKeyStatus.ACTIVE);
            
            for (SgisApiKey apiKey : activeKeys) {
                if (apiKey.getCurrentMonthlyUsage() > 0) {
                    apiKey.setCurrentMonthlyUsage(0);
                    sgisApiKeyRepository.save(apiKey);
                }
            }
            
            log.info("월간 사용량 초기화 완료: {}개 키 처리", activeKeys.size());
            
        } catch (Exception e) {
            log.error("월간 사용량 초기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 매시간 만료된 키 상태 자동 업데이트
     * 
     * 이 스케줄러는 매시간 실행되어 만료된 API 키의 상태를
     * 자동으로 EXPIRED로 변경합니다.
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다 (밀리초)
    public void updateExpiredKeysStatus() {
        log.debug("만료된 API 키 상태 업데이트 시작");
        
        try {
            List<SgisApiKey> expiredKeys = getExpiredApiKeys();
            
            for (SgisApiKey apiKey : expiredKeys) {
                if (ApiKeyStatus.ACTIVE.equals(apiKey.getStatus())) {
                    apiKey.setStatus(ApiKeyStatus.EXPIRED);
                    sgisApiKeyRepository.save(apiKey);
                    log.info("API 키 상태를 만료로 변경: Key ID={}", apiKey.getKeyId());
                }
            }
            
            if (!expiredKeys.isEmpty()) {
                log.info("만료된 API 키 상태 업데이트 완료: {}개 키 처리", expiredKeys.size());
            }
            
        } catch (Exception e) {
            log.error("만료된 API 키 상태 업데이트 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // ==================== 알림 발송 메서드 ====================

    /**
     * 만료 예정 알림 발송
     * 
     * @param apiKey 만료 예정인 API 키
     */
    private void sendExpirationNotification(SgisApiKey apiKey) {
        try {
            long daysUntilExpiry = java.time.Duration.between(
                LocalDateTime.now(), apiKey.getExpiresAt()
            ).toDays();
            
            log.info("만료 예정 알림 발송: Key ID={}, Organization={}, {}일 후 만료", 
                    apiKey.getKeyId(), apiKey.getOrganizationName(), daysUntilExpiry);
            
            // TODO: 실제 알림 발송 로직 구현
            // - 이메일 발송
            // - SMS 발송
            // - 웹훅 호출
            // - 알림 대시보드 업데이트
            
        } catch (Exception e) {
            log.error("만료 예정 알림 발송 실패: Key ID={}, Error={}", 
                    apiKey.getKeyId(), e.getMessage(), e);
        }
    }

    /**
     * 긴급 만료 알림 발송
     * 
     * @param apiKey 1일 이내 만료 예정인 API 키
     */
    private void sendUrgentExpirationNotification(SgisApiKey apiKey) {
        try {
            long daysUntilExpiry = java.time.Duration.between(
                LocalDateTime.now(), apiKey.getExpiresAt()
            ).toDays();
            
            log.warn("긴급 만료 알림 발송: Key ID={}, Organization={}, {}일 후 만료", 
                    apiKey.getKeyId(), apiKey.getOrganizationName(), daysUntilExpiry);
            
            // TODO: 실제 긴급 알림 발송 로직 구현
            // - 우선순위 높은 이메일 발송
            // - SMS 발송
            // - 관리자 대시보드 알림
            // - 슬랙/팀즈 등 메신저 알림
            
        } catch (Exception e) {
            log.error("긴급 만료 알림 발송 실패: Key ID={}, Error={}", 
                    apiKey.getKeyId(), e.getMessage(), e);
        }
    }
}
