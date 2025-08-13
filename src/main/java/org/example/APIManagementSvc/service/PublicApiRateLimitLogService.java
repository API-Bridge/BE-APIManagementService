package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.PublicApiRateLimitLog;
import org.example.APIManagementSvc.domain.enums.RateLimitStatus;
import org.example.APIManagementSvc.repository.PublicApiRateLimitLogRepository;
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
 * Public API Rate Limit 로그 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PublicApiRateLimitLogService {

    private final PublicApiRateLimitLogRepository publicApiRateLimitLogRepository;

    /**
     * Rate Limit 로그 생성
     */
    @Transactional
    public PublicApiRateLimitLog createLog(PublicApiRateLimitLog rateLimitLog) {
        log.info("Creating rate limit log for API: {}, User: {}, Status: {}", 
                rateLimitLog.getApiName(), rateLimitLog.getUserId(), rateLimitLog.getRateLimitStatus());
        
        PublicApiRateLimitLog savedLog = publicApiRateLimitLogRepository.save(rateLimitLog);
        log.info("Rate limit log created successfully with ID: {}", savedLog.getLogId());
        
        return savedLog;
    }

    /**
     * Rate Limit 허용 로그 기록
     */
    @Transactional
    public PublicApiRateLimitLog recordAllowedRequest(String apiName, String userId, String ipAddress, 
                                                    Integer currentUsage, Integer maxAllowed) {
        PublicApiRateLimitLog log = PublicApiRateLimitLog.builder()
                .apiName(apiName)
                .userId(userId)
                .ipAddress(ipAddress)
                .rateLimitStatus(RateLimitStatus.ALLOWED)
                .currentUsageCount(currentUsage)
                .maxAllowedCount(maxAllowed)
                .requestTime(LocalDateTime.now())
                .deleted(false)
                .build();
        
        return createLog(log);
    }

    /**
     * Rate Limit 경고 로그 기록
     */
    @Transactional
    public PublicApiRateLimitLog recordWarningRequest(String apiName, String userId, String ipAddress, 
                                                    Integer currentUsage, Integer maxAllowed) {
        PublicApiRateLimitLog log = PublicApiRateLimitLog.builder()
                .apiName(apiName)
                .userId(userId)
                .ipAddress(ipAddress)
                .rateLimitStatus(RateLimitStatus.WARNING)
                .currentUsageCount(currentUsage)
                .maxAllowedCount(maxAllowed)
                .requestTime(LocalDateTime.now())
                .deleted(false)
                .build();
        
        return createLog(log);
    }

    /**
     * Rate Limit 초과 로그 기록
     */
    @Transactional
    public PublicApiRateLimitLog recordExceededRequest(String apiName, String userId, String ipAddress, 
                                                     Integer currentUsage, Integer maxAllowed, 
                                                     LocalDateTime resetTime, String errorMessage) {
        PublicApiRateLimitLog log = PublicApiRateLimitLog.builder()
                .apiName(apiName)
                .userId(userId)
                .ipAddress(ipAddress)
                .rateLimitStatus(RateLimitStatus.EXCEEDED)
                .currentUsageCount(currentUsage)
                .maxAllowedCount(maxAllowed)
                .resetTime(resetTime)
                .errorMessage(errorMessage)
                .requestTime(LocalDateTime.now())
                .deleted(false)
                .build();
        
        return createLog(log);
    }

    /**
     * Rate Limit 차단 로그 기록
     */
    @Transactional
    public PublicApiRateLimitLog recordBlockedRequest(String apiName, String userId, String ipAddress, 
                                                    String errorMessage) {
        PublicApiRateLimitLog log = PublicApiRateLimitLog.builder()
                .apiName(apiName)
                .userId(userId)
                .ipAddress(ipAddress)
                .rateLimitStatus(RateLimitStatus.BLOCKED)
                .errorMessage(errorMessage)
                .requestTime(LocalDateTime.now())
                .deleted(false)
                .build();
        
        return createLog(log);
    }

    /**
     * Rate Limit 리셋 로그 기록
     */
    @Transactional
    public PublicApiRateLimitLog recordResetLog(String apiName, String userId, String ipAddress) {
        PublicApiRateLimitLog log = PublicApiRateLimitLog.builder()
                .apiName(apiName)
                .userId(userId)
                .ipAddress(ipAddress)
                .rateLimitStatus(RateLimitStatus.RESET)
                .requestTime(LocalDateTime.now())
                .deleted(false)
                .build();
        
        return createLog(log);
    }

    // 조회 메서드들
    public Optional<PublicApiRateLimitLog> getLogById(Long logId) {
        return publicApiRateLimitLogRepository.findByLogIdAndDeletedFalse(logId);
    }

    public List<PublicApiRateLimitLog> getLogsByApiName(String apiName) {
        return publicApiRateLimitLogRepository.findByApiNameAndDeletedFalse(apiName);
    }

    public List<PublicApiRateLimitLog> getLogsByUserId(String userId) {
        return publicApiRateLimitLogRepository.findByUserIdAndDeletedFalse(userId);
    }

    public List<PublicApiRateLimitLog> getLogsByIpAddress(String ipAddress) {
        return publicApiRateLimitLogRepository.findByIpAddressAndDeletedFalse(ipAddress);
    }

    public List<PublicApiRateLimitLog> getLogsByRateLimitStatus(RateLimitStatus status) {
        return publicApiRateLimitLogRepository.findByRateLimitStatusAndDeletedFalse(status);
    }

    public List<PublicApiRateLimitLog> getLogsByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        return publicApiRateLimitLogRepository.findByRequestTimeBetweenAndDeletedFalse(startTime, endTime);
    }

    public List<PublicApiRateLimitLog> getLogsByApiNameAndStatus(String apiName, RateLimitStatus status) {
        return publicApiRateLimitLogRepository.findByApiNameAndRateLimitStatusAndDeletedFalse(apiName, status);
    }

    public List<PublicApiRateLimitLog> getLogsByUserIdAndStatus(String userId, RateLimitStatus status) {
        return publicApiRateLimitLogRepository.findByUserIdAndRateLimitStatusAndDeletedFalse(userId, status);
    }

    public List<PublicApiRateLimitLog> getLogsByApiNameAndUserId(String apiName, String userId) {
        return publicApiRateLimitLogRepository.findByApiNameAndUserIdAndDeletedFalse(apiName, userId);
    }

    // 페이징 조회
    public Page<PublicApiRateLimitLog> getLogsWithPaging(Pageable pageable) {
        return publicApiRateLimitLogRepository.findByDeletedFalse(pageable);
    }

    public Page<PublicApiRateLimitLog> getLogsByApiNameWithPaging(String apiName, Pageable pageable) {
        return publicApiRateLimitLogRepository.findByApiNameAndDeletedFalse(apiName, pageable);
    }

    public Page<PublicApiRateLimitLog> getLogsByUserIdWithPaging(String userId, Pageable pageable) {
        return publicApiRateLimitLogRepository.findByUserIdAndDeletedFalse(userId, pageable);
    }

    public Page<PublicApiRateLimitLog> getLogsByStatusWithPaging(RateLimitStatus status, Pageable pageable) {
        return publicApiRateLimitLogRepository.findByRateLimitStatusAndDeletedFalse(status, pageable);
    }

    // 최근 로그 조회
    public List<PublicApiRateLimitLog> getRecentLogsByApiName(String apiName, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return publicApiRateLimitLogRepository.findTopByApiNameAndDeletedFalseOrderByRequestTimeDesc(apiName, pageable);
    }

    public List<PublicApiRateLimitLog> getRecentLogsByUserId(String userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return publicApiRateLimitLogRepository.findTopByUserIdAndDeletedFalseOrderByRequestTimeDesc(userId, pageable);
    }

    public List<PublicApiRateLimitLog> getRecentLogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return publicApiRateLimitLogRepository.findTopByDeletedFalseOrderByRequestTimeDesc(pageable);
    }

    // 통계 조회
    public long getTotalLogCount() {
        return publicApiRateLimitLogRepository.countByDeletedFalse();
    }

    public long getLogCountByApiName(String apiName) {
        return publicApiRateLimitLogRepository.countByApiNameAndDeletedFalse(apiName);
    }

    public long getLogCountByUserId(String userId) {
        return publicApiRateLimitLogRepository.countByUserIdAndDeletedFalse(userId);
    }

    public long getLogCountByStatus(RateLimitStatus status) {
        return publicApiRateLimitLogRepository.countByRateLimitStatusAndDeletedFalse(status);
    }

    public long getLogCountByApiNameAndStatus(String apiName, RateLimitStatus status) {
        return publicApiRateLimitLogRepository.countByApiNameAndRateLimitStatusAndDeletedFalse(apiName, status);
    }

    public long getLogCountByUserIdAndStatus(String userId, RateLimitStatus status) {
        return publicApiRateLimitLogRepository.countByUserIdAndRateLimitStatusAndDeletedFalse(userId, status);
    }

    // 마지막 요청 시간 조회
    public Optional<LocalDateTime> getLastRequestTimeByApiName(String apiName) {
        return publicApiRateLimitLogRepository.findLastRequestTimeByApiNameAndDeletedFalse(apiName);
    }

    public Optional<LocalDateTime> getLastRequestTimeByUserId(String userId) {
        return publicApiRateLimitLogRepository.findLastRequestTimeByUserIdAndDeletedFalse(userId);
    }

    public Optional<LocalDateTime> getLastRequestTimeByIpAddress(String ipAddress) {
        return publicApiRateLimitLogRepository.findLastRequestTimeByIpAddressAndDeletedFalse(ipAddress);
    }

    // Rate Limit 상태별 통계
    public Map<RateLimitStatus, Long> getRateLimitStatusStatistics() {
        List<Object[]> results = publicApiRateLimitLogRepository.findRateLimitStatusStatistics();
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (RateLimitStatus) row[0],
                        row -> (Long) row[1]
                ));
    }

    public Map<RateLimitStatus, Long> getRateLimitStatusStatisticsByApiName(String apiName) {
        List<Object[]> results = publicApiRateLimitLogRepository.findRateLimitStatusStatisticsByApiName(apiName);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (RateLimitStatus) row[0],
                        row -> (Long) row[1]
                ));
    }

    public Map<RateLimitStatus, Long> getRateLimitStatusStatisticsByUserId(String userId) {
        List<Object[]> results = publicApiRateLimitLogRepository.findRateLimitStatusStatisticsByUserId(userId);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (RateLimitStatus) row[0],
                        row -> (Long) row[1]
                ));
    }

    // 특정 시간대 Rate Limit 초과 로그
    public List<PublicApiRateLimitLog> getExceededLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return publicApiRateLimitLogRepository.findByRateLimitStatusAndTimeRangeAndDeletedFalse(
                RateLimitStatus.EXCEEDED, startTime, endTime);
    }

    // 사용량 비율이 높은 로그 조회
    public List<PublicApiRateLimitLog> getHighUsageRatioLogs(double threshold) {
        return publicApiRateLimitLogRepository.findByHighUsageRatioAndDeletedFalse(threshold);
    }

    // 삭제 메서드들
    @Transactional
    public void deleteLog(Long logId) {
        Optional<PublicApiRateLimitLog> logOpt = publicApiRateLimitLogRepository.findById(logId);
        if (logOpt.isPresent()) {
            PublicApiRateLimitLog rateLimitLog = logOpt.get();
            rateLimitLog.setDeleted(true);
            publicApiRateLimitLogRepository.save(rateLimitLog);
            log.info("Rate limit log soft deleted: {}", logId);
        }
    }

    @Transactional
    public void hardDeleteLog(Long logId) {
        publicApiRateLimitLogRepository.deleteById(logId);
        log.info("Rate limit log hard deleted: {}", logId);
    }

    // 사용자별 통계
    public Map<String, Object> getUserStatistics(String userId) {
        long totalRequests = getLogCountByUserId(userId);
        long allowedRequests = getLogCountByUserIdAndStatus(userId, RateLimitStatus.ALLOWED);
        long warningRequests = getLogCountByUserIdAndStatus(userId, RateLimitStatus.WARNING);
        long exceededRequests = getLogCountByUserIdAndStatus(userId, RateLimitStatus.EXCEEDED);
        long blockedRequests = getLogCountByUserIdAndStatus(userId, RateLimitStatus.BLOCKED);

        return Map.of(
                "userId", userId,
                "totalRequests", totalRequests,
                "allowedRequests", allowedRequests,
                "warningRequests", warningRequests,
                "exceededRequests", exceededRequests,
                "blockedRequests", blockedRequests,
                "successRate", totalRequests > 0 ? (double) allowedRequests / totalRequests : 0.0
        );
    }

    // API별 통계
    public Map<String, Object> getApiStatistics(String apiName) {
        long totalRequests = getLogCountByApiName(apiName);
        long allowedRequests = getLogCountByApiNameAndStatus(apiName, RateLimitStatus.ALLOWED);
        long warningRequests = getLogCountByApiNameAndStatus(apiName, RateLimitStatus.WARNING);
        long exceededRequests = getLogCountByApiNameAndStatus(apiName, RateLimitStatus.EXCEEDED);
        long blockedRequests = getLogCountByApiNameAndStatus(apiName, RateLimitStatus.BLOCKED);

        return Map.of(
                "apiName", apiName,
                "totalRequests", totalRequests,
                "allowedRequests", allowedRequests,
                "warningRequests", warningRequests,
                "exceededRequests", exceededRequests,
                "blockedRequests", blockedRequests,
                "successRate", totalRequests > 0 ? (double) allowedRequests / totalRequests : 0.0
        );
    }

    // 전체 통계
    public Map<String, Object> getOverallStatistics() {
        long totalRequests = getTotalLogCount();
        long allowedRequests = getLogCountByStatus(RateLimitStatus.ALLOWED);
        long warningRequests = getLogCountByStatus(RateLimitStatus.WARNING);
        long exceededRequests = getLogCountByStatus(RateLimitStatus.EXCEEDED);
        long blockedRequests = getLogCountByStatus(RateLimitStatus.BLOCKED);

        return Map.of(
                "totalRequests", totalRequests,
                "allowedRequests", allowedRequests,
                "warningRequests", warningRequests,
                "exceededRequests", exceededRequests,
                "blockedRequests", blockedRequests,
                "successRate", totalRequests > 0 ? (double) allowedRequests / totalRequests : 0.0
        );
    }
}
