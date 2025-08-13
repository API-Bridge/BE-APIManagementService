package org.example.APIManagementSvc.dto.externalapi;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API Parameter 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiParameterResponse {
    
    /** 파라미터 정보 고유 식별자 */
    private String parameterId;

    /** 이 파라미터가 속한 API의 ID */
    private String apiId;
    
    /** 파라미터 이름 */
    private String paramName;
    
    /** 파라미터의 데이터 타입 */
    private String paramType;
    
    /** 해당 파라미터가 필수인지 여부 */
    private Boolean isRequired;
    
    /** 파라미터의 기본값 */
    private String defaultValue;

    /** 생성 일시 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 수정 일시 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // deleted 필드는 클라이언트에게 노출할 필요 없으므로 제외
}

