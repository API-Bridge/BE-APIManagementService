package org.example.APIManagementSvc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 작업 설정 클래스
 * Spring 스케줄링 기능을 활성화하여 정기적인 작업 실행 지원
 * 
 * 주요 기능:
 * - @Scheduled 애노테이션 활성화
 * - 헬스체크와 같은 정기적인 시스템 작업 실행
 * - 마이크로서비스 환경에서 자동화된 모니터링 작업 지원
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    
    /**
     * 스케줄링 기능 활성화
     * 
     * @EnableScheduling 애노테이션을 통해 Spring의 스케줄링 기능을 활성화
     * 이를 통해 애플리케이션 내의 @Scheduled 애노테이션이 적용된 메서드들이
     * 정의된 주기에 따라 자동으로 실행됨
     * 
     * 현재 프로젝트에서 사용되는 스케줄링 작업:
     * - ApiHealthCheckService.performHealthChecks(): 1시간마다 API 헬스체크 실행
     * 
     * 스케줄링 설정:
     * - 기본 스레드 풀을 사용하여 스케줄링 작업 실행
     * - 각 스케줄링 작업은 별도 스레드에서 비동기로 실행
     * - 애플리케이션 시작 시 스케줄링 작업도 함께 시작됨
     */
}