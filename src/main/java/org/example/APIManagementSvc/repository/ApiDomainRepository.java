package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.Entity.ApiDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * API 도메인 데이터 접근 레포지토리
 * 
 * API를 대분류로 구분하는 도메인 정보를 관리
 * 예: 날씨(Weather), 교통(Transportation), 금융(Finance), 지도(Map) 등
 * 
 * 용도:
 * - API 카테고리 관리
 * - API 검색 및 필터링
 * - 통계 및 분석용 그룹핑
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Repository
public interface ApiDomainRepository extends JpaRepository<ApiDomain, Integer> {
    
    /**
     * 도메인명으로 도메인 정보 조회
     * 중복 검사나 특정 도메인 찾기에 사용
     * 
     * @param domainName 도메인명 (예: "Weather", "Transportation")
     * @return 해당 도메인 정보 (존재하지 않으면 Optional.empty())
     */
    Optional<ApiDomain> findByDomainName(String domainName);
    
    /**
     * 도메인명 중복 여부 확인
     * 새로운 도메인 등록 시 중복 검사에 사용
     * 
     * @param domainName 확인할 도메인명
     * @return 존재하면 true, 존재하지 않으면 false
     */
    boolean existsByDomainName(String domainName);
}