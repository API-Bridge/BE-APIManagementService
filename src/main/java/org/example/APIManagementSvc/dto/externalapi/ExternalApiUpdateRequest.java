package org.example.APIManagementSvc.dto.externalapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * External API 수정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiUpdateRequest {

    /**
     * API 이름
     */
    @Size(max = 255, message = "API 이름은 255자를 초과할 수 없습니다")
    private String apiName;

    /**
     * API 설명
     */
    @Size(max = 1000, message = "API 설명은 1000자를 초과할 수 없습니다")
    private String apiDescription;

    /**
     * API URL
     */
    @Size(max = 500, message = "API URL은 500자를 초과할 수 없습니다")
    private String apiUrl;

    /**
     * HTTP 메소드 (GET, POST, PUT, DELETE 등)
     */
    @Size(max = 10, message = "HTTP 메소드는 10자를 초과할 수 없습니다")
    private String httpMethod;

    /**
     * API 도메인 분류
     */
    @Size(max = 100, message = "도메인은 100자를 초과할 수 없습니다")
    private String apiDomain;

    /**
     * API 키워드 분류
     */
    @Size(max = 100, message = "키워드는 100자를 초과할 수 없습니다")
    private String apiKeyword;

    /**
     * API 제공기관/업체
     */
    @Size(max = 255, message = "제공기관은 255자를 초과할 수 없습니다")
    private String apiIssuer;

    /**
     * API 유효성 (활성/비활성)
     */
    private Boolean apiEffectiveness;

    /** API 파라미터 목록 */
    private List<ApiParameterRegisterRequest> parameters;
}
