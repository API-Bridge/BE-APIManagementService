package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.PublicApiManagementLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 공공데이터 API 관리 로그 Repository
 */
@Repository
public interface PublicApiManagementLogRepository extends JpaRepository<PublicApiManagementLog, Long> {

    /**
     * API 이름별 관리 로그 조회
     */
    List<PublicApiManagementLog> findByApiNameAndDeletedFalseOrderByOperationTimeDesc(String apiName);

    /**
     * 특정 기간 내 관리 로그 조회
     */
    List<PublicApiManagementLog> findByOperationTimeBetweenAndDeletedFalseOrderByOperationTimeDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 관리자별 관리 로그 조회
     */
    List<PublicApiManagementLog> findByAdminIdAndDeletedFalseOrderByOperationTimeDesc(String adminId);

    /**
     * 작업 타입별 관리 로그 조회
     */
    List<PublicApiManagementLog> findByOperationTypeAndDeletedFalseOrderByOperationTimeDesc(String operationType);

    /**
     * API 이름별 관리 로그 수 조회
     */
    long countByApiNameAndDeletedFalse(String apiName);

    /**
     * 특정 기간 내 관리 로그 수 조회
     */
    long countByOperationTimeBetweenAndDeletedFalse(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 관리자별 관리 로그 수 조회
     */
    long countByAdminIdAndDeletedFalse(String adminId);

    /**
     * 작업 타입별 관리 로그 수 조회
     */
    long countByOperationTypeAndDeletedFalse(String operationType);

    /**
     * API별 최근 관리 작업 시간 조회
     */
    @Query("SELECT MAX(p.operationTime) FROM PublicApiManagementLog p " +
           "WHERE p.apiName = :apiName AND p.deleted = false")
    LocalDateTime getLastOperationTimeByApiName(@Param("apiName") String apiName);

    /**
     * API별 작업 타입 통계 조회
     */
    @Query("SELECT p.operationType, COUNT(p) FROM PublicApiManagementLog p " +
           "WHERE p.apiName = :apiName AND p.deleted = false " +
           "GROUP BY p.operationType")
    List<Object[]> getOperationTypeStatsByApiName(@Param("apiName") String apiName);

    /**
     * 헬스체크 통과 여부별 관리 로그 조회
     */
    List<PublicApiManagementLog> findByHealthCheckPassedAndDeletedFalseOrderByOperationTimeDesc(Boolean healthCheckPassed);

    /**
     * 복합 검색: API 이름 + 기간 + 작업 타입
     */
    @Query("SELECT p FROM PublicApiManagementLog p WHERE p.apiName = :apiName " +
           "AND p.operationTime BETWEEN :startDate AND :endDate " +
           "AND p.operationType = :operationType AND p.deleted = false " +
           "ORDER BY p.operationTime DESC")
    List<PublicApiManagementLog> findByApiNameAndDateRangeAndOperationType(
            @Param("apiName") String apiName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("operationType") String operationType);

    /**
     * 사용자별 로그 조회 (삭제되지 않은 것만)
     */
    List<PublicApiManagementLog> findByApiNameAndDeletedFalse(String apiName);

    /**
     * 관리자별 로그 조회 (삭제되지 않은 것만)
     */
    List<PublicApiManagementLog> findByAdminIdAndDeletedFalse(String adminId);

    /**
     * 작업 타입별 로그 조회 (삭제되지 않은 것만)
     */
    List<PublicApiManagementLog> findByOperationTypeAndDeletedFalse(String operationType);

    /**
     * 날짜 범위별 로그 조회 (삭제되지 않은 것만)
     */
    List<PublicApiManagementLog> findByOperationTimeBetweenAndDeletedFalse(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 헬스 체크 상태별 로그 조회 (삭제되지 않은 것만)
     */
    List<PublicApiManagementLog> findByHealthCheckPassedAndDeletedFalse(Boolean healthCheckPassed);

    /**
     * API별 최근 로그 조회 (제한된 개수)
     */
    List<PublicApiManagementLog> findTopByApiNameAndDeletedFalseOrderByOperationTimeDesc(String apiName, Pageable pageable);

    /**
     * 관리자별 최근 로그 조회 (제한된 개수)
     */
    List<PublicApiManagementLog> findTopByAdminIdAndDeletedFalseOrderByOperationTimeDesc(String adminId, Pageable pageable);

    /**
     * 전체 최근 로그 조회 (제한된 개수)
     */
    List<PublicApiManagementLog> findTopByDeletedFalseOrderByOperationTimeDesc(Pageable pageable);

    /**
     * 전체 삭제되지 않은 로그 개수
     */
    long countByDeletedFalse();

    /**
     * 관리자별 마지막 작업 시간 조회
     */
    @Query("SELECT MAX(p.operationTime) FROM PublicApiManagementLog p " +
           "WHERE p.adminId = :adminId AND p.deleted = false")
    LocalDateTime findLastOperationTimeByAdminIdAndDeletedFalse(@Param("adminId") String adminId);

    /**
     * API별 마지막 작업 시간 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT MAX(p.operationTime) FROM PublicApiManagementLog p " +
           "WHERE p.apiName = :apiName AND p.deleted = false")
    LocalDateTime findLastOperationTimeByApiNameAndDeletedFalse(@Param("apiName") String apiName);
}
