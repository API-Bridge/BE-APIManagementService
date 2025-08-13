package org.example.APIManagementSvc.dto.externalapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 통계 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiStatisticsResponse {
    
    /**
     * 전체 API 개수
     */
    private long totalApis;
    
    /**
     * 활성 API 개수 (삭제되지 않은)
     */
    private long activeApis;
    
    /**
     * 유효한 API 개수 (effectiveness = true)
     */
    private long effectiveApis;
    
    /**
     * 비활성 API 개수 (삭제된)
     */
    private long inactiveApis;
    
    /**
     * 유효하지 않은 API 개수 (effectiveness = false)
     */
    private long ineffectiveApis;
    

}

