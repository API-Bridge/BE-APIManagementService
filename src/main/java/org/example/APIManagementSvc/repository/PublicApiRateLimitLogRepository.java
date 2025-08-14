package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.Entity.PublicApiRateLimitLog;
import org.example.APIManagementSvc.domain.enums.RateLimitStatus;
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
 * Public API Rate Limit 로그 Repository
 */
@Repository
public interface PublicApiRateLimitLogRepository extends JpaRepository<PublicApiRateLimitLog, Long> {

    // 기본 조회 메서드들
    Optional<PublicApiRateLimitLog> findByLogIdAndDeletedFalse(Long logId);
    
    List<PublicApiRateLimitLog> findByApiNameAndDeletedFalse(String apiName);
    
    List<PublicApiRateLimitLog> findByUserIdAndDeletedFalse(String userId);
    
    List<PublicApiRateLimitLog> findByIpAddressAndDeletedFalse(String ipAddress);
    
    List<PublicApiRateLimitLog> findByRateLimitStatusAndDeletedFalse(RateLimitStatus rateLimitStatus);
    
    List<PublicApiRateLimitLog> findByRequestTimeBetweenAndDeletedFalse(LocalDateTime startTime, LocalDateTime endTime);
    
    List<PublicApiRateLimitLog> findByApiNameAndRateLimitStatusAndDeletedFalse(String apiName, RateLimitStatus rateLimitStatus);
    
    List<PublicApiRateLimitLog> findByUserIdAndRateLimitStatusAndDeletedFalse(String userId, RateLimitStatus rateLimitStatus);
    
    List<PublicApiRateLimitLog> findByApiNameAndUserIdAndDeletedFalse(String apiName, String userId);
    
    // 페이징 조회
    Page<PublicApiRateLimitLog> findByDeletedFalse(Pageable pageable);
    
    Page<PublicApiRateLimitLog> findByApiNameAndDeletedFalse(String apiName, Pageable pageable);
    
    Page<PublicApiRateLimitLog> findByUserIdAndDeletedFalse(String userId, Pageable pageable);
    
    Page<PublicApiRateLimitLog> findByRateLimitStatusAndDeletedFalse(RateLimitStatus rateLimitStatus, Pageable pageable);
    
    // 최근 로그 조회
    List<PublicApiRateLimitLog> findTopByApiNameAndDeletedFalseOrderByRequestTimeDesc(String apiName, Pageable pageable);
    
    List<PublicApiRateLimitLog> findTopByUserIdAndDeletedFalseOrderByRequestTimeDesc(String userId, Pageable pageable);
    
    List<PublicApiRateLimitLog> findTopByDeletedFalseOrderByRequestTimeDesc(Pageable pageable);
    
    // 통계 조회
    long countByDeletedFalse();
    
    long countByApiNameAndDeletedFalse(String apiName);
    
    long countByUserIdAndDeletedFalse(String userId);
    
    long countByRateLimitStatusAndDeletedFalse(RateLimitStatus rateLimitStatus);
    
    long countByApiNameAndRateLimitStatusAndDeletedFalse(String apiName, RateLimitStatus rateLimitStatus);
    
    long countByUserIdAndRateLimitStatusAndDeletedFalse(String userId, RateLimitStatus rateLimitStatus);
    
    // 마지막 요청 시간 조회
    @Query("SELECT MAX(l.requestTime) FROM PublicApiRateLimitLog l WHERE l.apiName = :apiName AND l.deleted = false")
    Optional<LocalDateTime> findLastRequestTimeByApiNameAndDeletedFalse(@Param("apiName") String apiName);
    
    @Query("SELECT MAX(l.requestTime) FROM PublicApiRateLimitLog l WHERE l.userId = :userId AND l.deleted = false")
    Optional<LocalDateTime> findLastRequestTimeByUserIdAndDeletedFalse(@Param("userId") String userId);
    
    @Query("SELECT MAX(l.requestTime) FROM PublicApiRateLimitLog l WHERE l.ipAddress = :ipAddress AND l.deleted = false")
    Optional<LocalDateTime> findLastRequestTimeByIpAddressAndDeletedFalse(@Param("ipAddress") String ipAddress);
    
    // Rate Limit 상태별 통계
    @Query("SELECT l.rateLimitStatus, COUNT(l) FROM PublicApiRateLimitLog l WHERE l.deleted = false GROUP BY l.rateLimitStatus")
    List<Object[]> findRateLimitStatusStatistics();
    
    @Query("SELECT l.rateLimitStatus, COUNT(l) FROM PublicApiRateLimitLog l WHERE l.apiName = :apiName AND l.deleted = false GROUP BY l.rateLimitStatus")
    List<Object[]> findRateLimitStatusStatisticsByApiName(@Param("apiName") String apiName);
    
    @Query("SELECT l.rateLimitStatus, COUNT(l) FROM PublicApiRateLimitLog l WHERE l.userId = :userId AND l.deleted = false GROUP BY l.rateLimitStatus")
    List<Object[]> findRateLimitStatusStatisticsByUserId(@Param("userId") String userId);
    
    // 특정 시간대 Rate Limit 초과 로그
    @Query("SELECT l FROM PublicApiRateLimitLog l WHERE l.rateLimitStatus = :status AND l.requestTime BETWEEN :startTime AND :endTime AND l.deleted = false")
    List<PublicApiRateLimitLog> findByRateLimitStatusAndTimeRangeAndDeletedFalse(
            @Param("status") RateLimitStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    // 사용량 비율이 높은 로그 조회
    @Query("SELECT l FROM PublicApiRateLimitLog l WHERE l.currentUsageCount IS NOT NULL AND l.maxAllowedCount IS NOT NULL " +
           "AND (CAST(l.currentUsageCount AS double) / CAST(l.maxAllowedCount AS double)) >= :threshold " +
           "AND l.deleted = false ORDER BY (CAST(l.currentUsageCount AS double) / CAST(l.maxAllowedCount AS double)) DESC")
    List<PublicApiRateLimitLog> findByHighUsageRatioAndDeletedFalse(@Param("threshold") double threshold);
}
