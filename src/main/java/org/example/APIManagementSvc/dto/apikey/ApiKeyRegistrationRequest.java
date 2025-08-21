package org.example.APIManagementSvc.dto.apikey;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API 키 등록 요청 DTO
 */
@Data
public class ApiKeyRegistrationRequest {

    @NotBlank(message = "기관명은 필수입니다")
    private String organizationName;

    private String organizationCode;

    @NotBlank(message = "연락처 이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이어야 합니다")
    private String contactEmail;

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이어야 합니다")
    private String contactPhone;

    @NotBlank(message = "API 서비스명은 필수입니다")
    private String apiServiceName;

    private String apiServiceUrl;

    @NotBlank(message = "API 키는 필수입니다")
    private String apiKey;

    private String secretKey;

    private Integer dailyLimit;

    private Integer monthlyLimit;

    private LocalDateTime expiresAt;

    private String description;

    private String requestedApis;
}