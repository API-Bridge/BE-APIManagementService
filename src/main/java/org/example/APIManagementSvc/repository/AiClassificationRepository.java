package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.Entity.AiClassification;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AiClassification 엔티티를 위한 Repository
 * AI 분류 결과 관리 기능을 제공합니다.
 */
@Repository
public interface AiClassificationRepository extends JpaRepository<AiClassification, String> {

    // === 기본 조회 메서드 ===

    /**
     * 분류 ID로 조회
     */
    Optional<AiClassification> findByClassificationId(String classificationId);

    /**
     * API ID로 분류 결과 조회
     */
    List<AiClassification> findByApiId(String apiId);

    /**
     * 모델 버전으로 조회
     */
    List<AiClassification> findByModelVersion(String modelVersion);

    // === 분류 결과 기반 조회 ===

    /**
     * 분류된 도메인으로 조회
     */
    List<AiClassification> findByClassifiedDomain(ApiDomain classifiedDomain);

    /**
     * 분류된 키워드로 조회
     */
    List<AiClassification> findByClassifiedKeyword(ApiKeyword classifiedKeyword);

    /**
     * 도메인과 키워드로 조회
     */
    List<AiClassification> findByClassifiedDomainAndClassifiedKeyword(ApiDomain domain, ApiKeyword keyword);

    // === 시간 기반 조회 ===

    /**
     * 최근 분류된 결과 조회
     */
    @Query("SELECT a FROM AiClassification a WHERE a.classifiedAt >= :since AND a.deleted = false ORDER BY a.classifiedAt DESC")
    List<AiClassification> findRecentlyClassified(@Param("since") LocalDateTime since);

    /**
     * 특정 기간 내 분류된 결과 조회
     */
    @Query("SELECT a FROM AiClassification a WHERE a.classifiedAt BETWEEN :startTime AND :endTime AND a.deleted = false ORDER BY a.classifiedAt DESC")
    List<AiClassification> findClassificationsBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // === 복합 조건 조회 ===

    /**
     * API ID와 도메인으로 조회
     */
    List<AiClassification> findByApiIdAndClassifiedDomain(String apiId, ApiDomain domain);

    /**
     * API ID와 키워드로 조회
     */
    List<AiClassification> findByApiIdAndClassifiedKeyword(String apiId, ApiKeyword keyword);

    // === 통계 조회 ===

    /**
     * 도메인별 분류 통계
     */
    @Query("SELECT a.classifiedDomain, COUNT(a) FROM AiClassification a WHERE a.deleted = false GROUP BY a.classifiedDomain")
    List<Object[]> getClassificationStatsByDomain();

    /**
     * 키워드별 분류 통계
     */
    @Query("SELECT a.classifiedKeyword, COUNT(a) FROM AiClassification a WHERE a.deleted = false GROUP BY a.classifiedKeyword")
    List<Object[]> getClassificationStatsByKeyword();

    /**
     * 모델 버전별 분류 통계
     */
    @Query("SELECT a.modelVersion, COUNT(a) FROM AiClassification a WHERE a.deleted = false GROUP BY a.modelVersion")
    List<Object[]> getClassificationStatsByModelVersion();

    // === 검색 기능 ===

    /**
     * 분석된 텍스트에 검색어가 포함된 분류 결과 조회
     */
    @Query("SELECT a FROM AiClassification a WHERE a.analyzedText LIKE %:searchTerm% AND a.deleted = false")
    List<AiClassification> findByAnalyzedTextContaining(@Param("searchTerm") String searchTerm);

    /**
     * 분류 로그에 검색어가 포함된 분류 결과 조회
     */
    @Query("SELECT a FROM AiClassification a WHERE a.classificationLog LIKE %:searchTerm% AND a.deleted = false")
    List<AiClassification> findByClassificationLogContaining(@Param("searchTerm") String searchTerm);

    // === 존재 여부 확인 ===

    /**
     * 분류 ID 존재 여부 확인
     */
    boolean existsByClassificationId(String classificationId);

    /**
     * API ID로 분류 결과 존재 여부 확인
     */
    boolean existsByApiId(String apiId);

    // === 삭제된 분류 결과 조회 ===

    /**
     * 삭제된 분류 결과 조회
     */
    @Query("SELECT a FROM AiClassification a WHERE a.deleted = true")
    List<AiClassification> findDeletedClassifications();

    /**
     * API의 삭제된 분류 결과 조회
     */
    @Query("SELECT a FROM AiClassification a WHERE a.apiId = :apiId AND a.deleted = true")
    List<AiClassification> findDeletedClassificationsByApiId(@Param("apiId") String apiId);

    // === 분류 결과 검증 ===

    /**
     * API의 분류 결과 개수 조회
     */
    @Query("SELECT COUNT(a) FROM AiClassification a WHERE a.apiId = :apiId AND a.deleted = false")
    long countByApiId(@Param("apiId") String apiId);

    /**
     * 전체 분류 결과 개수 조회
     */
    @Query("SELECT COUNT(a) FROM AiClassification a WHERE a.deleted = false")
    long countAllClassifications();
}

