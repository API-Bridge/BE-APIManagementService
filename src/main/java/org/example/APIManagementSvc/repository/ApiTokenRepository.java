package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ApiToken 엔티티를 위한 Repository
 * API 토큰 관리 및 갱신 스케줄링 기능을 제공합니다.
 */
@Repository
public interface ApiTokenRepository extends JpaRepository<ApiToken, String> {

    // === 기본 조회 메서드 ===

    /**
     * 토큰 ID로 조회
     */
    Optional<ApiToken> findByTokenId(String tokenId);

    /**
     * API ID로 토큰 목록 조회
     */
    List<ApiToken> findByApiId(String apiId);

    /**
     * 토큰 값으로 조회
     */
    Optional<ApiToken> findByTokenValue(String tokenValue);

    /**
     * 토큰 타입으로 조회
     */
    List<ApiToken> findByTokenType(String tokenType);

    /**
     * 발급자로 조회
     */
    List<ApiToken> findByIssuer(String issuer);

    /**
     * 발급자 ID로 조회
     */
    List<ApiToken> findByIssuedBy(String issuedBy);

    // === 토큰 상태 기반 조회 ===

    /**
     * 활성 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.tokenStatus = 'ACTIVE' AND t.deleted = false")
    List<ApiToken> findActiveTokens();

    /**
     * 만료된 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.tokenStatus = 'EXPIRED' AND t.deleted = false")
    List<ApiToken> findExpiredTokens();

    /**
     * 취소된 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.tokenStatus = 'REVOKED' AND t.deleted = false")
    List<ApiToken> findRevokedTokens();

    /**
     * API의 활성 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.apiId = :apiId AND t.tokenStatus = 'ACTIVE' AND t.deleted = false")
    List<ApiToken> findActiveTokensByApiId(@Param("apiId") String apiId);

    // === 갱신 상태 기반 조회 ===

    /**
     * 갱신 대기 중인 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.refreshStatus = 'PENDING' AND t.deleted = false")
    List<ApiToken> findPendingRefreshTokens();

    /**
     * 갱신 성공한 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.refreshStatus = 'SUCCESS' AND t.deleted = false")
    List<ApiToken> findSuccessfullyRefreshedTokens();

    /**
     * 갱신 실패한 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.refreshStatus = 'FAILED' AND t.deleted = false")
    List<ApiToken> findFailedRefreshTokens();

    // === 만료 시간 기반 조회 ===

    /**
     * 곧 만료될 토큰 조회 (1시간 이내)
     */
    @Query("SELECT t FROM ApiToken t WHERE t.expiresAt <= :expiryTime AND t.tokenStatus = 'ACTIVE' AND t.deleted = false")
    List<ApiToken> findTokensExpiringSoon(@Param("expiryTime") LocalDateTime expiryTime);

    /**
     * 만료 시간이 지난 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.expiresAt < :currentTime AND t.tokenStatus = 'ACTIVE' AND t.deleted = false")
    List<ApiToken> findExpiredTokens(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 특정 기간 내에 만료될 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.expiresAt BETWEEN :startTime AND :endTime AND t.tokenStatus = 'ACTIVE' AND t.deleted = false")
    List<ApiToken> findTokensExpiringBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // === 갱신 시간 기반 조회 ===

    /**
     * 갱신이 필요한 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.lastRefreshAt IS NULL OR t.lastRefreshAt <= :refreshTime AND t.tokenStatus = 'ACTIVE' AND t.deleted = false")
    List<ApiToken> findTokensNeedingRefresh(@Param("refreshTime") LocalDateTime refreshTime);

    /**
     * 최근 갱신된 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.lastRefreshAt >= :since AND t.deleted = false ORDER BY t.lastRefreshAt DESC")
    List<ApiToken> findRecentlyRefreshedTokens(@Param("since") LocalDateTime since);

    // === 사용 통계 조회 ===

    /**
     * 사용 횟수별 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.usageCount >= :minUsage AND t.deleted = false ORDER BY t.usageCount DESC")
    List<ApiToken> findTokensByUsageCount(@Param("minUsage") Long minUsage);

    /**
     * API별 토큰 사용 통계
     */
    @Query("SELECT t.apiId, COUNT(t), SUM(t.usageCount) FROM ApiToken t WHERE t.deleted = false GROUP BY t.apiId")
    List<Object[]> getTokenUsageStatsByApi();

    /**
     * 토큰 타입별 통계
     */
    @Query("SELECT t.tokenType, COUNT(t) FROM ApiToken t WHERE t.deleted = false GROUP BY t.tokenType")
    List<Object[]> getTokenStatsByType();

    // === 복합 조건 조회 ===

    /**
     * API ID와 토큰 타입으로 조회
     */
    List<ApiToken> findByApiIdAndTokenType(String apiId, String tokenType);

    /**
     * API ID와 토큰 상태로 조회
     */
    List<ApiToken> findByApiIdAndTokenStatus(String apiId, String tokenStatus);

    /**
     * 발급자와 토큰 타입으로 조회
     */
    List<ApiToken> findByIssuerAndTokenType(String issuer, String tokenType);

    /**
     * 발급자 ID와 토큰 상태로 조회
     */
    List<ApiToken> findByIssuedByAndTokenStatus(String issuedBy, String tokenStatus);

    // === 검색 기능 ===

    /**
     * 발급자에 검색어가 포함된 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.issuer LIKE %:searchTerm% AND t.deleted = false")
    List<ApiToken> findByIssuerContaining(@Param("searchTerm") String searchTerm);

    /**
     * 발급자 ID에 검색어가 포함된 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.issuedBy LIKE %:searchTerm% AND t.deleted = false")
    List<ApiToken> findByIssuedByContaining(@Param("searchTerm") String searchTerm);

    // === 존재 여부 확인 ===

    /**
     * 토큰 ID 존재 여부 확인
     */
    boolean existsByTokenId(String tokenId);

    /**
     * 토큰 값 존재 여부 확인
     */
    boolean existsByTokenValue(String tokenValue);

    /**
     * API ID와 토큰 타입 조합 존재 여부 확인
     */
    boolean existsByApiIdAndTokenType(String apiId, String tokenType);

    // === 삭제된 토큰 조회 ===

    /**
     * 삭제된 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.deleted = true")
    List<ApiToken> findDeletedTokens();

    /**
     * API의 삭제된 토큰 조회
     */
    @Query("SELECT t FROM ApiToken t WHERE t.apiId = :apiId AND t.deleted = true")
    List<ApiToken> findDeletedTokensByApiId(@Param("apiId") String apiId);

    // === 토큰 검증 ===

    /**
     * API의 토큰 개수 조회
     */
    @Query("SELECT COUNT(t) FROM ApiToken t WHERE t.apiId = :apiId AND t.deleted = false")
    long countByApiId(@Param("apiId") String apiId);

    /**
     * 활성 토큰 개수 조회
     */
    @Query("SELECT COUNT(t) FROM ApiToken t WHERE t.tokenStatus = 'ACTIVE' AND t.deleted = false")
    long countActiveTokens();

    /**
     * 만료된 토큰 개수 조회
     */
    @Query("SELECT COUNT(t) FROM ApiToken t WHERE t.tokenStatus = 'EXPIRED' AND t.deleted = false")
    long countExpiredTokens();
}
