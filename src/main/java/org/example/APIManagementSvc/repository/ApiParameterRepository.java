package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.Entity.ApiParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * API 파라미터 데이터 접근 레포지토리
 * 
 * 각 외부 API의 파라미터 정보를 관리하는 레포지토리
 * API마다 다른 개수와 타입의 파라미터를 유연하게 처리
 * 
 * 주요 용도:
 * - API별 파라미터 목록 조회
 * - 파라미터 일괄 삭제 (API 수정 시)
 * - 개별 파라미터 CRUD 작업
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Repository
public interface ApiParameterRepository extends JpaRepository<ApiParameter, String> {
    
    /**
     * 특정 API의 모든 파라미터 조회
     * API 호출 시 필요한 파라미터 구조를 파악할 때 사용
     * 
     * @param apiId 외부 API 고유 식별자
     * @return 해당 API의 파라미터 목록
     */
    List<ApiParameter> findByApiSpec_ApiId(String apiId);

    /**
     * 특정 API의 모든 파라미터 일괄 삭제
     * API 명세 수정 시 기존 파라미터를 모두 삭제하고 새로 등록할 때 사용
     * 
     * @param apiId 외부 API 고유 식별자
     */
    void deleteByApiSpec_ApiId(String apiId);

    /**
     * 특정 자격증명을 사용하는 API들의 특정 이름을 가진 파라미터들 조회
     * SGIS 토큰 갱신 시 accessToken 파라미터들의 default_value를 업데이트할 때 사용
     * 
     * @param credentialId 자격증명 ID (예: "SGIS")
     * @param paramName 파라미터 이름 (예: "accessToken")
     * @return 해당 조건을 만족하는 파라미터 목록
     */
    List<ApiParameter> findByApiSpec_Credential_CredentialIdAndParamName(String credentialId, String paramName);
}