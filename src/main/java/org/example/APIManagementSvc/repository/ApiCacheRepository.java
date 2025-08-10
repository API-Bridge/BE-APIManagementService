package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.ApiCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ApiCache 엔티티를 위한 Repository
 * API 캐싱 관리 기능을 제공합니다.
 */
@Repository
public interface ApiCacheRepository extends JpaRepository<ApiCache, String> {

    // === 기본 조회 메서드 ===

    /**
     * 캐시 ID로 조회
     */
    Optional<ApiCache> findByCacheId(String cacheId);

    /**
     * API ID로 캐시 목록 조회
     */
    List<ApiCache> findByApiId(String apiId);

    /**
     * 캐시 키로 조회
     */
    Optional<ApiCache> findByCacheKey(String cacheKey);

    /**
     * 캐시 타입으로 조회
     */
    List<ApiCache> findByCacheType(String cacheType);



    // === 캐시 상태 기반 조회 ===

    /**
     * 활성 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.cacheStatus = 'ACTIVE' AND c.deleted = false")
    List<ApiCache> findActiveCaches();

    /**
     * 만료된 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.cacheStatus = 'EXPIRED' AND c.deleted = false")
    List<ApiCache> findExpiredCaches();

    /**
     * 무효화된 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.cacheStatus = 'INVALID' AND c.deleted = false")
    List<ApiCache> findInvalidCaches();

    /**
     * API의 활성 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.apiId = :apiId AND c.cacheStatus = 'ACTIVE' AND c.deleted = false")
    List<ApiCache> findActiveCachesByApiId(@Param("apiId") String apiId);

    // === 만료 시간 기반 조회 ===

    /**
     * 곧 만료될 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.expiresAt <= :expiryTime AND c.cacheStatus = 'ACTIVE' AND c.deleted = false")
    List<ApiCache> findCachesExpiringSoon(@Param("expiryTime") LocalDateTime expiryTime);

    /**
     * 만료 시간이 지난 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.expiresAt < :currentTime AND c.cacheStatus = 'ACTIVE' AND c.deleted = false")
    List<ApiCache> findExpiredCaches(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 특정 기간 내에 만료될 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.expiresAt BETWEEN :startTime AND :endTime AND c.cacheStatus = 'ACTIVE' AND c.deleted = false")
    List<ApiCache> findCachesExpiringBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // === 접근 시간 기반 조회 ===

    /**
     * 최근 접근된 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.lastAccessed >= :since AND c.deleted = false ORDER BY c.lastAccessed DESC")
    List<ApiCache> findRecentlyAccessedCaches(@Param("since") LocalDateTime since);

    /**
     * 오래된 캐시 조회 (마지막 접근이 오래된 순)
     */
    @Query("SELECT c FROM ApiCache c WHERE c.lastAccessed IS NOT NULL AND c.deleted = false ORDER BY c.lastAccessed ASC")
    List<ApiCache> findOldCaches(org.springframework.data.domain.Pageable pageable);

    // === 접근 횟수 기반 조회 ===

    /**
     * 높은 접근 횟수를 가진 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.accessCount >= :minAccess AND c.deleted = false ORDER BY c.accessCount DESC")
    List<ApiCache> findHighAccessCaches(@Param("minAccess") Long minAccess);

    /**
     * 낮은 접근 횟수를 가진 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.accessCount <= :maxAccess AND c.deleted = false ORDER BY c.accessCount ASC")
    List<ApiCache> findLowAccessCaches(@Param("maxAccess") Long maxAccess);

    // === 캐시 크기 기반 조회 ===

    /**
     * 큰 크기의 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.cacheSize >= :minSize AND c.deleted = false ORDER BY c.cacheSize DESC")
    List<ApiCache> findLargeCaches(@Param("minSize") Long minSize);

    /**
     * 작은 크기의 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.cacheSize <= :maxSize AND c.deleted = false ORDER BY c.cacheSize ASC")
    List<ApiCache> findSmallCaches(@Param("maxSize") Long maxSize);

    // === 복합 조건 조회 ===

    /**
     * API ID와 캐시 키로 조회
     */
    Optional<ApiCache> findByApiIdAndCacheKey(String apiId, String cacheKey);

    /**
     * API ID와 캐시 타입으로 조회
     */
    List<ApiCache> findByApiIdAndCacheType(String apiId, String cacheType);

    /**
     * API ID와 캐시 상태로 조회
     */
    List<ApiCache> findByApiIdAndCacheStatus(String apiId, String cacheStatus);

    // === 통계 조회 ===

    /**
     * API별 캐시 통계
     */
    @Query("SELECT c.apiId, COUNT(c), AVG(c.accessCount), SUM(c.cacheSize) FROM ApiCache c WHERE c.deleted = false GROUP BY c.apiId")
    List<Object[]> getCacheStatsByApi();

    /**
     * 캐시 타입별 통계
     */
    @Query("SELECT c.cacheType, COUNT(c), AVG(c.accessCount) FROM ApiCache c WHERE c.deleted = false GROUP BY c.cacheType")
    List<Object[]> getCacheStatsByType();



    // === 검색 기능 ===

    /**
     * 캐시 키에 검색어가 포함된 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.cacheKey LIKE %:searchTerm% AND c.deleted = false")
    List<ApiCache> findByCacheKeyContaining(@Param("searchTerm") String searchTerm);

    /**
     * API ID와 캐시 키 검색
     */
    @Query("SELECT c FROM ApiCache c WHERE c.apiId = :apiId AND c.cacheKey LIKE %:searchTerm% AND c.deleted = false")
    List<ApiCache> findByApiIdAndCacheKeyContaining(@Param("apiId") String apiId, @Param("searchTerm") String searchTerm);

    // === 존재 여부 확인 ===

    /**
     * 캐시 ID 존재 여부 확인
     */
    boolean existsByCacheId(String cacheId);

    /**
     * 캐시 키 존재 여부 확인
     */
    boolean existsByCacheKey(String cacheKey);

    /**
     * API ID와 캐시 키 조합 존재 여부 확인
     */
    boolean existsByApiIdAndCacheKey(String apiId, String cacheKey);

    // === 삭제된 캐시 조회 ===

    /**
     * 삭제된 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.deleted = true")
    List<ApiCache> findDeletedCaches();

    /**
     * API의 삭제된 캐시 조회
     */
    @Query("SELECT c FROM ApiCache c WHERE c.apiId = :apiId AND c.deleted = true")
    List<ApiCache> findDeletedCachesByApiId(@Param("apiId") String apiId);

    // === 캐시 검증 ===

    /**
     * API의 캐시 개수 조회
     */
    @Query("SELECT COUNT(c) FROM ApiCache c WHERE c.apiId = :apiId AND c.deleted = false")
    long countByApiId(@Param("apiId") String apiId);

    /**
     * 활성 캐시 개수 조회
     */
    @Query("SELECT COUNT(c) FROM ApiCache c WHERE c.cacheStatus = 'ACTIVE' AND c.deleted = false")
    long countActiveCaches();

    /**
     * 만료된 캐시 개수 조회
     */
    @Query("SELECT COUNT(c) FROM ApiCache c WHERE c.cacheStatus = 'EXPIRED' AND c.deleted = false")
    long countExpiredCaches();

    /**
     * 총 캐시 크기 조회
     */
    @Query("SELECT SUM(c.cacheSize) FROM ApiCache c WHERE c.cacheStatus = 'ACTIVE' AND c.deleted = false")
    Long getTotalCacheSize();
}
