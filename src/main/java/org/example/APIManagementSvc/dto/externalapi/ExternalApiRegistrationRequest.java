package org.example.APIManagementSvc.dto.externalapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 🔥 API 등록 요청 DTO
 * 관리자가 API를 등록할 때 사용하는 요청 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiRegistrationRequest {

    /** API 이름 */
    @NotBlank(message = "API 이름은 필수입니다")
    @Size(max = 255, message = "API 이름은 255자를 초과할 수 없습니다")
    private String apiName;

    /** API URL 주소 */
    @NotBlank(message = "API URL은 필수입니다")
    @Size(max = 500, message = "API URL은 500자를 초과할 수 없습니다")
    private String apiUrl;

    /** API 발급처 */
    @NotBlank(message = "API 발급처는 필수입니다")
    @Size(max = 255, message = "API 발급처는 255자를 초과할 수 없습니다")
    private String apiIssuer;

    /** API를 추가한 관리자 ID */
    @NotBlank(message = "API 소유자 ID는 필수입니다")
    @Size(max = 36, message = "API 소유자 ID는 36자를 초과할 수 없습니다")
    private String apiOwner;

    /** API의 HTTP 메소드 */
    @NotBlank(message = "HTTP 메소드는 필수입니다")
    @Size(max = 10, message = "HTTP 메소드는 10자를 초과할 수 없습니다")
    private String httpMethod;

    /** API에 대한 상세 설명 */
    @Size(max = 2000, message = "API 설명은 2000자를 초과할 수 없습니다")
    private String apiDescription;

    /** 
     * 🔥 API 파라미터 목록
     * API마다 다른 파라미터 구조를 유연하게 저장
     */
    @NotNull(message = "API 파라미터 목록은 필수입니다")
    private List<ApiParameterRequest> parameters;

    /**
     * 🔥 API 파라미터 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiParameterRequest {

        /** 파라미터 이름 */
        @NotBlank(message = "파라미터 이름은 필수입니다")
        @Size(max = 255, message = "파라미터 이름은 255자를 초과할 수 없습니다")
        private String paramName;

        /** 파라미터의 데이터 타입 */
        @NotBlank(message = "파라미터 타입은 필수입니다")
        @Size(max = 50, message = "파라미터 타입은 50자를 초과할 수 없습니다")
        private String paramType;

        /** 해당 파라미터가 필수인지 여부 */
        @NotNull(message = "필수 여부는 필수입니다")
        private Boolean isRequired;

        /** 파라미터의 기본값 */
        @Size(max = 500, message = "기본값은 500자를 초과할 수 없습니다")
        private String defaultValue;

        /** 파라미터에 대한 설명 */
        @Size(max = 1000, message = "파라미터 설명은 1000자를 초과할 수 없습니다")
        private String paramDescription;

        /** 
         * 🔥 동적 추가 필드
         * API별로 다른 파라미터 속성을 유연하게 저장
         * 예: decimal_places, unit, validation_rules 등
         */
        private java.util.Map<String, Object> additionalFields;
    }
}
