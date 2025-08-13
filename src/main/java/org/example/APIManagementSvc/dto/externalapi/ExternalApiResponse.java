package org.example.APIManagementSvc.dto.externalapi;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;

import java.time.LocalDateTime;

/**
 * External API 응답 DTO
 * ExternalApi 엔티티와 동일한 필드명 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiResponse {
    
    /** 외부 API의 고유 식별자 */
    private String apiId;

    /** API의 이름 */
    private String apiName;

    /** API의 URL 주소 */
    private String apiUrl;

    /** API 발급처 */
    private String apiIssuer;

    /** API를 추가한 사용자 ID */
    private String apiOwner;

    /** API 분류 도메인 */
    private ApiDomain apiDomain;

    /** API 세부분류용 키워드 */
    private ApiKeyword apiKeyword;

    /** API의 HTTP 메소드 */
    private String httpMethod;

    /** API에 대한 상세 설명 */
    private String apiDescription;

    /** API 유효성 상태 */
    private Boolean apiEffectiveness;

    /** 생성 일시 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 수정 일시 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    /** 삭제 여부 */
    private Boolean deleted;

    // deleted 필드는 클라이언트에게 노출할 필요 없으므로 제외
}
