package org.example.APIManagementSvc.dto.apikey;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API 키 수정 요청 DTO
 */
@Data
public class ApiKeyUpdateRequest {

    private String organizationName;

    private String organizationCode;

    @Email(message = "올바른 이메일 형식이어야 합니다")
    private String contactEmail;

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이어야 합니다")
    private String contactPhone;

    private String apiServiceUrl;

    private Integer dailyLimit;

    private Integer monthlyLimit;

    private LocalDateTime expiresAt;

    private String description;

    private String requestedApis;
}