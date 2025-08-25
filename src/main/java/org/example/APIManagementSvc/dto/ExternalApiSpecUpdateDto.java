package org.example.APIManagementSvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 외부 API 명세 수정 요청 DTO
 * 기존에 등록된 외부 API 명세 정보를 부분적으로 수정할 때 사용
 * null이 아닌 필드만 업데이트되는 부분 업데이트(Partial Update) 지원
 * 수정 가능한 항목:
 * - 기본 정보: 이름, 설명, 발급처, URL, HTTP 메소드
 * - 연관 정보: 자격증명, 도메인, 키워드
 * - 상태: 활성화/비활성화
 * - 파라미터: 전체 교체 (기존 삭제 후 새로 생성)
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalApiSpecUpdateDto {
    
    /** 수정할 API 이름 (null이면 기존값 유지) */
    private String apiName;
    
    /** 수정할 API 설명 (null이면 기존값 유지) */
    private String apiDescription;
    
    /** 수정할 API 발급처 (null이면 기존값 유지) */
    private String apiIssuer;
    
    /** 수정할 API URL (null이면 기존값 유지) */
    private String apiUrl;
    
    /** 수정할 HTTP 메소드 (null이면 기존값 유지) */
    private String httpMethod;
    
    /** 수정할 자격증명 ID (null이면 기존값 유지) */
    private String credentialId;
    
    /** 수정할 도메인 ID (null이면 기존값 유지) */
    private Integer domainId;
    
    /** 수정할 키워드 ID (null이면 기존값 유지) */
    private Integer keywordId;
    
    /** 수정할 활성화 상태 (null이면 기존값 유지) */
    private Boolean isActive;
    
    /** 
     * 수정할 파라미터 목록 (null이면 파라미터 변경 없음)
     * null이 아니면 기존 파라미터를 모두 삭제하고 새로운 파라미터로 교체
     * 빈 리스트([])를 전달하면 모든 파라미터가 삭제됨
     */
    private List<ApiParameterDto> parameters;
}