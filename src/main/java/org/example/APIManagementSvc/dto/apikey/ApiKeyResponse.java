package org.example.APIManagementSvc.dto.apikey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;

import java.time.LocalDateTime;

/**
 * API 키 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {
    
    private Long id;
    
    /** 조직명 */
    private String organizationName;
    
    /** 조직 코드 */
    private String organizationCode;
    
    /** 연락처 이메일 */
    private String contactEmail;
    
    /** 연락처 전화번호 */
    private String contactPhone;
    
    /** API 서비스명 */
    private String apiServiceName;
    
    /** API 서비스 URL */
    private String apiServiceUrl;
    
    /** API 키 */
    private String apiKey;
    
    /** 시크릿 키 */
    private String secretKey;
    
    /** 일일 사용 제한 */
    private Integer dailyLimit;
    
    /** 월간 사용 제한 */
    private Integer monthlyLimit;
    
    /** 만료일 */
    private LocalDateTime expiresAt;
    
    /** 설명 */
    private String description;
    
    /** 요청된 API 목록 */
    private String requestedApis;
    
    /** 상태 */
    private ApiKeyStatus status;
    
    /** 생성일 */
    private LocalDateTime createdAt;
    
    /** 수정일 */
    private LocalDateTime updatedAt;
}
