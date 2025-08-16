package org.example.APIManagementSvc.dto.sgis;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;

import java.time.LocalDateTime;

/**
 * SGIS API 키 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SgisApiKeyResponse {

    private String keyId;
    private String organizationName;
    private String organizationCode;
    private String contactEmail;
    private String contactPhone;
    private String apiKey;
    private String secretKey;
    private Integer dailyLimit;
    private Integer monthlyLimit;
    private Integer currentDailyUsage;
    private Integer currentMonthlyUsage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUsedAt;

    private ApiKeyStatus status;
    private String description;
    private String requestedApis;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
