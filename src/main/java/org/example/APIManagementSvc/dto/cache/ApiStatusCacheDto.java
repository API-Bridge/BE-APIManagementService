package org.example.APIManagementSvc.dto.cache;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;

import java.time.LocalDateTime;

/**
 * API 상태 정보를 Redis에 캐싱하기 위한 DTO입니다.
 * 
 * ⚠️  중요: 이 DTO는 Redis 캐싱 전용으로 사용됩니다!
 * - API의 현재 상태, 기본 정보, 사용량 통계 등을 캐싱
 * - 캐시 만료 시간과 함께 저장되어 자동으로 갱신
 * - API 응답 속도 향상과 서버 부하 감소를 목적으로 함
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiStatusCacheDto {
    
    /**
     * API 식별자
     */
    private String apiId;
    
    /**
     * API 이름
     */
    private String apiName;
    
    /**
     * API 도메인 (통계, 지리정보, 교통 등)
     */
    private ApiDomain domain;
    
    /**
     * API 키워드 (인구통계, 날씨, 교통상황 등)
     */
    private ApiKeyword keyword;
    
    /**
     * API 상태 (ACTIVE, INACTIVE, MAINTENANCE, ERROR)
     */
    private String status;
    
    /**
     * API 사용량 한도 (일일/월간)
     */
    private Long dailyLimit;
    private Long monthlyLimit;
    
    /**
     * 현재 사용량
     */
    private Long currentDailyUsage;
    private Long currentMonthlyUsage;
    
    /**
     * 마지막 사용 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUsedAt;
    
    /**
     * API 제공자 (SGIS, 공공데이터포털 등)
     */
    private String provider;
    
    /**
     * API 버전
     */
    private String version;
    
    /**
     * API 문서 URL
     */
    private String documentationUrl;
    
    /**
     * 지원 연락처
     */
    private String supportContact;
    
    /**
     * 마지막 업데이트 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdated;
    
    /**
     * 캐시 만료 시간 (초)
     */
    private Long ttlSeconds;
}
