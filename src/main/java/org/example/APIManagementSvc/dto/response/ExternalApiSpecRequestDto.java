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
    
    @NotBlank(message = "API ID는 필수입니다")
    private String apiId;
    
    @NotBlank(message = "API 이름은 필수입니다")
    private String apiName;
    
    private String apiDescription;
    
    @NotBlank(message = "API 발급처는 필수입니다")
    private String apiIssuer;
    
    @NotBlank(message = "API URL은 필수입니다")
    private String apiUrl;
    
    @NotBlank(message = "HTTP 메소드는 필수입니다")
    private String httpMethod;
    
    @NotBlank(message = "자격증명 ID는 필수입니다")
    private String credentialId;
    
    private Integer domainId;
    private Integer keywordId;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private List<ApiParameterDto> parameters;
}