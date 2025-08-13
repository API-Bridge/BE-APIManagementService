package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.CustomApiUsageLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 커스텀 API 사용 로그 Repository
 */
@Repository
public interface CustomApiUsageLogRepository extends JpaRepository<CustomApiUsageLog, Long> {

    /**
     * 사용자별 사용 로그 조회
     */
    List<CustomApiUsageLog> findByUserIdAndDeletedFalseOrderByRequestTimeDesc(String userId);

    /**
     * 특정 기간 내 사용 로그 조회
     */
    List<CustomApiUsageLog> findByRequestTimeBetweenAndDeletedFalseOrderByRequestTimeDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 커스텀 API 이름별 사용 로그 조회
     */
    List<CustomApiUsageLog> findByCustomApiNameAndDeletedFalseOrderByRequestTimeDesc(String customApiName);

    /**
     * 응답 시간 범위별 사용 로그 조회 (성능 분석용)
     */
    List<CustomApiUsageLog> findByTotalResponseTimeBetweenAndDeletedFalseOrderByRequestTimeDesc(
            Long minResponseTime, Long maxResponseTime);

    /**
     * 사용자별 사용 로그 수 조회
     */
    long countByUserIdAndDeletedFalse(String userId);

    /**
     * 특정 기간 내 사용 로그 수 조회
     */
    long countByRequestTimeBetweenAndDeletedFalse(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 커스텀 API별 총 사용 횟수 조회
     */
    long countByCustomApiNameAndDeletedFalse(String customApiName);

    /**
     * 평균 응답 시간 조회 (성능 분석용)
     */
    @Query("SELECT AVG(c.totalResponseTime) FROM CustomApiUsageLog c " +
           "WHERE c.customApiName = :customApiName AND c.deleted = false")
    Double getAverageResponseTimeByCustomApiName(@Param("customApiName") String customApiName);

    /**
     * 복합 검색: 사용자 + 기간 + 커스텀 API 이름
     */
    @Query("SELECT c FROM CustomApiUsageLog c WHERE c.userId = :userId " +
           "AND c.requestTime BETWEEN :startDate AND :endDate " +
           "AND c.customApiName = :customApiName AND c.deleted = false " +
           "ORDER BY c.requestTime DESC")
    List<CustomApiUsageLog> findByUserIdAndDateRangeAndCustomApiName(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("customApiName") String customApiName);

    /**
     * 사용자별 로그 조회 (삭제되지 않은 것만)
     */
    List<CustomApiUsageLog> findByUserIdAndDeletedFalse(String userId);

    /**
     * 날짜 범위별 로그 조회 (삭제되지 않은 것만)
     */
    List<CustomApiUsageLog> findByRequestTimeBetweenAndDeletedFalse(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 커스텀 API 이름별 로그 조회 (삭제되지 않은 것만)
     */
    List<CustomApiUsageLog> findByCustomApiNameAndDeletedFalse(String customApiName);

    /**
     * 응답 시간 범위별 로그 조회 (삭제되지 않은 것만)
     */
    List<CustomApiUsageLog> findByTotalResponseTimeBetweenAndDeletedFalse(Long minTime, Long maxTime);

    /**
     * 전체 삭제되지 않은 로그 개수
     */
    long countByDeletedFalse();

    /**
     * 사용자별 최근 로그 조회 (제한된 개수)
     */
    List<CustomApiUsageLog> findTopByUserIdAndDeletedFalseOrderByRequestTimeDesc(String userId, Pageable pageable);

    /**
     * 전체 최근 로그 조회 (제한된 개수)
     */
    List<CustomApiUsageLog> findTopByDeletedFalseOrderByRequestTimeDesc(Pageable pageable);

    /**
     * 사용자별 평균 응답 시간 조회
     */
    @Query("SELECT AVG(c.totalResponseTime) FROM CustomApiUsageLog c " +
           "WHERE c.userId = :userId AND c.deleted = false")
    Double findAverageResponseTimeByUserIdAndDeletedFalse(@Param("userId") String userId);

    /**
     * 전체 평균 응답 시간 조회
     */
    @Query("SELECT AVG(c.totalResponseTime) FROM CustomApiUsageLog c " +
           "WHERE c.deleted = false")
    Double findAverageResponseTimeByDeletedFalse();
}
