package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.CustomApiUsageLog;
import org.example.APIManagementSvc.repository.CustomApiUsageLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 커스텀 API 사용 로그 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomApiUsageLogService {

    private final CustomApiUsageLogRepository customApiUsageLogRepository;

    /**
     * 커스텀 API 사용 로그 저장
     */
    @Transactional
    public CustomApiUsageLog createLog(String userId, String customApiName, 
                                     String usedPublicApis, Long totalResponseTime, 
                                     Long responseDataSize) {
        log.info("Creating custom API usage log for user: {}, API: {}", userId, customApiName);

        CustomApiUsageLog logEntry = CustomApiUsageLog.builder()
                .userId(userId)
                .customApiName(customApiName)
                .usedPublicApis(usedPublicApis)
                .totalResponseTime(totalResponseTime)
                .responseDataSize(responseDataSize)
                .deleted(false)
                .build();

        CustomApiUsageLog savedLog = customApiUsageLogRepository.save(logEntry);
        log.info("Custom API usage log created with ID: {}", savedLog.getLogId());

        return savedLog;
    }

    /**
     * 로그 ID로 로그 조회
     */
    public Optional<CustomApiUsageLog> getLogById(Long logId) {
        log.debug("Fetching custom API usage log by ID: {}", logId);
        return customApiUsageLogRepository.findById(logId);
    }

    /**
     * 사용자별 로그 조회
     */
    public List<CustomApiUsageLog> getLogsByUser(String userId) {
        log.debug("Fetching custom API usage logs for user: {}", userId);
        return customApiUsageLogRepository.findByUserIdAndDeletedFalse(userId);
    }

    /**
     * 날짜 범위별 로그 조회
     */
    public List<CustomApiUsageLog> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching custom API usage logs from {} to {}", startDate, endDate);
        return customApiUsageLogRepository.findByRequestTimeBetweenAndDeletedFalse(startDate, endDate);
    }

    /**
     * 커스텀 API 이름별 로그 조회
     */
    public List<CustomApiUsageLog> getLogsByCustomApiName(String customApiName) {
        log.debug("Fetching custom API usage logs for API: {}", customApiName);
        return customApiUsageLogRepository.findByCustomApiNameAndDeletedFalse(customApiName);
    }

    /**
     * 응답 시간 범위별 로그 조회
     */
    public List<CustomApiUsageLog> getLogsByResponseTimeRange(Long minTime, Long maxTime) {
        log.debug("Fetching custom API usage logs with response time between {} and {}", minTime, maxTime);
        return customApiUsageLogRepository.findByTotalResponseTimeBetweenAndDeletedFalse(minTime, maxTime);
    }

    /**
     * 사용자별 로그 개수 조회
     */
    public long getLogCountByUser(String userId) {
        log.debug("Getting usage log count for user: {}", userId);
        return customApiUsageLogRepository.countByUserIdAndDeletedFalse(userId);
    }

    /**
     * 전체 로그 개수 조회
     */
    public long getTotalLogCount() {
        log.debug("Getting total usage log count");
        return customApiUsageLogRepository.countByDeletedFalse();
    }

    /**
     * 사용자별 평균 응답 시간 조회
     */
    public Double getAverageResponseTimeByUser(String userId) {
        log.debug("Getting average response time for user: {}", userId);
        return customApiUsageLogRepository.findAverageResponseTimeByUserIdAndDeletedFalse(userId);
    }

    /**
     * 전체 평균 응답 시간 조회
     */
    public Double getOverallAverageResponseTime() {
        log.debug("Getting overall average response time");
        return customApiUsageLogRepository.findAverageResponseTimeByDeletedFalse();
    }

    /**
     * 로그 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteLog(Long logId) {
        log.info("Soft deleting custom API usage log: {}", logId);
        
        Optional<CustomApiUsageLog> logOptional = customApiUsageLogRepository.findById(logId);
        if (logOptional.isPresent()) {
            CustomApiUsageLog logEntry = logOptional.get();
            logEntry.setDeleted(true);
            customApiUsageLogRepository.save(logEntry);
            log.info("Custom API usage log soft deleted: {}", logId);
        } else {
            log.warn("Custom API usage log not found for deletion: {}", logId);
            throw new IllegalArgumentException("로그를 찾을 수 없습니다: " + logId);
        }
    }

    /**
     * 로그 영구 삭제 (Hard Delete)
     */
    @Transactional
    public void hardDeleteLog(Long logId) {
        log.info("Hard deleting custom API usage log: {}", logId);
        customApiUsageLogRepository.deleteById(logId);
        log.info("Custom API usage log hard deleted: {}", logId);
    }

    /**
     * 사용자별 최근 로그 조회
     */
    public List<CustomApiUsageLog> getRecentLogsByUser(String userId, int limit) {
        log.debug("Fetching recent {} usage logs for user: {}", limit, userId);
        Pageable pageable = PageRequest.of(0, limit);
        return customApiUsageLogRepository.findTopByUserIdAndDeletedFalseOrderByRequestTimeDesc(userId, pageable);
    }

    /**
     * 전체 최근 로그 조회
     */
    public List<CustomApiUsageLog> getRecentLogs(int limit) {
        log.debug("Fetching recent {} usage logs", limit);
        Pageable pageable = PageRequest.of(0, limit);
        return customApiUsageLogRepository.findTopByDeletedFalseOrderByRequestTimeDesc(pageable);
    }

    /**
     * 사용자별 통계 정보 조회
     */
    public UsageStatistics getUserStatistics(String userId) {
        log.debug("Getting usage statistics for user: {}", userId);
        
        long totalUsage = getLogCountByUser(userId);
        Double avgResponseTime = getAverageResponseTimeByUser(userId);
        List<CustomApiUsageLog> recentLogs = getRecentLogsByUser(userId, 5);
        
        return UsageStatistics.builder()
                .userId(userId)
                .totalUsage(totalUsage)
                .averageResponseTime(avgResponseTime)
                .recentLogs(recentLogs)
                .build();
    }

    /**
     * 전체 통계 정보 조회
     */
    public OverallUsageStatistics getOverallStatistics() {
        log.debug("Getting overall usage statistics");
        
        long totalUsage = getTotalLogCount();
        Double avgResponseTime = getOverallAverageResponseTime();
        List<CustomApiUsageLog> recentLogs = getRecentLogs(10);
        
        return OverallUsageStatistics.builder()
                .totalUsage(totalUsage)
                .averageResponseTime(avgResponseTime)
                .recentLogs(recentLogs)
                .build();
    }

    /**
     * 사용자별 사용 통계 내부 클래스
     */
    public static class UsageStatistics {
        private final String userId;
        private final long totalUsage;
        private final Double averageResponseTime;
        private final List<CustomApiUsageLog> recentLogs;

        public static Builder builder() {
            return new Builder();
        }

        private UsageStatistics(Builder builder) {
            this.userId = builder.userId;
            this.totalUsage = builder.totalUsage;
            this.averageResponseTime = builder.averageResponseTime;
            this.recentLogs = builder.recentLogs;
        }

        public String getUserId() { return userId; }
        public long getTotalUsage() { return totalUsage; }
        public Double getAverageResponseTime() { return averageResponseTime; }
        public List<CustomApiUsageLog> getRecentLogs() { return recentLogs; }

        public static class Builder {
            private String userId;
            private long totalUsage;
            private Double averageResponseTime;
            private List<CustomApiUsageLog> recentLogs;

            public Builder userId(String userId) {
                this.userId = userId;
                return this;
            }

            public Builder totalUsage(long totalUsage) {
                this.totalUsage = totalUsage;
                return this;
            }

            public Builder averageResponseTime(Double averageResponseTime) {
                this.averageResponseTime = averageResponseTime;
                return this;
            }

            public Builder recentLogs(List<CustomApiUsageLog> recentLogs) {
                this.recentLogs = recentLogs;
                return this;
            }

            public UsageStatistics build() {
                return new UsageStatistics(this);
            }
        }
    }

    /**
     * 전체 사용 통계 내부 클래스
     */
    public static class OverallUsageStatistics {
        private final long totalUsage;
        private final Double averageResponseTime;
        private final List<CustomApiUsageLog> recentLogs;

        public static Builder builder() {
            return new Builder();
        }

        private OverallUsageStatistics(Builder builder) {
            this.totalUsage = builder.totalUsage;
            this.averageResponseTime = builder.averageResponseTime;
            this.recentLogs = builder.recentLogs;
        }

        public long getTotalUsage() { return totalUsage; }
        public Double getAverageResponseTime() { return averageResponseTime; }
        public List<CustomApiUsageLog> getRecentLogs() { return recentLogs; }

        public static class Builder {
            private long totalUsage;
            private Double averageResponseTime;
            private List<CustomApiUsageLog> recentLogs;

            public Builder totalUsage(long totalUsage) {
                this.totalUsage = totalUsage;
                return this;
            }

            public Builder averageResponseTime(Double averageResponseTime) {
                this.averageResponseTime = averageResponseTime;
                return this;
            }

            public Builder recentLogs(List<CustomApiUsageLog> recentLogs) {
                this.recentLogs = recentLogs;
                return this;
            }

            public OverallUsageStatistics build() {
                return new OverallUsageStatistics(this);
            }
        }
    }
}
