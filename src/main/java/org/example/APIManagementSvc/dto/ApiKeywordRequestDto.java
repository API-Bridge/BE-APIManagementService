package org.example.APIManagementSvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeywordRequestDto {
    
    @NotBlank(message = "키워드명은 필수입니다")
    @Size(max = 100, message = "키워드명은 100자 이하여야 합니다")
    private String keywordName;
    
    @Size(max = 500, message = "키워드 설명은 500자 이하여야 합니다")
    private String description;
}