package org.example.APIManagementSvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 파라미터 정보 전달용 DTO
 * 
 * 외부 API의 파라미터 정보를 클라이언트와 서버 간에 전달할 때 사용
 * 각 API마다 파라미터의 개수와 타입이 다르므로 유연한 구조로 설계
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiParameterDto {
    
    /** 파라미터 고유 식별자 (UUID) */
    private String parameterId;
    
    /** 파라미터 이름 (예: lat, lng, serviceKey 등) */
    private String paramName;
    
    /** 파라미터 데이터 타입 (string, integer, boolean 등) */
    private String paramType;
    
    /** 필수 파라미터 여부 */
    private Boolean isRequired;
    
    /** 파라미터 상세 설명 */
    private String paramDescription;
    
    /** 파라미터 기본값 (선택사항) */
    private String defaultValue;
    
    /** JSON 형태의 추가 필드 (확장성을 위한 필드) */
    private String additionalFields;
}