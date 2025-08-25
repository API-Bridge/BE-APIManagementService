package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.Entity.ApiCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * API 자격증명 데이터 접근 레포지토리
 * 
 * 외부 API 호출에 필요한 인증 정보(API Key, Secret Key)를 관리
 * 보안이 중요한 정보이므로 접근과 조회에 주의가 필요
 * 
 * 관리하는 정보:
 * - API Key/Secret Key (암호화 저장 권장)
 * - 발급 기관 정보
 * - 자격증명 상태 (ACTIVE, INACTIVE, EXPIRED)
 * - 담당자 연락처
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Repository
public interface ApiCredentialRepository extends JpaRepository<ApiCredential, String> {
    
    /**
     * 특정 상태의 자격증명 목록 조회
     * 만료되거나 비활성화된 자격증명을 관리할 때 사용
     * 
     * @param status 자격증명 상태 (ACTIVE, INACTIVE, EXPIRED)
     * @return 해당 상태의 자격증명 목록
     */
    List<ApiCredential> findByStatus(ApiCredential.CredentialStatus status);
    
    /**
     * 기관명으로 자격증명 검색 (대소문자 무시)
     * 특정 기관의 자격증명을 찾을 때 사용
     * 
     * @param organizationName 기관명 (부분 검색 지원)
     * @return 기관명이 포함된 자격증명 목록
     */
    List<ApiCredential> findByOrganizationNameContainingIgnoreCase(String organizationName);
}