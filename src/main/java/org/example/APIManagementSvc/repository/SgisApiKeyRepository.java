package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.Entity.SgisApiKey;
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
 * SGIS API 키 Repository
 * SGIS API 키 정보를 데이터베이스에서 관리합니다.
 */
@Repository
public interface SgisApiKeyRepository extends JpaRepository<SgisApiKey, Long> {

    /**
     * 기관명으로 API 키 조회
     */
    Optional<SgisApiKey> findByOrganizationNameAndDeletedFalse(String organizationName);

    /**
     * 기관 코드로 API 키 조회
     */
    Optional<SgisApiKey> findByOrganizationCodeAndDeletedFalse(String organizationCode);

    /**
     * 연락처 이메일로 API 키 조회
     */
    Optional<SgisApiKey> findByContactEmailAndDeletedFalse(String contactEmail);

    /**
     * keyId로 API 키 조회
     */
    Optional<SgisApiKey> findByKeyIdAndDeletedFalse(String keyId);

    /**
     * API 키 값으로 조회
     */
    Optional<SgisApiKey> findByApiKeyAndDeletedFalse(String apiKey);

    /**
     * 상태별 API 키 목록 조회
     */
    List<SgisApiKey> findByStatusAndDeletedFalse(ApiKeyStatus status);

    /**
     * 기관명으로 API 키 목록 조회 (페이징)
     */
    Page<SgisApiKey> findByOrganizationNameContainingAndDeletedFalse(String organizationName, Pageable pageable);

    /**
     * 상태별 API 키 목록 조회 (페이징)
     */
    Page<SgisApiKey> findByStatusAndDeletedFalse(ApiKeyStatus status, Pageable pageable);

    /**
     * 만료 예정 API 키 목록 조회 (7일 이내)
     */
    @Query("SELECT s FROM SgisApiKey s WHERE s.expiresAt BETWEEN :startDate AND :endDate AND s.deleted = false")
    List<SgisApiKey> findExpiringKeys(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 만료된 API 키 목록 조회
     */
    @Query("SELECT s FROM SgisApiKey s WHERE s.expiresAt < :currentDate AND s.deleted = false")
    List<SgisApiKey> findExpiredKeys(@Param("currentDate") LocalDateTime currentDate);

    /**
     * 일일 사용량 제한에 도달한 API 키 목록 조회
     */
    @Query("SELECT s FROM SgisApiKey s WHERE s.currentDailyUsage >= s.dailyLimit AND s.deleted = false")
    List<SgisApiKey> findDailyLimitExceededKeys();

    /**
     * 월간 사용량 제한에 도달한 API 키 목록 조회
     */
    @Query("SELECT s FROM SgisApiKey s WHERE s.currentMonthlyUsage >= s.monthlyLimit AND s.deleted = false")
    List<SgisApiKey> findMonthlyLimitExceededKeys();

    /**
     * 활성 상태인 API 키 개수 조회
     */
    long countByStatusAndDeletedFalse(ApiKeyStatus status);

    /**
     * 삭제되지 않은 모든 API 키 목록 조회 (페이징)
     */
    Page<SgisApiKey> findByDeletedFalse(Pageable pageable);

    /**
     * 기관별 API 키 개수 조회
     */
    @Query("SELECT s.organizationName, COUNT(s) FROM SgisApiKey s WHERE s.deleted = false GROUP BY s.organizationName")
    List<Object[]> countByOrganization();

    /**
     * 상태별 API 키 개수 조회
     */
    @Query("SELECT s.status, COUNT(s) FROM SgisApiKey s WHERE s.deleted = false GROUP BY s.status")
    List<Object[]> countByStatus();

    /**
     * 최근 발급된 API 키 목록 조회
     */
    @Query("SELECT s FROM SgisApiKey s WHERE s.deleted = false ORDER BY s.issuedAt DESC")
    List<SgisApiKey> findRecentKeys(Pageable pageable);

    /**
     * 최근 사용된 API 키 목록 조회
     */
    @Query("SELECT s FROM SgisApiKey s WHERE s.lastUsedAt IS NOT NULL AND s.deleted = false ORDER BY s.lastUsedAt DESC")
    List<SgisApiKey> findRecentlyUsedKeys(Pageable pageable);

    /**
     * 특정 기간 내에 발급된 API 키 목록 조회
     */
    @Query("SELECT s FROM SgisApiKey s WHERE s.issuedAt BETWEEN :startDate AND :endDate AND s.deleted = false")
    List<SgisApiKey> findKeysIssuedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 기간 내에 사용된 API 키 목록 조회
     */
    @Query("SELECT s FROM SgisApiKey s WHERE s.lastUsedAt BETWEEN :startDate AND :endDate AND s.deleted = false")
    List<SgisApiKey> findKeysUsedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
