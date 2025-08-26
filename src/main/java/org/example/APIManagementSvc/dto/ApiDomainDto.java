package org.example.APIManagementSvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDomainDto {
    
    private Integer domainId;
    
    @NotBlank(message = "도메인명은 필수입니다")
    @Size(max = 100, message = "도메인명은 100자 이하여야 합니다")
    private String domainName;
    
    @Size(max = 500, message = "도메인 설명은 500자 이하여야 합니다")
    private String description;
    
    private LocalDateTime createdAt;
    
    private Integer keywordCount;
    
    private Integer apiSpecCount;
    
    @Valid
    private List<ApiKeywordRequestDto> keywords;
    
    private List<ApiKeywordDto> keywordDetails;
}