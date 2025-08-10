package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.ApiRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ApiRateLimit 엔티티를 위한 Repository
 * API Rate Limiting 관리 기능을 제공합니다.
 */
@Repository
public interface ApiRateLimitRepository extends JpaRepository<ApiRateLimit, String> {

    // === 기본 조회 메서드 ===

    /**
     * Rate Limit ID로 조회
     */
    Optional<ApiRateLimit> findByRateLimitId(String rateLimitId);

    /**
     * API ID로 Rate Limit 목록 조회
     */
    List<ApiRateLimit> findByApiId(String apiId);

    /**
     * 사용자 ID로 Rate Limit 목록 조회
     */
    List<ApiRateLimit> findByUserId(String userId);

    /**
     * IP 주소로 Rate Limit 목록 조회
     */
    List<ApiRateLimit> findByIpAddress(String ipAddress);

    /**
     * 제한 단위로 조회
     */
    List<ApiRateLimit> findByLimitUnit(String limitUnit);

    // === 상태 기반 조회 ===

    /**
     * 활성 Rate Limit 조회
     */
    @Query("SELECT r FROM ApiRateLimit r WHERE r.status = 'ACTIVE' AND r.deleted = false")
    List<ApiRateLimit> findActiveRateLimits();

    /**
     * 일시정지된 Rate Limit 조회
     */
    @Query("SELECT r FROM ApiRateLimit r WHERE r.status = 'SUSPENDED' AND r.deleted = false")
    List<ApiRateLimit> findSuspendedRateLimits();

    /**
     * API의 활성 Rate Limit 조회
     */
    @Query("SELECT r FROM ApiRateLimit r WHERE r.apiId = :apiId AND r.status = 'ACTIVE' AND r.deleted = false")
    List<ApiRateLimit> findActiveRateLimitsByApiId(@Param("apiId") String apiId);

    /**
     * 사용자의 활성 Rate Limit 조회
     */
    @Query("SELECT r FROM ApiRateLimit r WHERE r.userId = :userId AND r.status = 'ACTIVE' AND r.deleted = false")
    List<ApiRateLimit> findActiveRateLimitsByUserId(@Param("userId") String userId);

    // === 리셋 시간 기반 조회 ===

    /**
     * 곧 리셋될 Rate Limit 조회
     */
    @Query("SELECT r FROM ApiRateLimit r WHERE r.resetAt <= :resetTime AND r.status = 'ACTIVE' AND r.deleted = false")
    List<ApiRateLimit> findRateLimitsResettingSoon(@Param("resetTime") LocalDateTime resetTime);

