package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.ApiLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ApiLog 엔티티를 위한 Repository
 * API 로그 및 감사 기능을 제공합니다.
 */
@Repository
public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {

    // === 기본 조회 메서드 ===

    /**
     * 로그 ID로 조회
     */
    Optional<ApiLog> findById(Long id);

    /**
     * API ID로 로그 목록 조회
     */
    List<ApiLog> findByApiId(String apiId);

    /**
     * 사용자 ID로 로그 목록 조회
     */
    List<ApiLog> findByUserId(String userId);

    /**
     * 관리자 ID로 로그 목록 조회
     */
    List<ApiLog> findByAdminId(String adminId);

    /**
     * IP 주소로 로그 목록 조회
     */
    List<ApiLog> findByIpAddress(String ipAddress);

    /**
     * 세션 ID로 로그 목록 조회
     */
    List<ApiLog> findBySessionId(String sessionId);

    /**
     * 요청 ID로 로그 목록 조회
     */
    List<ApiLog> findByRequestId(String requestId);

    /**
     * 제공자로 로그 목록 조회
     */
    List<ApiLog> findByProvider(String provider);

    // === 로그 타입 기반 조회 ===

    /**
     * 로그 타입으로 조회
     */
    List<ApiLog> findByLogType(ApiLog.LogType logType);

    /**
     * 로그 레벨로 조회
     */
    List<ApiLog> findByLogLevel(ApiLog.LogLevel logLevel);

    /**
     * 성공한 로그 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.success = true AND l.deleted = false")
    List<ApiLog> findSuccessfulLogs();

    /**
     * 실패한 로그 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.success = false AND l.deleted = false")
    List<ApiLog> findFailedLogs();

    /**
     * API ID와 로그 타입으로 조회
     */
    List<ApiLog> findByApiIdAndLogType(String apiId, ApiLog.LogType logType);

    /**
     * 사용자 ID와 로그 타입으로 조회
     */
    List<ApiLog> findByUserIdAndLogType(String userId, ApiLog.LogType logType);

    // === 시간 기반 조회 ===

    /**
     * 특정 기간 내 로그 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.createdAt BETWEEN :startTime AND :endTime AND l.deleted = false ORDER BY l.createdAt DESC")
    List<ApiLog> findLogsBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 최근 로그 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.createdAt >= :since AND l.deleted = false ORDER BY l.createdAt DESC")
    List<ApiLog> findRecentLogs(@Param("since") LocalDateTime since);

    /**
     * 오래된 로그 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.createdAt <= :before AND l.deleted = false ORDER BY l.createdAt ASC")
    List<ApiLog> findOldLogs(@Param("before") LocalDateTime before);

    // === 처리 시간 기반 조회 ===

    /**
     * 긴 처리 시간을 가진 로그 조회 (1초 이상)
     */
    @Query("SELECT l FROM ApiLog l WHERE l.duration >= 1000 AND l.deleted = false ORDER BY l.duration DESC")
    List<ApiLog> findSlowLogs();

    /**
     * 빠른 처리 시간을 가진 로그 조회 (100ms 이하)
     */
    @Query("SELECT l FROM ApiLog l WHERE l.duration <= 100 AND l.deleted = false ORDER BY l.duration ASC")
    List<ApiLog> findFastLogs();

    /**
     * 특정 범위의 처리 시간을 가진 로그 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.duration BETWEEN :minDuration AND :maxDuration AND l.deleted = false ORDER BY l.duration DESC")
    List<ApiLog> findLogsByDurationRange(@Param("minDuration") Long minDuration, @Param("maxDuration") Long maxDuration);

    // === 에러 기반 조회 ===

    /**
     * 에러 코드로 조회
     */
    List<ApiLog> findByErrorCode(String errorCode);

    /**
     * 에러 메시지에 검색어가 포함된 로그 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.errorMessage LIKE %:searchTerm% AND l.deleted = false")
    List<ApiLog> findByErrorMessageContaining(@Param("searchTerm") String searchTerm);

    /**
     * 특정 에러 코드를 가진 실패한 로그 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.errorCode = :errorCode AND l.success = false AND l.deleted = false")
    List<ApiLog> findFailedLogsByErrorCode(@Param("errorCode") String errorCode);

    // === 메시지 기반 조회 ===

    /**
     * 로그 메시지에 검색어가 포함된 로그 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.message LIKE %:searchTerm% AND l.deleted = false")
    List<ApiLog> findByMessageContaining(@Param("searchTerm") String searchTerm);

    /**
     * 상세 정보에 검색어가 포함된 로그 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.details LIKE %:searchTerm% AND l.deleted = false")
    List<ApiLog> findByDetailsContaining(@Param("searchTerm") String searchTerm);

    // === 태그 기반 조회 ===

    /**
     * 태그에 검색어가 포함된 로그 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.tags LIKE %:searchTerm% AND l.deleted = false")
    List<ApiLog> findByTagsContaining(@Param("searchTerm") String searchTerm);

    /**
     * 특정 태그를 가진 로그 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.tags LIKE %:tag% AND l.deleted = false")
    List<ApiLog> findByTag(@Param("tag") String tag);

    // === 복합 조건 조회 ===

    /**
     * API ID와 성공 여부로 조회
     */
    List<ApiLog> findByApiIdAndSuccess(String apiId, Boolean success);

    /**
     * 사용자 ID와 성공 여부로 조회
     */
    List<ApiLog> findByUserIdAndSuccess(String userId, Boolean success);

    /**
     * 로그 타입과 성공 여부로 조회
     */
    List<ApiLog> findByLogTypeAndSuccess(ApiLog.LogType logType, Boolean success);

    /**
     * 로그 레벨과 성공 여부로 조회
     */
    List<ApiLog> findByLogLevelAndSuccess(ApiLog.LogLevel logLevel, Boolean success);

    // === 페이징 조회 ===

    /**
     * API ID로 페이징 조회
     */
    Page<ApiLog> findByApiId(String apiId, Pageable pageable);

    /**
     * 사용자 ID로 페이징 조회
     */
    Page<ApiLog> findByUserId(String userId, Pageable pageable);

    /**
     * 로그 타입으로 페이징 조회
     */
    Page<ApiLog> findByLogType(ApiLog.LogType logType, Pageable pageable);

    /**
     * 성공한 로그 페이징 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.success = true AND l.deleted = false")
    Page<ApiLog> findSuccessfulLogs(Pageable pageable);

    /**
     * 실패한 로그 페이징 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.success = false AND l.deleted = false")
    Page<ApiLog> findFailedLogs(Pageable pageable);

    // === 통계 조회 ===

    /**
     * API별 로그 통계
     */
    @Query("SELECT l.apiId, COUNT(l), COUNT(CASE WHEN l.success = true THEN 1 END), AVG(l.duration) FROM ApiLog l WHERE l.deleted = false GROUP BY l.apiId")
    List<Object[]> getLogStatsByApi();

    /**
     * 사용자별 로그 통계
     */
    @Query("SELECT l.userId, COUNT(l), COUNT(CASE WHEN l.success = true THEN 1 END), AVG(l.duration) FROM ApiLog l WHERE l.deleted = false GROUP BY l.userId")
    List<Object[]> getLogStatsByUser();

    /**
     * 로그 타입별 통계
     */
    @Query("SELECT l.logType, COUNT(l), COUNT(CASE WHEN l.success = true THEN 1 END), AVG(l.duration) FROM ApiLog l WHERE l.deleted = false GROUP BY l.logType")
    List<Object[]> getLogStatsByType();

    /**
     * 제공자별 로그 통계
     */
    @Query("SELECT l.provider, COUNT(l), COUNT(CASE WHEN l.success = true THEN 1 END), AVG(l.duration) FROM ApiLog l WHERE l.deleted = false GROUP BY l.provider")
    List<Object[]> getLogStatsByProvider();

    /**
     * 에러 코드별 통계
     */
    @Query("SELECT l.errorCode, COUNT(l) FROM ApiLog l WHERE l.errorCode IS NOT NULL AND l.deleted = false GROUP BY l.errorCode")
    List<Object[]> getLogStatsByErrorCode();

    // === 존재 여부 확인 ===

    /**
     * 로그 ID 존재 여부 확인
     */
    boolean existsById(Long id);

    /**
     * API ID와 로그 타입 조합 존재 여부 확인
     */
    boolean existsByApiIdAndLogType(String apiId, ApiLog.LogType logType);

    // === 삭제된 로그 조회 ===

    /**
     * 삭제된 로그 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.deleted = true")
    List<ApiLog> findDeletedLogs();

    /**
     * API의 삭제된 로그 조회
     */
    @Query("SELECT l FROM ApiLog l WHERE l.apiId = :apiId AND l.deleted = true")
    List<ApiLog> findDeletedLogsByApiId(@Param("apiId") String apiId);

    // === 로그 검증 ===

    /**
     * API의 로그 개수 조회
     */
    @Query("SELECT COUNT(l) FROM ApiLog l WHERE l.apiId = :apiId AND l.deleted = false")
    long countByApiId(@Param("apiId") String apiId);

    /**
     * 사용자의 로그 개수 조회
     */
    @Query("SELECT COUNT(l) FROM ApiLog l WHERE l.userId = :userId AND l.deleted = false")
    long countByUserId(@Param("userId") String userId);

    /**
     * 성공한 로그 개수 조회
     */
    @Query("SELECT COUNT(l) FROM ApiLog l WHERE l.success = true AND l.deleted = false")
    long countSuccessfulLogs();

    /**
     * 실패한 로그 개수 조회
     */
    @Query("SELECT COUNT(l) FROM ApiLog l WHERE l.success = false AND l.deleted = false")
    long countFailedLogs();

    /**
     * 특정 로그 타입의 개수 조회
     */
    @Query("SELECT COUNT(l) FROM ApiLog l WHERE l.logType = :logType AND l.deleted = false")
    long countByLogType(@Param("logType") ApiLog.LogType logType);
}
