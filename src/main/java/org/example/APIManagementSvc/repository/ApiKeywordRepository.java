package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.Entity.ApiKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * API 키워드 데이터 접근 레포지토리
 * 
 * API를 세부적으로 분류하는 키워드 정보를 관리
 * 도메인 하위의 더 구체적인 분류를 담당
 * 
 * 분류 체계:
 * - 도메인: Weather (날씨)
 *   - 키워드: current_weather (현재 날씨)
 *   - 키워드: weather_forecast (일기예보)
 *   - 키워드: weather_warning (기상특보)
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Repository
public interface ApiKeywordRepository extends JpaRepository<ApiKeyword, Integer> {
    
    /**
     * 키워드명으로 키워드 정보 조회
     * 중복 검사나 특정 키워드 찾기에 사용
     * 
     * @param keywordName 키워드명 (예: "current_weather", "stock_price")
     * @return 해당 키워드 정보 (존재하지 않으면 Optional.empty())
     */
    Optional<ApiKeyword> findByKeywordName(String keywordName);
    
    /**
     * 특정 도메인에 속한 모든 키워드 조회
     * 도메인별로 사용 가능한 세부 분류를 확인할 때 사용
     * 
     * @param domainId 도메인 ID
     * @return 해당 도메인의 키워드 목록
     */
    List<ApiKeyword> findByDomain_DomainId(Integer domainId);
    
    /**
     * 키워드명 중복 여부 확인
     * 새로운 키워드 등록 시 중복 검사에 사용
     * 
     * @param keywordName 확인할 키워드명
     * @return 존재하면 true, 존재하지 않으면 false
     */
    boolean existsByKeywordName(String keywordName);
}