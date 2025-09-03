package org.example.APIManagementSvc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.APIManagementSvc.domain.Entity.ApiCredential;

import java.time.LocalDateTime;

/**
 * API 자격증명 응답 DTO
 * 
 * 자격증명 정보를 클라이언트에게 안전하게 전달할 때 사용
 * 보안상 민감한 정보(secretKey)는 마스킹 처리하여 노출 최소화
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiCredentialResponseDto {

    private String credentialId;
    private String organizationName;
    private String maskedApiKey;      // 마스킹된 API 키 (앞 4자리만 표시)
    private boolean hasSecretKey;     // Secret Key 보유 여부만 표시
    private ApiCredential.CredentialStatus status;
    private LocalDateTime issuedAt;
    private String contactEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity에서 Response DTO로 변환
     * 민감한 정보는 마스킹 처리
     */
    public static ApiCredentialResponseDto from(ApiCredential credential) {
        return ApiCredentialResponseDto.builder()
                .credentialId(credential.getCredentialId())
                .organizationName(credential.getOrganizationName())
                .maskedApiKey(maskApiKey(credential.getApiKey()))
                .hasSecretKey(credential.getSecretKey() != null && !credential.getSecretKey().isEmpty())
                .status(credential.getStatus())
                .issuedAt(credential.getIssuedAt())
                .contactEmail(credential.getContactEmail())
                .createdAt(credential.getCreatedAt())
                .updatedAt(credential.getUpdatedAt())
                .build();
    }

    /**
     * API 키 마스킹 처리
     * 앞 4자리만 보여주고 나머지는 '*'로 처리
     */
    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) {
            return "****";
        }
        return apiKey.substring(0, 4) + "*".repeat(apiKey.length() - 4);
    }
}