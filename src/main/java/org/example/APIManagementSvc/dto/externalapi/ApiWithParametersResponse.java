package org.example.APIManagementSvc.dto.externalapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * API와 파라미터 정보를 포함한 응답 DTO
 * 검색 결과에서 사용되며 Lazy Loading 문제를 방지하기 위해 간소화된 DTO를 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiWithParametersResponse {
    
    /** 외부 API 정보 (간소화된 버전) */
    private ExternalApiSimpleResponse api;
    
    /** API 파라미터 목록 (간소화된 정보) */
    private List<ApiParameterSimpleResponse> parameters;
}