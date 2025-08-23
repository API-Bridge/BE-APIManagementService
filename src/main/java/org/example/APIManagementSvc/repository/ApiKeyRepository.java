package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.Entity.ApiKey;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;
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
 * ApiKey 엔티티를 위한 Repository
 * API 키 관리 기능을 제공합니다.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {

    // === 기본 조회 메서드 ===

    /**
     * 키 ID로 조회
     */
    Optional<ApiKey> findByKeyIdAndDeletedFalse(String keyId);

    /**
     * 조직명으로 API 키 조회 (삭제되지 않은 것만)
     */
    List<ApiKey> findByOrganizationNameAndDeletedFalse(String organizationName);

    /**
     * API 키로 API 키 조회 (삭제되지 않은 것만)
     */
    Optional<ApiKey> findByApiKeyAndDeletedFalse(String apiKey);

    /**
     * 연락처 이메일로 조회
     */
    List<ApiKey> findByContactEmailAndDeletedFalse(String contactEmail);

    // === 상태 기반 조회 ===

    /**
     * 상태로 조회
     */
    List<ApiKey> findByStatusAndDeletedFalse(ApiKeyStatus status);

    /**
     * 활성 상태인 API 키 조회
     */
    List<ApiKey> findByStatusAndDeletedFalseAndExpiresAtAfter(ApiKeyStatus status, LocalDateTime now);

    /**
     * 만료된 API 키 조회
     */
    @Query("SELECT a FROM ApiKey a WHERE a.expiresAt <= :now AND a.deleted = false")
    List<ApiKey> findExpiredApiKeys(@Param("now") LocalDateTime now);

    /**
     * 곧 만료될 API 키 조회 (7일 이내)
     */
    @Query("SELECT a FROM ApiKey a WHERE a.expiresAt BETWEEN :now AND :expiryDate AND a.deleted = false")
    List<ApiKey> findExpiringSoonApiKeys(@Param("now") LocalDateTime now, @Param("expiryDate") LocalDateTime expiryDate);

    // === 사용량 기반 조회 ===

    /**
     * 일일 사용량 제한에 도달한 API 키 조회
     */
    @Query("SELECT a FROM ApiKey a WHERE a.dailyLimit IS NOT NULL AND a.currentDailyUsage >= a.dailyLimit AND a.deleted = false")
    List<ApiKey> findDailyLimitExceededApiKeys();

    /**
     * 월간 사용량 제한에 도달한 API 키 조회
     */
    @Query("SELECT a FROM ApiKey a WHERE a.monthlyLimit IS NOT NULL AND a.currentMonthlyUsage >= a.monthlyLimit AND a.deleted = false")
    List<ApiKey> findMonthlyLimitExceededApiKeys();

    /**
     * 사용량이 높은 API 키 조회 (80% 이상 사용)
     */
    @Query("SELECT a FROM ApiKey a WHERE " +
           "(a.dailyLimit IS NOT NULL AND a.currentDailyUsage >= a.dailyLimit * 0.8) OR " +
           "(a.monthlyLimit IS NOT NULL AND a.currentMonthlyUsage >= a.monthlyLimit * 0.8) " +
           "AND a.deleted = false")
    List<ApiKey> findHighUsageApiKeys();

    // === 시간 기반 조회 ===

    /**
     * 발급일 기준으로 조회
     */
    @Query("SELECT a FROM ApiKey a WHERE a.issuedAt BETWEEN :startDate AND :endDate AND a.deleted = false")
    List<ApiKey> findByIssuedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 마지막 사용일 기준으로 조회
     */
    @Query("SELECT a FROM ApiKey a WHERE a.lastUsedAt BETWEEN :startDate AND :endDate AND a.deleted = false")
    List<ApiKey> findByLastUsedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 오랫동안 사용되지 않은 API 키 조회 (30일 이상)
     */
    @Query("SELECT a FROM ApiKey a WHERE a.lastUsedAt <= :inactiveDate AND a.deleted = false")
    List<ApiKey> findInactiveApiKeys(@Param("inactiveDate") LocalDateTime inactiveDate);

    // === 페이징 조회 ===

    /**
     * 페이징으로 조회
     */
    Page<ApiKey> findByDeletedFalse(Pageable pageable);

    /**
     * 상태별 페이징 조회
     */
    Page<ApiKey> findByStatusAndDeletedFalse(ApiKeyStatus status, Pageable pageable);

    /**
     * 기관명별 페이징 조회
     */
    Page<ApiKey> findByOrganizationNameAndDeletedFalse(String organizationName, Pageable pageable);

    /**
     * API 서비스명별 페이징 조회
     */
    Page<ApiKey> findByApiServiceNameAndDeletedFalse(String apiServiceName, Pageable pageable);

    // === 통계 조회 ===

    /**
     * 상태별 API 키 개수 통계
     */
    @Query("SELECT a.status, COUNT(a) FROM ApiKey a WHERE a.deleted = false GROUP BY a.status")
    List<Object[]> getApiKeyStatusStatistics();

    /**
     * API 서비스별 API 키 개수 통계
     */
    @Query("SELECT a.apiServiceName, COUNT(a) FROM ApiKey a WHERE a.deleted = false GROUP BY a.apiServiceName")
    List<Object[]> getApiKeyServiceStatistics();

    /**
     * 기관별 API 키 개수 통계
     */
    @Query("SELECT a.organizationName, COUNT(a) FROM ApiKey a WHERE a.deleted = false GROUP BY a.organizationName")
    List<Object[]> getApiKeyOrganizationStatistics();

    // === 검색 기능 ===

    /**
     * 기관명에 검색어가 포함된 API 키 조회
     */
    @Query("SELECT a FROM ApiKey a WHERE a.organizationName LIKE %:searchTerm% AND a.deleted = false")
    List<ApiKey> findByOrganizationNameContaining(@Param("searchTerm") String searchTerm);

    /**
     * 설명에 검색어가 포함된 API 키 조회
     */
    @Query("SELECT a FROM ApiKey a WHERE a.description LIKE %:searchTerm% AND a.deleted = false")
    List<ApiKey> findByDescriptionContaining(@Param("searchTerm") String searchTerm);

    // === 존재 여부 확인 ===

    /**
     * 키 ID 존재 여부 확인
     */
    boolean existsByKeyIdAndDeletedFalse(String keyId);

    /**
     * API 키 값 존재 여부 확인
     */
    boolean existsByApiKeyAndDeletedFalse(String apiKey);

    /**
     * 기관명과 API 서비스명으로 존재 여부 확인
     */
    boolean existsByOrganizationNameAndApiServiceNameAndDeletedFalse(String organizationName, String apiServiceName);

    // === 삭제된 API 키 조회 ===

    /**
     * 삭제된 API 키 조회
     */
    @Query("SELECT a FROM ApiKey a WHERE a.deleted = true")
    List<ApiKey> findDeletedApiKeys();

    /**
     * 키 ID로 삭제된 API 키 조회
     */
    @Query("SELECT a FROM ApiKey a WHERE a.keyId = :keyId AND a.deleted = true")
    Optional<ApiKey> findDeletedApiKeyByKeyId(@Param("keyId") String keyId);

    // === 개수 조회 ===

    /**
     * 전체 API 키 개수
     */
    @Query("SELECT COUNT(a) FROM ApiKey a WHERE a.deleted = false")
    long countAllApiKeys();

    /**
     * 상태별 API 키 개수
     */
    @Query("SELECT COUNT(a) FROM ApiKey a WHERE a.status = :status AND a.deleted = false")
    long countByStatusAndDeletedFalse(@Param("status") ApiKeyStatus status);

    /**
     * API 서비스별 API 키 개수
     */
    @Query("SELECT COUNT(a) FROM ApiKey a WHERE a.apiServiceName = :apiServiceName AND a.deleted = false")
    long countByApiServiceNameAndDeletedFalse(@Param("apiServiceName") String apiServiceName);
}
