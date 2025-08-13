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
 * 기존 API를 시스템에 등록하기 위한 입력 데이터
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
    @NotNull(message = "API 도메인은 필수입니다")
    private ApiDomain apiDomain;
    
    /** API 세부분류용 키워드 */
    @NotNull(message = "API 키워드는 필수입니다")
    private ApiKeyword apiKeyword;
    
    /** API의 HTTP 메소드 */
    @NotBlank(message = "HTTP 메소드는 필수입니다")
    private String httpMethod;
    
    /** API에 대한 상세 설명 */
    private String apiDescription;
    
    /** API 파라미터 목록 */
    private List<ApiParameterRegisterRequest> parameters;
}