    /**
     * 리셋 시간이 지난 Rate Limit 조회
     */
    @Query("SELECT r FROM ApiRateLimit r WHERE r.resetAt < :currentTime AND r.status = 'ACTIVE' AND r.deleted = false")
    List<ApiRateLimit> findExpiredRateLimits(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 특정 기간 내에 리셋될 Rate Limit 조회
     */
    @Query("SELECT r FROM ApiRateLimit r WHERE r.resetAt BETWEEN :startTime AND :endTime AND r.status = 'ACTIVE' AND r.deleted = false")
    List<ApiRateLimit> findRateLimitsResettingBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // === 사용량 기반 조회 ===

    /**
     * 제한에 도달한 Rate Limit 조회
     */
    @Query("SELECT r FROM ApiRateLimit r WHERE r.currentCount >= r.limitCount AND r.status = 'ACTIVE' AND r.deleted = false")
    List<ApiRateLimit> findExceededRateLimits();

    /**
     * 높은 사용률을 가진 Rate Limit 조회 (80% 이상)
     */
    @Query("SELECT r FROM ApiRateLimit r WHERE (r.currentCount * 1.0 / r.limitCount) >= 0.8 AND r.status = 'ACTIVE' AND r.deleted = false")
    List<ApiRateLimit> findHighUsageRateLimits();

    /**
     * API의 제한에 도달한 Rate Limit 조회
     */
    @Query("SELECT r FROM ApiRateLimit r WHERE r.apiId = :apiId AND r.currentCount >= r.limitCount AND r.status = 'ACTIVE' AND r.deleted = false")
    List<ApiRateLimit> findExceededRateLimitsByApiId(@Param("apiId") String apiId);

    // === 복합 조건 조회 ===

    /**
     * API ID와 사용자 ID로 조회
     */
    Optional<ApiRateLimit> findByApiIdAndUserId(String apiId, String userId);

    /**
     * API ID와 IP 주소로 조회
     */
    Optional<ApiRateLimit> findByApiIdAndIpAddress(String apiId, String ipAddress);

    /**
     * API ID와 제한 단위로 조회
     */
    List<ApiRateLimit> findByApiIdAndLimitUnit(String apiId, String limitUnit);

    /**
     * 사용자 ID와 제한 단위로 조회
     */
    List<ApiRateLimit> findByUserIdAndLimitUnit(String userId, String limitUnit);

    /**
     * API ID와 상태로 조회
     */
    List<ApiRateLimit> findByApiIdAndStatus(String apiId, String status);

    // === 통계 조회 ===

    /**
     * API별 Rate Limit 통계
     */
    @Query("SELECT r.apiId, COUNT(r), AVG(r.currentCount), MAX(r.currentCount) FROM ApiRateLimit r WHERE r.deleted = false GROUP BY r.apiId")
    List<Object[]> getRateLimitStatsByApi();

    /**
     * 제한 단위별 통계
     */
    @Query("SELECT r.limitUnit, COUNT(r) FROM ApiRateLimit r WHERE r.deleted = false GROUP BY r.limitUnit")
    List<Object[]> getRateLimitStatsByUnit();

    /**
     * 사용자별 Rate Limit 통계
     */
    @Query("SELECT r.userId, COUNT(r), SUM(r.currentCount) FROM ApiRateLimit r WHERE r.deleted = false GROUP BY r.userId")
    List<Object[]> getRateLimitStatsByUser();

    // === 검색 기능 ===

    /**
     * 설명에 검색어가 포함된 Rate Limit 조회
     */
    @Query("SELECT r FROM ApiRateLimit r WHERE r.description LIKE %:searchTerm% AND r.deleted = false")
    List<ApiRateLimit> findByDescriptionContaining(@Param("searchTerm") String searchTerm);

    /**
     * API ID와 설명 검색
     */
    @Query("SELECT r FROM ApiRateLimit r WHERE r.apiId = :apiId AND r.description LIKE %:searchTerm% AND r.deleted = false")
    List<ApiRateLimit> findByApiIdAndDescriptionContaining(@Param("apiId") String apiId, @Param("searchTerm") String searchTerm);

    // === 존재 여부 확인 ===

    /**
     * Rate Limit ID 존재 여부 확인
     */
    boolean existsByRateLimitId(String rateLimitId);

    /**
     * API ID와 사용자 ID 조합 존재 여부 확인
     */
    boolean existsByApiIdAndUserId(String apiId, String userId);

    /**
     * API ID와 IP 주소 조합 존재 여부 확인
     */
    boolean existsByApiIdAndIpAddress(String apiId, String ipAddress);

    // === 삭제된 Rate Limit 조회 ===

    /**
     * 삭제된 Rate Limit 조회
     */
    @Query("SELECT r FROM ApiRateLimit r WHERE r.deleted = true")
    List<ApiRateLimit> findDeletedRateLimits();

    /**
     * API의 삭제된 Rate Limit 조회
     */
    @Query("SELECT r FROM ApiRateLimit r WHERE r.apiId = :apiId AND r.deleted = true")
    List<ApiRateLimit> findDeletedRateLimitsByApiId(@Param("apiId") String apiId);

    // === Rate Limit 검증 ===

    /**
     * API의 Rate Limit 개수 조회
     */
    @Query("SELECT COUNT(r) FROM ApiRateLimit r WHERE r.apiId = :apiId AND r.deleted = false")
    long countByApiId(@Param("apiId") String apiId);

    /**
     * 사용자의 Rate Limit 개수 조회
     */
    @Query("SELECT COUNT(r) FROM ApiRateLimit r WHERE r.userId = :userId AND r.deleted = false")
    long countByUserId(@Param("userId") String userId);

    /**
     * 활성 Rate Limit 개수 조회
     */
    @Query("SELECT COUNT(r) FROM ApiRateLimit r WHERE r.status = 'ACTIVE' AND r.deleted = false")
    long countActiveRateLimits();

    /**
     * 제한에 도달한 Rate Limit 개수 조회
     */
    @Query("SELECT COUNT(r) FROM ApiRateLimit r WHERE r.currentCount >= r.limitCount AND r.status = 'ACTIVE' AND r.deleted = false")
    long countExceededRateLimits();
}
