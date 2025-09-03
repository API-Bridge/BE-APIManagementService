package org.example.APIManagementSvc.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 자격증명 등록 요청 DTO
 * 
 * 외부 API 호출에 필요한 인증 정보를 등록할 때 사용
 * 보안이 중요한 정보이므로 입력 검증을 엄격하게 적용
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiCredentialRequestDto {

    @NotBlank(message = "자격증명 ID는 필수입니다")
    private String credentialId;

    @NotBlank(message = "기관명은 필수입니다")
    private String organizationName;

    @NotBlank(message = "API 키는 필수입니다")
    private String apiKey;

    private String secretKey;

    private LocalDateTime issuedAt;

    @Email(message = "올바른 이메일 형식이어야 합니다")
    private String contactEmail;

}