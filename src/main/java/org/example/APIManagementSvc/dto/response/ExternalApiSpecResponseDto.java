package org.example.APIManagementSvc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.APIManagementSvc.dto.ApiParameterDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalApiSpecResponseDto {
    
    private String apiId;
    private String apiName;
    private String apiDescription;
    private String apiIssuer;
    private String apiUrl;
    private String httpMethod;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String credentialId;
    private String organizationName;
    private Integer domainId;
    private String domainName;
    private Integer keywordId;
    private String keywordName;
    
    private List<ApiParameterDto> parameters;
}