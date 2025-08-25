package org.example.APIManagementSvc.dto.externalapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API Parameter 간소화된 응답 DTO
 * 검색 결과에서 불필요한 필드들을 제외한 버전
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiParameterSimpleResponse {
    
    /** 이 파라미터가 속한 API의 ID */
    private String apiId;
    
    /** 파라미터 이름 */
    private String paramName;
    
    /** 파라미터의 데이터 타입 */
    private String paramType;
    
    /** 해당 파라미터가 필수인지 여부 */
    private Boolean isRequired;
    
    /** 파라미터의 기본값 */
    private String defaultValue;

    /** 파라미터에 대한 설명 */
    private String paramDescription;
}