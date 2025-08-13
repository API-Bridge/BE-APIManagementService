package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.ExternalApi;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ExternalApi 엔티티를 위한 Repository
 * API 메타데이터 관리 기능을 제공합니다.
 */
@Repository
public interface ExternalApiRepository extends JpaRepository<ExternalApi, String> {

    // === 기본 조회 메서드 ===

    /**
     * API ID로 조회
     */
    Optional<ExternalApi> findByApiId(String apiId);

    /**
     * API 이름으로 조회
     */
    Optional<ExternalApi> findByApiName(String apiName);

    /**
     * API URL로 조회
     */
    Optional<ExternalApi> findByApiUrl(String apiUrl);

    /**
     * 소유자로 조회
     */
    List<ExternalApi> findByApiOwner(String apiOwner);

    // === 도메인/키워드 기반 조회 ===

    /**
     * 도메인으로 조회
     */
    List<ExternalApi> findByApiDomain(ApiDomain apiDomain);

    /**
     * 키워드로 조회
     */
    List<ExternalApi> findByApiKeyword(ApiKeyword apiKeyword);

    /**
     * 도메인과 키워드로 조회
     */
    List<ExternalApi> findByApiDomainAndApiKeyword(ApiDomain apiDomain, ApiKeyword apiKeyword);

    /**
     * 도메인으로 페이징 조회
     */
    Page<ExternalApi> findByApiDomain(ApiDomain apiDomain, Pageable pageable);

    // === HTTP 메소드 기반 조회 ===



    // === 유효성 기반 조회 ===

    /**
     * 유효한 API만 조회 (effectiveness = true, deleted = false)
     */
    @Query("SELECT e FROM ExternalApi e WHERE e.apiEffectiveness = true AND e.deleted = false")
    List<ExternalApi> findValidApis();

    /**
     * 유효한 API 페이징 조회
     */
    @Query("SELECT e FROM ExternalApi e WHERE e.apiEffectiveness = true AND e.deleted = false")
    Page<ExternalApi> findValidApis(Pageable pageable);

    /**
     * 도메인별 유효한 API 조회
     */
    @Query("SELECT e FROM ExternalApi e WHERE e.apiDomain = :domain AND e.apiEffectiveness = true AND e.deleted = false")
    List<ExternalApi> findValidApisByDomain(@Param("domain") ApiDomain domain);

    /**
     * 키워드별 유효한 API 조회
     */
    @Query("SELECT e FROM ExternalApi e WHERE e.apiKeyword = :keyword AND e.apiEffectiveness = true AND e.deleted = false")
    List<ExternalApi> findValidApisByKeyword(@Param("keyword") ApiKeyword keyword);

    // === 검색 기능 ===

    /**
     * API 이름에 검색어가 포함된 API 조회
     */
    @Query("SELECT e FROM ExternalApi e WHERE e.apiName LIKE %:searchTerm% AND e.deleted = false")
    List<ExternalApi> findByApiNameContaining(@Param("searchTerm") String searchTerm);

    /**
     * API 설명에 검색어가 포함된 API 조회
     */
    @Query("SELECT e FROM ExternalApi e WHERE e.apiDescription LIKE %:searchTerm% AND e.deleted = false")
    List<ExternalApi> findByApiDescriptionContaining(@Param("searchTerm") String searchTerm);

    /**
     * 발급처에 검색어가 포함된 API 조회
     */
    @Query("SELECT e FROM ExternalApi e WHERE e.apiIssuer LIKE %:searchTerm% AND e.deleted = false")
    List<ExternalApi> findByApiIssuerContaining(@Param("searchTerm") String searchTerm);

    /**
     * 통합 검색 (이름, 설명, 발급처)
     */
    @Query("SELECT e FROM ExternalApi e WHERE " +
           "(e.apiName LIKE %:searchTerm% OR e.apiDescription LIKE %:searchTerm% OR e.apiIssuer LIKE %:searchTerm%) " +
           "AND e.deleted = false")
    List<ExternalApi> searchApis(@Param("searchTerm") String searchTerm);

    // === 통계 조회 ===

    /**
     * 도메인별 API 개수 조회
     */
    @Query("SELECT e.apiDomain, COUNT(e) FROM ExternalApi e WHERE e.deleted = false GROUP BY e.apiDomain")
    List<Object[]> countByDomain();

    /**
     * 키워드별 API 개수 조회
     */
    @Query("SELECT e.apiKeyword, COUNT(e) FROM ExternalApi e WHERE e.deleted = false GROUP BY e.apiKeyword")
    List<Object[]> countByKeyword();

    /**
     * HTTP 메소드별 API 개수 조회
     */
    @Query("SELECT e.httpMethod, COUNT(e) FROM ExternalApi e WHERE e.deleted = false GROUP BY e.httpMethod")
    List<Object[]> countByHttpMethod();

    // === 최근 생성된 API 조회 ===

    /**
     * 최근 생성된 API 조회 (최신순)
     */
    @Query("SELECT e FROM ExternalApi e WHERE e.deleted = false ORDER BY e.createdAt DESC")
    List<ExternalApi> findRecentApis(Pageable pageable);



    // === 존재 여부 확인 ===

    /**
     * API ID 존재 여부 확인
     */
    boolean existsByApiId(String apiId);

    /**
     * API 이름 존재 여부 확인
     */
    boolean existsByApiName(String apiName);



    // === 삭제된 API 조회 ===



    // === Service에서 필요한 메서드들 ===

    /**
     * 삭제되지 않은 API 조회
     */
    List<ExternalApi> findByDeletedFalse();

    /**
     * 도메인별 삭제되지 않은 API 조회
     */
    List<ExternalApi> findByApiDomainAndDeletedFalse(ApiDomain apiDomain);

    /**
     * 키워드별 삭제되지 않은 API 조회
     */
    List<ExternalApi> findByApiKeywordAndDeletedFalse(ApiKeyword apiKeyword);

    /**
     * 소유자별 삭제되지 않은 API 조회
     */
    List<ExternalApi> findByApiOwnerAndDeletedFalse(String apiOwner);

    /**
     * 검색어로 삭제되지 않은 API 조회
     */
    @Query("SELECT e FROM ExternalApi e WHERE " +
           "(e.apiName LIKE %:searchTerm% OR e.apiDescription LIKE %:searchTerm% OR e.apiIssuer LIKE %:searchTerm%) " +
           "AND e.deleted = false")
    List<ExternalApi> findBySearchTermAndDeletedFalse(@Param("searchTerm") String searchTerm);

    /**
     * 삭제되지 않은 API 개수 조회
     */
    long countByDeletedFalse();

    /**
     * 유효하고 삭제되지 않은 API 개수 조회
     */
    long countByApiEffectivenessTrueAndDeletedFalse();
}
