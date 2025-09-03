package org.example.APIManagementSvc.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.APIManagementSvc.domain.Entity.ApiCredential;

import java.time.LocalDateTime;

/**
 * API 자격증명 수정 요청 DTO
 * 
 * 기존 자격증명 정보를 부분 수정할 때 사용
 * null이 아닌 필드만 업데이트되는 Partial Update 지원
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiCredentialUpdateDto {

    private String organizationName;
    private String apiKey;
    private String secretKey;
    private ApiCredential.CredentialStatus status;
    private LocalDateTime issuedAt;

    @Email(message = "올바른 이메일 형식이어야 합니다")
    private String contactEmail;
}