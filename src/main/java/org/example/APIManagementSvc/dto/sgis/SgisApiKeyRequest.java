package org.example.APIManagementSvc.dto.sgis;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SGIS API 키 등록 요청 DTO
 * 
 * 기관이 SGIS 공공데이터포털에서 발급받은 API 키를
 * 우리 시스템에 등록하기 위한 요청 데이터입니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SgisApiKeyRequest {

    /**
     * 기관명 (필수)
     */
    @NotBlank(message = "기관명은 필수입니다")
    @Size(max = 255, message = "기관명은 255자를 초과할 수 없습니다")
    private String organizationName;

    /**
     * 기관 코드 (선택)
     */
    @Size(max = 100, message = "기관 코드는 100자를 초과할 수 없습니다")
    private String organizationCode;

    /**
     * 연락처 이메일 (필수)
     */
    @NotBlank(message = "연락처 이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다")
    private String contactEmail;

    /**
     * 연락처 전화번호 (선택)
     */
    @Size(max = 50, message = "전화번호는 50자를 초과할 수 없습니다")
    private String contactPhone;

    /**
     * SGIS에서 발급받은 실제 API 키 (필수)
     */
    @NotBlank(message = "API 키는 필수입니다")
    @Size(max = 500, message = "API 키는 500자를 초과할 수 없습니다")
    private String apiKey;

    /**
     * SGIS에서 발급받은 시크릿 키 (필수)
     */
    @NotBlank(message = "시크릿 키는 필수입니다")
    @Size(max = 500, message = "시크릿 키는 500자를 초과할 수 없습니다")
    private String secretKey;

    /**
     * 요청한 API 목록 (선택)
     */
    private List<String> requestedApis;

    /**
     * 일일 API 호출 제한 횟수 (필수)
     */
    @NotNull(message = "일일 사용량 제한은 필수입니다")
    @Min(value = 1, message = "일일 사용량 제한은 1 이상이어야 합니다")
    @Max(value = 1000000, message = "일일 사용량 제한은 1,000,000을 초과할 수 없습니다")
    private Integer dailyLimit;

    /**
     * 월간 API 호출 제한 횟수 (필수)
     */
    @NotNull(message = "월간 사용량 제한은 필수입니다")
    @Min(value = 1, message = "월간 사용량 제한은 1 이상이어야 합니다")
    @Max(value = 10000000, message = "월간 사용량 제한은 10,000,000을 초과할 수 없습니다")
    private Integer monthlyLimit;

    /**
     * API 키 만료일시 (필수)
     */
    @NotNull(message = "만료일시는 필수입니다")
    @Future(message = "만료일시는 현재 시간 이후여야 합니다")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    /**
     * API 키 설명 (선택)
     */
    @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
    private String description;
}
