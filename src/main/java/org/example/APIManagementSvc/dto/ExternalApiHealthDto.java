package org.example.APIManagementSvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.APIManagementSvc.domain.Entity.ExternalApiSpec;

import java.time.LocalDateTime;

/**
 * API 헬스체크 상태 응답 DTO
 * LazyInitializationException을 방지하기 위해 필요한 필드만 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalApiHealthDto {

    /** API 고유 식별자 */
    private String apiId;

    /** API 이름 */
    private String apiName;

    /** API 설명 */
    private String apiDescription;

    /** API 발급처 */
    private String apiIssuer;

    /** API URL */
    private String apiUrl;

    /** HTTP 메소드 */
    private String httpMethod;

    /** API 활성화 상태 */
    private Boolean isActive;

    /** 헬스체크 상태 */
    private ExternalApiSpec.HealthStatus healthStatus;

    /** 헬스체크 경로 */
    private String healthCheckPath;

    /** 마지막 헬스체크 수행 시간 */
    private LocalDateTime lastHealthCheck;

    /** 생성 시간 */
    private LocalDateTime createdAt;

    /** 수정 시간 */
    private LocalDateTime updatedAt;

    /**
     * ExternalApiSpec 엔티티를 DTO로 변환
     */
    public static ExternalApiHealthDto from(ExternalApiSpec apiSpec) {
        return ExternalApiHealthDto.builder()
                .apiId(apiSpec.getApiId())
                .apiName(apiSpec.getApiName())
                .apiDescription(apiSpec.getApiDescription())
                .apiIssuer(apiSpec.getApiIssuer())
                .apiUrl(apiSpec.getApiUrl())
                .httpMethod(apiSpec.getHttpMethod())
                .isActive(apiSpec.getIsActive())
                .healthStatus(apiSpec.getHealthStatus())
                .healthCheckPath(apiSpec.getHealthCheckPath())
                .lastHealthCheck(apiSpec.getLastHealthCheck())
                .createdAt(apiSpec.getCreatedAt())
                .updatedAt(apiSpec.getUpdatedAt())
                .build();
    }
}