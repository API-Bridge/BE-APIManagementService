package org.example.APIManagementSvc.dto.externalapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * API 파라미터 등록 요청 DTO
 * API 파라미터를 시스템에 등록하기 위한 입력 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiParameterRegisterRequest {
    
    /** 파라미터 이름 */
    @NotBlank(message = "파라미터 이름은 필수입니다")
    private String paramName;
    
    /** 파라미터의 데이터 타입 */
    @NotBlank(message = "파라미터 타입은 필수입니다")
    private String paramType;
    
    /** 해당 파라미터가 필수인지 여부 */
    @NotNull(message = "필수 여부는 필수입니다")
    private Boolean isRequired;
    
    /** 파라미터의 기본값 */
    private String defaultValue;
}

