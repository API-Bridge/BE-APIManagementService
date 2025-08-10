package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.ApiParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ApiParameter 엔티티를 위한 Repository
 * API 파라미터 정보 관리 기능을 제공합니다.
 */
@Repository
public interface ApiParameterRepository extends JpaRepository<ApiParameter, String> {

    // === 기본 조회 메서드 ===

    /**
     * 파라미터 ID로 조회
     */
    ApiParameter findByParameterId(String parameterId);

    /**
     * API ID로 파라미터 목록 조회
     */
    List<ApiParameter> findByApiId(String apiId);

    /**
     * 파라미터 이름으로 조회
     */
    List<ApiParameter> findByParamName(String paramName);

    /**
     * 파라미터 타입으로 조회
     */
    List<ApiParameter> findByParamType(String paramType);

    // === 복합 조건 조회 ===

    /**
     * API ID와 파라미터 이름으로 조회
     */
    ApiParameter findByApiIdAndParamName(String apiId, String paramName);

    /**
     * API ID와 파라미터 타입으로 조회
     */
    List<ApiParameter> findByApiIdAndParamType(String apiId, String paramType);

    /**
     * API ID와 필수 여부로 조회
     */
    List<ApiParameter> findByApiIdAndIsRequired(String apiId, Boolean isRequired);

    /**
     * 파라미터 이름과 타입으로 조회
     */
    List<ApiParameter> findByParamNameAndParamType(String paramName, String paramType);

    // === 필수 파라미터 조회 ===

    /**
     * API의 필수 파라미터 조회
     */
    @Query("SELECT p FROM ApiParameter p WHERE p.apiId = :apiId AND p.isRequired = true AND p.deleted = false")
    List<ApiParameter> findRequiredParametersByApiId(@Param("apiId") String apiId);

    /**
     * API의 선택적 파라미터 조회
     */
    @Query("SELECT p FROM ApiParameter p WHERE p.apiId = :apiId AND p.isRequired = false AND p.deleted = false")
    List<ApiParameter> findOptionalParametersByApiId(@Param("apiId") String apiId);

    // === 기본값이 있는 파라미터 조회 ===

    /**
     * 기본값이 있는 파라미터 조회
     */
    @Query("SELECT p FROM ApiParameter p WHERE p.defaultValue IS NOT NULL AND p.defaultValue != '' AND p.deleted = false")
    List<ApiParameter> findParametersWithDefaultValue();

    /**
     * API의 기본값이 있는 파라미터 조회
     */
    @Query("SELECT p FROM ApiParameter p WHERE p.apiId = :apiId AND p.defaultValue IS NOT NULL AND p.defaultValue != '' AND p.deleted = false")
    List<ApiParameter> findParametersWithDefaultValueByApiId(@Param("apiId") String apiId);

    // === 파라미터 타입별 통계 ===

    /**
     * API별 파라미터 개수 조회
     */
    @Query("SELECT p.apiId, COUNT(p) FROM ApiParameter p WHERE p.deleted = false GROUP BY p.apiId")
    List<Object[]> countParametersByApiId();

    /**
     * 파라미터 타입별 개수 조회
     */
    @Query("SELECT p.paramType, COUNT(p) FROM ApiParameter p WHERE p.deleted = false GROUP BY p.paramType")
    List<Object[]> countByParamType();

    /**
     * API별 필수 파라미터 개수 조회
     */
    @Query("SELECT p.apiId, COUNT(p) FROM ApiParameter p WHERE p.isRequired = true AND p.deleted = false GROUP BY p.apiId")
    List<Object[]> countRequiredParametersByApiId();

    // === 검색 기능 ===

    /**
     * 파라미터 이름에 검색어가 포함된 파라미터 조회
     */
    @Query("SELECT p FROM ApiParameter p WHERE p.paramName LIKE %:searchTerm% AND p.deleted = false")
    List<ApiParameter> findByParamNameContaining(@Param("searchTerm") String searchTerm);

    /**
     * API ID와 파라미터 이름 검색
     */
    @Query("SELECT p FROM ApiParameter p WHERE p.apiId = :apiId AND p.paramName LIKE %:searchTerm% AND p.deleted = false")
    List<ApiParameter> findByApiIdAndParamNameContaining(@Param("apiId") String apiId, @Param("searchTerm") String searchTerm);

    // === 존재 여부 확인 ===

    /**
     * 파라미터 ID 존재 여부 확인
     */
    boolean existsByParameterId(String parameterId);

    /**
     * API ID와 파라미터 이름 조합 존재 여부 확인
     */
    boolean existsByApiIdAndParamName(String apiId, String paramName);

    // === 삭제된 파라미터 조회 ===

    /**
     * 삭제된 파라미터 조회
     */
    @Query("SELECT p FROM ApiParameter p WHERE p.deleted = true")
    List<ApiParameter> findDeletedParameters();

    /**
     * API의 삭제된 파라미터 조회
     */
    @Query("SELECT p FROM ApiParameter p WHERE p.apiId = :apiId AND p.deleted = true")
    List<ApiParameter> findDeletedParametersByApiId(@Param("apiId") String apiId);

    // === 파라미터 검증 ===

    /**
     * API의 파라미터 개수 조회
     */
    @Query("SELECT COUNT(p) FROM ApiParameter p WHERE p.apiId = :apiId AND p.deleted = false")
    long countByApiId(@Param("apiId") String apiId);

    /**
     * API의 필수 파라미터 개수 조회
     */
    @Query("SELECT COUNT(p) FROM ApiParameter p WHERE p.apiId = :apiId AND p.isRequired = true AND p.deleted = false")
    long countRequiredByApiId(@Param("apiId") String apiId);

    // === Service에서 필요한 메서드들 ===

    /**
     * API ID로 삭제되지 않은 파라미터 목록 조회
     */
    List<ApiParameter> findByApiIdAndDeletedFalse(String apiId);

    /**
     * API ID와 필수 여부로 삭제되지 않은 파라미터 조회
     */
    List<ApiParameter> findByApiIdAndIsRequiredTrueAndDeletedFalse(String apiId);

    /**
     * API ID와 파라미터 타입으로 삭제되지 않은 파라미터 조회
     */
    List<ApiParameter> findByApiIdAndParamTypeAndDeletedFalse(String apiId, String paramType);

    /**
     * API ID와 파라미터 이름 검색으로 삭제되지 않은 파라미터 조회
     */
    List<ApiParameter> findByApiIdAndParamNameContainingAndDeletedFalse(String apiId, String paramName);
}
