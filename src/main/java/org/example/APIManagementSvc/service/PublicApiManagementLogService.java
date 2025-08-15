package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.PublicApiManagementLog;
import org.example.APIManagementSvc.domain.enums.OperationType;
import org.example.APIManagementSvc.repository.PublicApiManagementLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 공개 API 관리 로그 서비스
 * 공개 API의 관리 작업(활성화/비활성화, 상태 변경 등)을 로그로 기록합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicApiManagementLogService {

    private final PublicApiManagementLogRepository publicApiManagementLogRepository;

    /**
     * API 관리 작업 로그 생성
     */
    @Transactional
    public PublicApiManagementLog createLog(String apiName, String adminId, 
                                          OperationType operationType, String operationReason, 
                                          boolean healthCheckPassed) {
        log.info("Creating public API management log for API: {}, operation: {}, admin: {}", 
                apiName, operationType, adminId);

        PublicApiManagementLog logEntry = PublicApiManagementLog.builder()
                .apiName(apiName)
                .adminId(adminId)
                .operationType(operationType.name())
                .operationReason(operationReason)
                .healthCheckPassed(healthCheckPassed)
                .deleted(false)
                .build();

        PublicApiManagementLog savedLog = publicApiManagementLogRepository.save(logEntry);
        log.info("Public API management log created with ID: {}", savedLog.getLogId());

        return savedLog;
    }

    /**
     * API 활성화 로그 생성 (간편 메서드)
     */
    @Transactional
    public PublicApiManagementLog recordApiActivation(String apiName, String adminId, 
                                                    String reason, boolean healthCheckPassed) {
        return createLog(apiName, adminId, OperationType.ACTIVATE, reason, healthCheckPassed);
    }

    /**
     * API 비활성화 로그 생성 (간편 메서드)
     */
    @Transactional
    public PublicApiManagementLog recordApiDeactivation(String apiName, String adminId, 
                                                      String reason, boolean healthCheckPassed) {
        return createLog(apiName, adminId, OperationType.DEACTIVATE, reason, healthCheckPassed);
    }

    /**
     * API 상태 변경 로그 생성 (간편 메서드)
     */
    @Transactional
    public PublicApiManagementLog recordApiStatusChange(String apiName, String adminId, 
                                                      String reason, boolean healthCheckPassed) {
        return createLog(apiName, adminId, OperationType.STATUS_CHANGE, reason, healthCheckPassed);
    }

    /**
     * API 설정 변경 로그 생성 (간편 메서드)
     */
    @Transactional
    public PublicApiManagementLog recordApiConfigChange(String apiName, String adminId, 
                                                      String reason, boolean healthCheckPassed) {
        return createLog(apiName, adminId, OperationType.CONFIG_CHANGE, reason, healthCheckPassed);
    }

    /**
     * 로그 ID로 로그 조회
     */
    public Optional<PublicApiManagementLog> getLogById(Long logId) {
        log.debug("Fetching public API management log by ID: {}", logId);
        return publicApiManagementLogRepository.findById(logId);
    }

    /**
     * API 이름별 로그 조회
     */
    public List<PublicApiManagementLog> getLogsByApiName(String apiName) {
        log.debug("Fetching public API management logs for API: {}", apiName);
        return publicApiManagementLogRepository.findByApiNameAndDeletedFalse(apiName);
    }

    /**
     * 관리자별 로그 조회
     */
    public List<PublicApiManagementLog> getLogsByAdmin(String adminId) {
        log.debug("Fetching public API management logs by admin: {}", adminId);
        return publicApiManagementLogRepository.findByAdminIdAndDeletedFalse(adminId);
    }

    /**
     * 작업 타입별 로그 조회
     */
    public List<PublicApiManagementLog> getLogsByOperationType(OperationType operationType) {
        log.debug("Fetching public API management logs by operation type: {}", operationType);
        return publicApiManagementLogRepository.findByOperationTypeAndDeletedFalse(operationType.name());
    }

    /**
     * 날짜 범위별 로그 조회
     */
    public List<PublicApiManagementLog> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching public API management logs from {} to {}", startDate, endDate);
        return publicApiManagementLogRepository.findByOperationTimeBetweenAndDeletedFalse(startDate, endDate);
    }

    /**
     * 헬스 체크 상태별 로그 조회
     */
    public List<PublicApiManagementLog> getLogsByHealthCheckStatus(boolean healthCheckPassed) {
        log.debug("Fetching public API management logs with health check status: {}", healthCheckPassed);
        return publicApiManagementLogRepository.findByHealthCheckPassedAndDeletedFalse(healthCheckPassed);
    }

    /**
     * API별 최근 로그 조회
     */
    public List<PublicApiManagementLog> getRecentLogsByApi(String apiName, int limit) {
        log.debug("Fetching recent {} management logs for API: {}", limit, apiName);
        Pageable pageable = PageRequest.of(0, limit);
        return publicApiManagementLogRepository.findTopByApiNameAndDeletedFalseOrderByOperationTimeDesc(apiName, pageable);
    }

    /**
     * 관리자별 최근 로그 조회
     */
    public List<PublicApiManagementLog> getRecentLogsByAdmin(String adminId, int limit) {
        log.debug("Fetching recent {} management logs by admin: {}", limit, adminId);
        Pageable pageable = PageRequest.of(0, limit);
        return publicApiManagementLogRepository.findTopByAdminIdAndDeletedFalseOrderByOperationTimeDesc(adminId, pageable);
    }

    /**
     * 전체 최근 로그 조회
     */
    public List<PublicApiManagementLog> getRecentLogs(int limit) {
        log.debug("Fetching recent {} management logs", limit);
        Pageable pageable = PageRequest.of(0, limit);
        return publicApiManagementLogRepository.findTopByDeletedFalseOrderByOperationTimeDesc(pageable);
    }

    /**
     * API별 로그 개수 조회
     */
    public long getLogCountByApi(String apiName) {
        log.debug("Getting management log count for API: {}", apiName);
        return publicApiManagementLogRepository.countByApiNameAndDeletedFalse(apiName);
    }

    /**
     * 관리자별 로그 개수 조회
     */
    public long getLogCountByAdmin(String adminId) {
        log.debug("Getting management log count for admin: {}", adminId);
        return publicApiManagementLogRepository.countByAdminIdAndDeletedFalse(adminId);
    }

    /**
     * 전체 로그 개수 조회
     */
    public long getTotalLogCount() {
        log.debug("Getting total management log count");
        return publicApiManagementLogRepository.countByDeletedFalse();
    }

    /**
     * 작업 타입별 로그 개수 조회
     */
    public long getLogCountByOperationType(OperationType operationType) {
        log.debug("Getting management log count for operation type: {}", operationType);
        return publicApiManagementLogRepository.countByOperationTypeAndDeletedFalse(operationType.name());
    }

    /**
     * API별 마지막 작업 시간 조회
     */
    public LocalDateTime getLastOperationTimeByApi(String apiName) {
        log.debug("Getting last operation time for API: {}", apiName);
        return publicApiManagementLogRepository.findLastOperationTimeByApiNameAndDeletedFalse(apiName);
    }

    /**
     * 관리자별 마지막 작업 시간 조회
     */
    public LocalDateTime getLastOperationTimeByAdmin(String adminId) {
        log.debug("Getting last operation time for admin: {}", adminId);
        return publicApiManagementLogRepository.findLastOperationTimeByAdminIdAndDeletedFalse(adminId);
    }

    /**
     * 작업 타입별 통계 조회
     */
    public OperationTypeStatistics getOperationTypeStatistics() {
        log.debug("Getting operation type statistics");
        
        long totalLogs = getTotalLogCount();
        long activationLogs = getLogCountByOperationType(OperationType.ACTIVATE);
        long deactivationLogs = getLogCountByOperationType(OperationType.DEACTIVATE);
        long statusChangeLogs = getLogCountByOperationType(OperationType.STATUS_CHANGE);
        long configChangeLogs = getLogCountByOperationType(OperationType.CONFIG_CHANGE);
        
        return OperationTypeStatistics.builder()
                .totalLogs(totalLogs)
                .activationLogs(activationLogs)
                .deactivationLogs(deactivationLogs)
                .statusChangeLogs(statusChangeLogs)
                .configChangeLogs(configChangeLogs)
                .build();
    }

    /**
     * 로그 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteLog(Long logId) {
        log.info("Soft deleting public API management log: {}", logId);
        
        Optional<PublicApiManagementLog> logOptional = publicApiManagementLogRepository.findById(logId);
        if (logOptional.isPresent()) {
            PublicApiManagementLog logEntry = logOptional.get();
            logEntry.setDeleted(true);
            publicApiManagementLogRepository.save(logEntry);
            log.info("Public API management log soft deleted: {}", logId);
        } else {
            log.warn("Public API management log not found for deletion: {}", logId);
            throw new IllegalArgumentException("로그를 찾을 수 없습니다: " + logId);
        }
    }

    /**
     * 로그 영구 삭제 (Hard Delete)
     */
    @Transactional
    public void hardDeleteLog(Long logId) {
        log.info("Hard deleting public API management log: {}", logId);
        publicApiManagementLogRepository.deleteById(logId);
        log.info("Public API management log hard deleted: {}", logId);
    }

    /**
     * 작업 타입별 통계 내부 클래스
     */
    public static class OperationTypeStatistics {
        private final long totalLogs;
        private final long activationLogs;
        private final long deactivationLogs;
        private final long statusChangeLogs;
        private final long configChangeLogs;

        public static Builder builder() {
            return new Builder();
        }

        private OperationTypeStatistics(Builder builder) {
            this.totalLogs = builder.totalLogs;
            this.activationLogs = builder.activationLogs;
            this.deactivationLogs = builder.deactivationLogs;
            this.statusChangeLogs = builder.statusChangeLogs;
            this.configChangeLogs = builder.configChangeLogs;
        }

        public long getTotalLogs() { return totalLogs; }
        public long getActivationLogs() { return activationLogs; }
        public long getDeactivationLogs() { return deactivationLogs; }
        public long getStatusChangeLogs() { return statusChangeLogs; }
        public long getConfigChangeLogs() { return configChangeLogs; }

        public static class Builder {
            private long totalLogs;
            private long activationLogs;
            private long deactivationLogs;
            private long statusChangeLogs;
            private long configChangeLogs;

            public Builder totalLogs(long totalLogs) {
                this.totalLogs = totalLogs;
                return this;
            }

            public Builder activationLogs(long activationLogs) {
                this.activationLogs = activationLogs;
                return this;
            }

            public Builder deactivationLogs(long deactivationLogs) {
                this.deactivationLogs = deactivationLogs;
                return this;
            }

            public Builder statusChangeLogs(long statusChangeLogs) {
                this.statusChangeLogs = statusChangeLogs;
                return this;
            }

            public Builder configChangeLogs(long configChangeLogs) {
                this.configChangeLogs = configChangeLogs;
                return this;
            }

            public OperationTypeStatistics build() {
                return new OperationTypeStatistics(this);
            }
        }
    }
}
