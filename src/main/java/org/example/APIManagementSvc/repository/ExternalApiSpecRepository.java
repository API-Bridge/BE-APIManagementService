package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.Entity.ExternalApiSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 외부 API 명세 데이터 접근 레포지토리
 * 
 * ExternalApiSpec 엔티티에 대한 데이터베이스 접근을 담당
 * Spring Data JPA를 활용한 기본 CRUD 작업과 커스텀 조회 메서드 제공
 * 
 * 주요 기능:
 * - 기본 CRUD 작업 (JpaRepository 상속)
 * - 관련 엔티티별 필터링 조회
 * - 성능 최적화를 위한 Fetch Join 쿼리
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Repository
public interface ExternalApiSpecRepository extends JpaRepository<ExternalApiSpec, String> {
    
    /**
     * 특정 자격증명으로 등록된 외부 API 명세들 조회
     * @param credentialId 자격증명 ID
     * @return 해당 자격증명을 사용하는 API 명세 목록
     */
    List<ExternalApiSpec> findByCredential_CredentialId(String credentialId);
    
    /**
     * 특정 도메인에 속한 외부 API 명세들 조회
     * @param domainId 도메인 ID
     * @return 해당 도메인의 API 명세 목록
     */
    List<ExternalApiSpec> findByDomain_DomainId(Integer domainId);
    
    /**
     * 특정 키워드에 속한 외부 API 명세들 조회
     * @param keywordId 키워드 ID
     * @return 해당 키워드의 API 명세 목록
     */
    List<ExternalApiSpec> findByKeyword_KeywordId(Integer keywordId);

    /**
     * API ID로 조회 시 모든 관련 엔티티를 함께 가져오기 (성능 최적화)
     * 자격증명, 도메인, 키워드, 파라미터를 모두 한번에 조회하여 지연 로딩 방지
     * 
     * @param apiId API 고유 식별자
     * @return 모든 관련 정보가 포함된 API 명세
     */
    @Query("SELECT a FROM ExternalApiSpec a " +
           "LEFT JOIN FETCH a.credential c " +
           "LEFT JOIN FETCH a.domain d " +
           "LEFT JOIN FETCH a.keyword k " +
           "LEFT JOIN FETCH a.parameters p " +
           "WHERE a.apiId = :apiId")
    Optional<ExternalApiSpec> findByIdWithAllRelations(@Param("apiId") String apiId);
    
    @Query("SELECT a FROM ExternalApiSpec a " +
           "LEFT JOIN FETCH a.credential c " +
           "LEFT JOIN FETCH a.domain d " +
           "LEFT JOIN FETCH a.keyword k " +
           "WHERE a.isActive = true")
    List<ExternalApiSpec> findAllActiveWithRelations();
}