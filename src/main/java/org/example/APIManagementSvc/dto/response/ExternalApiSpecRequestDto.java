package org.example.APIManagementSvc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import org.example.APIManagementSvc.dto.ApiParameterDto;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalApiSpecRequestDto {
    
    private String apiId; // UUID로 자동 생성됨 (사용자 입력 불필요)
    
    @NotBlank(message = "API 이름은 필수입니다")
    private String apiName;
    
    private String apiDescription;
    
    @NotBlank(message = "API 발급처는 필수입니다")
    private String apiIssuer;
    
    @NotBlank(message = "API URL은 필수입니다")
    private String apiUrl;
    
    @NotBlank(message = "HTTP 메소드는 필수입니다")
    private String httpMethod;
    
    @NotBlank(message = "액세스키 ID는 필수입니다")
    private String credentialId;
    
    private Integer domainId;
    private Integer keywordId;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private List<ApiParameterDto> parameters;
}