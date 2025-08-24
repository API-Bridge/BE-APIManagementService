package org.example.APIManagementSvc.dto.externalapi;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;

import java.time.LocalDateTime;

/**
 * 검색 결과용 간소화된 외부 API 응답 DTO
 * 지연 로딩 문제를 방지하기 위해 연관 관계를 제외한 기본 필드만 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiSimpleResponse {
    
    /** 외부 API의 고유 식별자 */
    private String apiId;
    
    /** API의 이름 */
    private String apiName;
    
    /** API의 URL 주소 */
    private String apiUrl;
    
    /** API 발급처 */
    private String apiIssuer;
    
    /** API를 추가한 관리자 ID */
    private String apiOwner;
    
    /** API 분류 도메인 */
    private ApiDomain apiDomain;
    
    /** API 분류 키워드 */
    private ApiKeyword apiKeyword;
    
    /** HTTP 메소드 */
    private String httpMethod;
    
    /** API에 대한 설명 */
    private String apiDescription;
    
    /** API 유효성 여부 */
    private Boolean apiEffectiveness;
    
    /** 생성 일시 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /** 수정 일시 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}