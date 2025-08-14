package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.Entity.CustomApiCreationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 커스텀 API 생성 로그 Repository
 */
@Repository
public interface CustomApiCreationLogRepository extends JpaRepository<CustomApiCreationLog, Long> {

    /**
     * 사용자별 생성 로그 조회
     */
    List<CustomApiCreationLog> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(String userId);

    /**
     * 특정 기간 내 생성 로그 조회
     */
    List<CustomApiCreationLog> findByCreatedAtBetweenAndDeletedFalseOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 커스텀 API 이름별 생성 로그 조회
     */
    List<CustomApiCreationLog> findByCustomApiNameAndDeletedFalseOrderByCreatedAtDesc(String customApiName);

    /**
     * BYOK 사용 여부별 생성 로그 조회
     */
    List<CustomApiCreationLog> findByByokUsedAndDeletedFalseOrderByCreatedAtDesc(Boolean byokUsed);

    /**
     * 사용자별 생성 로그 수 조회
     */
    long countByUserIdAndDeletedFalse(String userId);

    /**
     * 특정 기간 내 생성 로그 수 조회
     */
    long countByCreatedAtBetweenAndDeletedFalse(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 복합 검색: 사용자 + 기간 + BYOK 사용 여부
     */
    @Query("SELECT c FROM CustomApiCreationLog c WHERE c.userId = :userId " +
           "AND c.createdAt BETWEEN :startDate AND :endDate " +
           "AND c.byokUsed = :byokUsed AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    List<CustomApiCreationLog> findByUserIdAndDateRangeAndByokUsed(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("byokUsed") Boolean byokUsed);

    /**
     * 사용자별 로그 조회 (삭제되지 않은 것만)
     */
    List<CustomApiCreationLog> findByUserIdAndDeletedFalse(String userId);

    /**
     * 날짜 범위별 로그 조회 (삭제되지 않은 것만)
     */
    List<CustomApiCreationLog> findByCreatedAtBetweenAndDeletedFalse(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * BYOK 사용 여부별 로그 조회 (삭제되지 않은 것만)
     */
    List<CustomApiCreationLog> findByByokUsedAndDeletedFalse(Boolean byokUsed);

    /**
     * 커스텀 API 이름별 로그 조회 (삭제되지 않은 것만)
     */
    List<CustomApiCreationLog> findByCustomApiNameAndDeletedFalse(String customApiName);

    /**
     * 전체 삭제되지 않은 로그 개수
     */
    long countByDeletedFalse();

    /**
     * 사용자별 최근 로그 조회 (제한된 개수)
     */
    List<CustomApiCreationLog> findTopByUserIdAndDeletedFalseOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * 전체 최근 로그 조회 (제한된 개수)
     */
    List<CustomApiCreationLog> findTopByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);
}
