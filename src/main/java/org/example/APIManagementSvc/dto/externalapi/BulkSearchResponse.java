package org.example.APIManagementSvc.dto.externalapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 벌크 검색 응답 DTO
 * 도메인-키워드 조합별로 결과를 그룹핑하여 반환
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkSearchResponse {
    
    /** 전체 매칭된 API 개수 */
    private int totalCount;
    
    /** 검색 결과 요약 */
    private SearchSummary summary;
    
    /** 도메인-키워드 조합별 결과 */
    private Map<String, List<ApiWithParametersResponse>> results;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchSummary {
        /** 요청된 도메인 개수 */
        private int requestedDomains;
        
        /** 요청된 키워드 개수 */
        private int requestedKeywords;
        
        /** 실제 매칭된 조합 개수 */
        private int matchedCombinations;
        
        /** 전체 발견된 API 개수 */
        private int totalApis;
    }
}