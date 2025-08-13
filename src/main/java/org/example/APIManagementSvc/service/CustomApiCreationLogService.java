package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.CustomApiCreationLog;
import org.example.APIManagementSvc.repository.CustomApiCreationLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 커스텀 API 생성 로그 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomApiCreationLogService {

    private final CustomApiCreationLogRepository customApiCreationLogRepository;

    /**
     * 커스텀 API 생성 로그 저장
     */
    @Transactional
    public CustomApiCreationLog createLog(String userId, String customApiName, 
                                        String combinedApis, boolean byokUsed) {
        log.info("Creating custom API creation log for user: {}, API: {}", userId, customApiName);

        CustomApiCreationLog logEntry = CustomApiCreationLog.builder()
                .userId(userId)
                .customApiName(customApiName)
                .combinedApis(combinedApis)
                .byokUsed(byokUsed)
                .deleted(false)
                .build();

        CustomApiCreationLog savedLog = customApiCreationLogRepository.save(logEntry);
        log.info("Custom API creation log created with ID: {}", savedLog.getLogId());

        return savedLog;
    }

    /**
     * 로그 ID로 로그 조회
     */
    public Optional<CustomApiCreationLog> getLogById(Long logId) {
        log.debug("Fetching custom API creation log by ID: {}", logId);
        return customApiCreationLogRepository.findById(logId);
    }

    /**
     * 사용자별 로그 조회
     */
    public List<CustomApiCreationLog> getLogsByUser(String userId) {
        log.debug("Fetching custom API creation logs for user: {}", userId);
        return customApiCreationLogRepository.findByUserIdAndDeletedFalse(userId);
    }

    /**
     * 날짜 범위별 로그 조회
     */
    public List<CustomApiCreationLog> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching custom API creation logs from {} to {}", startDate, endDate);
        return customApiCreationLogRepository.findByCreatedAtBetweenAndDeletedFalse(startDate, endDate);
    }

    /**
     * BYOK 사용 여부별 로그 조회
     */
    public List<CustomApiCreationLog> getLogsByByokUsage(boolean byokUsed) {
        log.debug("Fetching custom API creation logs with BYOK usage: {}", byokUsed);
        return customApiCreationLogRepository.findByByokUsedAndDeletedFalse(byokUsed);
    }

    /**
     * 커스텀 API 이름별 로그 조회
     */
    public List<CustomApiCreationLog> getLogsByCustomApiName(String customApiName) {
        log.debug("Fetching custom API creation logs for API: {}", customApiName);
        return customApiCreationLogRepository.findByCustomApiNameAndDeletedFalse(customApiName);
    }

    /**
     * 사용자별 로그 개수 조회
     */
    public long getLogCountByUser(String userId) {
        log.debug("Getting log count for user: {}", userId);
        return customApiCreationLogRepository.countByUserIdAndDeletedFalse(userId);
    }

    /**
     * 전체 로그 개수 조회
     */
    public long getTotalLogCount() {
        log.debug("Getting total log count");
        return customApiCreationLogRepository.countByDeletedFalse();
    }

    /**
     * 로그 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteLog(Long logId) {
        log.info("Soft deleting custom API creation log: {}", logId);
        
        Optional<CustomApiCreationLog> logOptional = customApiCreationLogRepository.findById(logId);
        if (logOptional.isPresent()) {
            CustomApiCreationLog logEntry = logOptional.get();
            logEntry.setDeleted(true);
            customApiCreationLogRepository.save(logEntry);
            log.info("Custom API creation log soft deleted: {}", logId);
        } else {
            log.warn("Custom API creation log not found for deletion: {}", logId);
            throw new IllegalArgumentException("로그를 찾을 수 없습니다: " + logId);
        }
    }

    /**
     * 로그 영구 삭제 (Hard Delete)
     */
    @Transactional
    public void hardDeleteLog(Long logId) {
        log.info("Hard deleting custom API creation log: {}", logId);
        customApiCreationLogRepository.deleteById(logId);
        log.info("Custom API creation log hard deleted: {}", logId);
    }

    /**
     * 사용자별 최근 로그 조회
     */
    public List<CustomApiCreationLog> getRecentLogsByUser(String userId, int limit) {
        log.debug("Fetching recent {} logs for user: {}", limit, userId);
        Pageable pageable = PageRequest.of(0, limit);
        return customApiCreationLogRepository.findTopByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 전체 최근 로그 조회
     */
    public List<CustomApiCreationLog> getRecentLogs(int limit) {
        log.debug("Fetching recent {} logs", limit);
        Pageable pageable = PageRequest.of(0, limit);
        return customApiCreationLogRepository.findTopByDeletedFalseOrderByCreatedAtDesc(pageable);
    }
}
