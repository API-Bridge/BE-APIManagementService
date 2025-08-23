package org.example.APIManagementSvc.dto.externalapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * External API 등록 요청 DTO
 * API 정보, 파라미터, 토큰을 한 번에 등록하기 위한 통합 입력 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiRegisterRequest {
    
    /** API의 이름 */
    @NotBlank(message = "API 이름은 필수입니다")
    private String apiName;
    
    /** API의 URL 주소 */
    @NotBlank(message = "API URL은 필수입니다")
    private String apiUrl;
    
    /** API 발급처 */
    @NotBlank(message = "API 발급처는 필수입니다")
    private String apiIssuer;
    
    /** API를 추가한 사용자 ID */
    private String apiOwner;
    
    /** API 분류 도메인 */
    private ApiDomain apiDomain;
    
    /** API 세부분류용 키워드 */
    private ApiKeyword apiKeyword;
    
    /** API의 HTTP 메소드 */
    @NotBlank(message = "HTTP 메소드는 필수입니다")
    private String httpMethod;
    
    /** API에 대한 상세 설명 */
    private String apiDescription;
    
    /** API 파라미터 목록 */
    private List<ApiParameterRegisterRequest> parameters;
    
    /** API 토큰 (선택사항) */
    private String apiToken;
    
    /** 자동 토큰 갱신 여부 (기본값: false) */
    private Boolean autoTokenRefresh = false;
    
    /** 토큰 만료 시간 (선택사항, 설정하지 않으면 4시간 후로 자동 설정) */
    private String tokenExpiresAt;
}

