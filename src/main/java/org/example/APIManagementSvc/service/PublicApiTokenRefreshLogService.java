package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.PublicApiTokenRefreshLog;
import org.example.APIManagementSvc.domain.enums.RefreshStatus;
import org.example.APIManagementSvc.repository.PublicApiTokenRefreshLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Public API Token Refresh 로그 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PublicApiTokenRefreshLogService {

    private final PublicApiTokenRefreshLogRepository publicApiTokenRefreshLogRepository;

    /**
     * Token Refresh 로그 생성
     */
    @Transactional
    public PublicApiTokenRefreshLog createLog(PublicApiTokenRefreshLog tokenRefreshLog) {
        log.info("Creating token refresh log for provider: {}, Status: {}", 
                tokenRefreshLog.getProviderName(), tokenRefreshLog.getRefreshStatus());
        
        PublicApiTokenRefreshLog savedLog = publicApiTokenRefreshLogRepository.save(tokenRefreshLog);
        log.info("Token refresh log created successfully with ID: {}", savedLog.getLogId());
        
        return savedLog;
    }

    /**
     * 성공적인 Token Refresh 로그 기록
     */
    @Transactional
    public PublicApiTokenRefreshLog recordSuccessfulRefresh(String providerName) {
        PublicApiTokenRefreshLog tokenRefreshLog = PublicApiTokenRefreshLog.builder()
                .providerName(providerName)
                .refreshStatus(RefreshStatus.SUCCESS)
                .refreshTime(LocalDateTime.now())
                .deleted(false)
                .build();
        
        return createLog(tokenRefreshLog);
    }

    /**
     * 실패한 Token Refresh 로그 기록
     */
    @Transactional
    public PublicApiTokenRefreshLog recordFailedRefresh(String providerName, String errorMessage) {
        PublicApiTokenRefreshLog tokenRefreshLog = PublicApiTokenRefreshLog.builder()
                .providerName(providerName)
                .refreshStatus(RefreshStatus.FAILED)
                .errorMessage(errorMessage)
                .refreshTime(LocalDateTime.now())
                .deleted(false)
                .build();
        
        return createLog(tokenRefreshLog);
    }

    /**
     * 진행 중인 Token Refresh 로그 기록
     */
    @Transactional
    public PublicApiTokenRefreshLog recordInProgressRefresh(String providerName) {
        PublicApiTokenRefreshLog tokenRefreshLog = PublicApiTokenRefreshLog.builder()
                .providerName(providerName)
                .refreshStatus(RefreshStatus.IN_PROGRESS)
                .refreshTime(LocalDateTime.now())
                .deleted(false)
                .build();
        
        return createLog(tokenRefreshLog);
    }

    /**
     * 만료된 Token 로그 기록
     */
    @Transactional
    public PublicApiTokenRefreshLog recordExpiredToken(String providerName) {
        PublicApiTokenRefreshLog tokenRefreshLog = PublicApiTokenRefreshLog.builder()
                .providerName(providerName)
                .refreshStatus(RefreshStatus.EXPIRED)
                .refreshTime(LocalDateTime.now())
                .deleted(false)
                .build();
        
        return createLog(tokenRefreshLog);
    }

    /**
     * 수동 갱신 로그 기록
     */
    @Transactional
    public PublicApiTokenRefreshLog recordManualRefresh(String providerName) {
        PublicApiTokenRefreshLog tokenRefreshLog = PublicApiTokenRefreshLog.builder()
                .providerName(providerName)
                .refreshStatus(RefreshStatus.MANUAL_REFRESH)
                .refreshTime(LocalDateTime.now())
                .deleted(false)
                .build();
        
        return createLog(tokenRefreshLog);
    }

    // 조회 메서드들
    public Optional<PublicApiTokenRefreshLog> getLogById(Long logId) {
        return publicApiTokenRefreshLogRepository.findByLogIdAndDeletedFalse(logId);
    }

    public List<PublicApiTokenRefreshLog> getLogsByProvider(String providerName) {
        return publicApiTokenRefreshLogRepository.findByProviderNameAndDeletedFalse(providerName);
    }

    public List<PublicApiTokenRefreshLog> getLogsByRefreshStatus(RefreshStatus status) {
        return publicApiTokenRefreshLogRepository.findByRefreshStatusAndDeletedFalse(status);
    }

    public List<PublicApiTokenRefreshLog> getLogsByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        return publicApiTokenRefreshLogRepository.findByRefreshTimeBetweenAndDeletedFalse(startTime, endTime);
    }

    public List<PublicApiTokenRefreshLog> getLogsByProviderAndStatus(String providerName, RefreshStatus status) {
        return publicApiTokenRefreshLogRepository.findByProviderNameAndRefreshStatusAndDeletedFalse(providerName, status);
    }

    // 페이징 조회
    public Page<PublicApiTokenRefreshLog> getLogsWithPaging(Pageable pageable) {
        return publicApiTokenRefreshLogRepository.findByDeletedFalse(pageable);
    }

    public Page<PublicApiTokenRefreshLog> getLogsByProviderWithPaging(String providerName, Pageable pageable) {
        return publicApiTokenRefreshLogRepository.findByProviderNameAndDeletedFalse(providerName, pageable);
    }

    public Page<PublicApiTokenRefreshLog> getLogsByStatusWithPaging(RefreshStatus status, Pageable pageable) {
        return publicApiTokenRefreshLogRepository.findByRefreshStatusAndDeletedFalse(status, pageable);
    }

    // 최근 로그 조회
    public List<PublicApiTokenRefreshLog> getRecentLogsByProvider(String providerName, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return publicApiTokenRefreshLogRepository.findTopByProviderNameAndDeletedFalseOrderByRefreshTimeDesc(providerName, pageable);
    }

    public List<PublicApiTokenRefreshLog> getRecentLogsByStatus(RefreshStatus status, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return publicApiTokenRefreshLogRepository.findTopByRefreshStatusAndDeletedFalseOrderByRefreshTimeDesc(status, pageable);
    }

    public List<PublicApiTokenRefreshLog> getRecentLogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return publicApiTokenRefreshLogRepository.findTopByDeletedFalseOrderByRefreshTimeDesc(pageable);
    }

    // 통계 조회
    public long getTotalLogCount() {
        return publicApiTokenRefreshLogRepository.countByDeletedFalse();
    }

    public long getLogCountByProvider(String providerName) {
        return publicApiTokenRefreshLogRepository.countByProviderNameAndDeletedFalse(providerName);
    }

    public long getLogCountByStatus(RefreshStatus status) {
        return publicApiTokenRefreshLogRepository.countByRefreshStatusAndDeletedFalse(status);
    }

    public long getLogCountByProviderAndStatus(String providerName, RefreshStatus status) {
        return publicApiTokenRefreshLogRepository.countByProviderNameAndRefreshStatusAndDeletedFalse(providerName, status);
    }

    // 마지막 갱신 시간 조회
    public Optional<LocalDateTime> getLastRefreshTimeByProvider(String providerName) {
        return publicApiTokenRefreshLogRepository.findLastRefreshTimeByProviderNameAndDeletedFalse(providerName);
    }

    public Optional<LocalDateTime> getLastRefreshTimeByStatus(RefreshStatus status) {
        return publicApiTokenRefreshLogRepository.findLastRefreshTimeByRefreshStatusAndDeletedFalse(status);
    }

    // Refresh 상태별 통계
    public Map<RefreshStatus, Long> getRefreshStatusStatistics() {
        List<Object[]> results = publicApiTokenRefreshLogRepository.findRefreshStatusStatistics();
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (RefreshStatus) row[0],
                        row -> (Long) row[1]
                ));
    }

    public Map<RefreshStatus, Long> getRefreshStatusStatisticsByProvider(String providerName) {
        List<Object[]> results = publicApiTokenRefreshLogRepository.findRefreshStatusStatisticsByProviderName(providerName);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (RefreshStatus) row[0],
                        row -> (Long) row[1]
                ));
    }

    // 특정 시간대 특정 상태 로그
    public List<PublicApiTokenRefreshLog> getLogsByStatusAndTimeRange(RefreshStatus status, LocalDateTime startTime, LocalDateTime endTime) {
        return publicApiTokenRefreshLogRepository.findByRefreshStatusAndTimeRangeAndDeletedFalse(status, startTime, endTime);
    }

    // 삭제 메서드들
    @Transactional
    public void deleteLog(Long logId) {
        Optional<PublicApiTokenRefreshLog> logOpt = publicApiTokenRefreshLogRepository.findById(logId);
        if (logOpt.isPresent()) {
            PublicApiTokenRefreshLog tokenRefreshLog = logOpt.get();
            tokenRefreshLog.setDeleted(true);
            publicApiTokenRefreshLogRepository.save(tokenRefreshLog);
            log.info("Token refresh log soft deleted: {}", logId);
        }
    }

    @Transactional
    public void hardDeleteLog(Long logId) {
        publicApiTokenRefreshLogRepository.deleteById(logId);
        log.info("Token refresh log hard deleted: {}", logId);
    }

    // 제공자별 통계
    public Map<String, Object> getProviderStatistics(String providerName) {
        long totalRefreshes = getLogCountByProvider(providerName);
        long successfulRefreshes = getLogCountByProviderAndStatus(providerName, RefreshStatus.SUCCESS);
        long failedRefreshes = getLogCountByProviderAndStatus(providerName, RefreshStatus.FAILED);
        long inProgressRefreshes = getLogCountByProviderAndStatus(providerName, RefreshStatus.IN_PROGRESS);
        long expiredTokens = getLogCountByProviderAndStatus(providerName, RefreshStatus.EXPIRED);
        long manualRefreshes = getLogCountByProviderAndStatus(providerName, RefreshStatus.MANUAL_REFRESH);

        return Map.of(
                "providerName", providerName,
                "totalRefreshes", totalRefreshes,
                "successfulRefreshes", successfulRefreshes,
                "failedRefreshes", failedRefreshes,
                "inProgressRefreshes", inProgressRefreshes,
                "expiredTokens", expiredTokens,
                "manualRefreshes", manualRefreshes,
                "successRate", totalRefreshes > 0 ? (double) successfulRefreshes / totalRefreshes : 0.0
        );
    }

    // 전체 통계
    public Map<String, Object> getOverallStatistics() {
        long totalRefreshes = getTotalLogCount();
        long successfulRefreshes = getLogCountByStatus(RefreshStatus.SUCCESS);
        long failedRefreshes = getLogCountByStatus(RefreshStatus.FAILED);
        long inProgressRefreshes = getLogCountByStatus(RefreshStatus.IN_PROGRESS);
        long expiredTokens = getLogCountByStatus(RefreshStatus.EXPIRED);
        long manualRefreshes = getLogCountByStatus(RefreshStatus.MANUAL_REFRESH);

        return Map.of(
                "totalRefreshes", totalRefreshes,
                "successfulRefreshes", successfulRefreshes,
                "failedRefreshes", failedRefreshes,
                "inProgressRefreshes", inProgressRefreshes,
                "expiredTokens", expiredTokens,
                "manualRefreshes", manualRefreshes,
                "successRate", totalRefreshes > 0 ? (double) successfulRefreshes / totalRefreshes : 0.0
        );
    }

    // 성공률이 낮은 제공자 조회
    public List<String> getLowSuccessRateProviders(double threshold) {
        // 모든 제공자에 대해 성공률을 계산하고 임계값 이하인 제공자들을 반환
        // 이는 복잡한 쿼리가 필요하므로 별도 구현이 필요할 수 있음
        return List.of(); // 임시 구현
    }

    // 최근 실패한 제공자들 조회
    public List<String> getRecentlyFailedProviders(int hours) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        List<PublicApiTokenRefreshLog> failedLogs = getLogsByStatusAndTimeRange(RefreshStatus.FAILED, cutoffTime, LocalDateTime.now());
        return failedLogs.stream()
                .map(PublicApiTokenRefreshLog::getProviderName)
                .distinct()
                .collect(Collectors.toList());
    }
}
