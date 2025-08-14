package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.Entity.PublicApiTokenRefreshLog;
import org.example.APIManagementSvc.domain.enums.RefreshStatus;
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
 * Public API Token Refresh 로그 Repository
 */
@Repository
public interface PublicApiTokenRefreshLogRepository extends JpaRepository<PublicApiTokenRefreshLog, Long> {

    // 기본 조회 메서드들
    Optional<PublicApiTokenRefreshLog> findByLogIdAndDeletedFalse(Long logId);
    
    List<PublicApiTokenRefreshLog> findByProviderNameAndDeletedFalse(String providerName);
    
    List<PublicApiTokenRefreshLog> findByRefreshStatusAndDeletedFalse(RefreshStatus refreshStatus);
    
    List<PublicApiTokenRefreshLog> findByRefreshTimeBetweenAndDeletedFalse(LocalDateTime startTime, LocalDateTime endTime);
    
    List<PublicApiTokenRefreshLog> findByProviderNameAndRefreshStatusAndDeletedFalse(String providerName, RefreshStatus refreshStatus);
    
    // 페이징 조회
    Page<PublicApiTokenRefreshLog> findByDeletedFalse(Pageable pageable);
    
    Page<PublicApiTokenRefreshLog> findByProviderNameAndDeletedFalse(String providerName, Pageable pageable);
    
    Page<PublicApiTokenRefreshLog> findByRefreshStatusAndDeletedFalse(RefreshStatus refreshStatus, Pageable pageable);
    
    // 최근 로그 조회
    List<PublicApiTokenRefreshLog> findTopByProviderNameAndDeletedFalseOrderByRefreshTimeDesc(String providerName, Pageable pageable);
    
    List<PublicApiTokenRefreshLog> findTopByRefreshStatusAndDeletedFalseOrderByRefreshTimeDesc(RefreshStatus refreshStatus, Pageable pageable);
    
    List<PublicApiTokenRefreshLog> findTopByDeletedFalseOrderByRefreshTimeDesc(Pageable pageable);
    
    // 통계 조회
    long countByDeletedFalse();
    
    long countByProviderNameAndDeletedFalse(String providerName);
    
    long countByRefreshStatusAndDeletedFalse(RefreshStatus refreshStatus);
    
    long countByProviderNameAndRefreshStatusAndDeletedFalse(String providerName, RefreshStatus refreshStatus);
    
    // 마지막 갱신 시간 조회
    @Query("SELECT MAX(l.refreshTime) FROM PublicApiTokenRefreshLog l WHERE l.providerName = :providerName AND l.deleted = false")
    Optional<LocalDateTime> findLastRefreshTimeByProviderNameAndDeletedFalse(@Param("providerName") String providerName);
    
    @Query("SELECT MAX(l.refreshTime) FROM PublicApiTokenRefreshLog l WHERE l.refreshStatus = :refreshStatus AND l.deleted = false")
    Optional<LocalDateTime> findLastRefreshTimeByRefreshStatusAndDeletedFalse(@Param("refreshStatus") RefreshStatus refreshStatus);
    
    // Refresh 상태별 통계
    @Query("SELECT l.refreshStatus, COUNT(l) FROM PublicApiTokenRefreshLog l WHERE l.deleted = false GROUP BY l.refreshStatus")
    List<Object[]> findRefreshStatusStatistics();
    
    @Query("SELECT l.refreshStatus, COUNT(l) FROM PublicApiTokenRefreshLog l WHERE l.providerName = :providerName AND l.deleted = false GROUP BY l.refreshStatus")
    List<Object[]> findRefreshStatusStatisticsByProviderName(@Param("providerName") String providerName);
    
    // 특정 시간대 특정 상태 로그
    @Query("SELECT l FROM PublicApiTokenRefreshLog l WHERE l.refreshStatus = :status AND l.refreshTime BETWEEN :startTime AND :endTime AND l.deleted = false")
    List<PublicApiTokenRefreshLog> findByRefreshStatusAndTimeRangeAndDeletedFalse(
            @Param("status") RefreshStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
