package org.example.APIManagementSvc.dto.externalapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 🔥 API 등록 응답 DTO
 * API 등록 완료 후 AI 분류 결과와 함께 응답하는 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiRegistrationResponse {

    /** API 등록 성공 여부 */
    private Boolean success;

    /** API ID */
    private String apiId;

    /** API 이름 */
    private String apiName;

    /** API URL */
    private String apiUrl;

    /** AI 분류 결과 - 도메인 */
    private ApiDomain classifiedDomain;

    /** AI 분류 결과 - 키워드 */
    private ApiKeyword classifiedKeyword;

    /** 등록된 파라미터 개수 */
    private Integer parameterCount;

    /** 등록된 파라미터 목록 */
    private List<ApiParameterResponse> parameters;

    /** 등록 일시 */
    private LocalDateTime registeredAt;

    /** 응답 메시지 */
    private String message;

    /**
     * 🔥 API 파라미터 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiParameterResponse {

        /** 파라미터 ID */
        private String parameterId;

        /** 파라미터 이름 */
        private String paramName;

        /** 파라미터 타입 */
        private String paramType;

        /** 필수 여부 */
        private Boolean isRequired;

        /** 기본값 */
        private String defaultValue;

        /** 파라미터 설명 */
        private String paramDescription;

        /** 동적 추가 필드 */
        private java.util.Map<String, Object> additionalFields;
    }
}
