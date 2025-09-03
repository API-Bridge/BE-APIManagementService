package org.example.APIManagementSvc.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ApiToken;
import org.example.APIManagementSvc.service.SgisTokenService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SGIS 토큰 자동 갱신 스케줄러
 * 
 * 통계청 SGIS API의 액세스 토큰을 주기적으로 자동 갱신하는 스케줄링 서비스
 * Spring의 @Scheduled 애노테이션을 사용하여 일정한 주기로 토큰 갱신 작업을 수행
 * 
 * 스케줄링 정책:
 * - 실행 주기: 3시간 30분 (12,600,000 밀리초)
 * - 대상: SGIS 자격증명만 (credential_id = 'SGIS')
 * - 실행 방식: fixedRate (이전 실행 시작 시점 기준)
 * 
 * 토큰 갱신 로직:
 * 1. 현재 SGIS 토큰 상태 확인
 * 2. 토큰이 없거나 만료 임박 시 새 토큰 발급
 * 3. 발급 성공/실패에 대한 로그 기록
 * 4. 예외 발생 시에도 스케줄러 중단 방지
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SgisTokenScheduler {

    /** SGIS 토큰 관리 서비스 */
    private final SgisTokenService sgisTokenService;
    
    /** 토큰 갱신 사전 확인 시간 (분) - 30분 전에 미리 확인 */
    private static final int TOKEN_RENEWAL_CHECK_MINUTES = 30;

    /**
     * SGIS 토큰 정기 갱신 스케줄러
     * 
     * 매 3시간 30분마다 실행되어 SGIS 액세스 토큰을 자동으로 갱신
     * fixedRate 방식으로 이전 실행 시작 시점부터 정확히 3시간 30분 후에 실행
     * 
     * 실행 조건:
     * - Spring 애플리케이션이 실행 중일 때
     * - SchedulingConfig에서 @EnableScheduling이 활성화된 상태
     * - 이전 스케줄링 작업과 독립적으로 실행
     * 
     * 처리 과정:
     * 1. 스케줄러 실행 시작 로그 기록
     * 2. 현재 토큰 상태 확인 (만료 여부, 만료 임박 여부)
     * 3. 갱신이 필요한 경우 새 토큰 발급
     * 4. 결과에 따른 성공/실패 로그 기록
     * 5. 예외 발생 시 에러 로그 기록 후 스케줄러 지속
     * 
     * @throws Exception 스케줄링 중 발생하는 모든 예외 (캐치하여 로그 기록)
     */
    @Scheduled(fixedRate = 12600000) // 3시간 30분 = 3.5 * 60 * 60 * 1000 = 12,600,000ms
    public void renewSgisToken() {
        log.info("=== SGIS Token Renewal Scheduler Started ===");
        
        try {
            // 현재 토큰 상태 확인
            boolean isTokenValid = sgisTokenService.isTokenValid();
            boolean isExpiringWithin30Minutes = sgisTokenService.isTokenExpiringWithin(TOKEN_RENEWAL_CHECK_MINUTES);
            
            log.info("Current SGIS token status - Valid: {}, Expiring within {} minutes: {}", 
                    isTokenValid, TOKEN_RENEWAL_CHECK_MINUTES, isExpiringWithin30Minutes);
            
            // 토큰 갱신 필요 여부 판단
            if (!isTokenValid || isExpiringWithin30Minutes) {
                log.info("SGIS token renewal required - issuing new token");
                
                // 새 토큰 발급
                ApiToken newToken = sgisTokenService.issueToken();
                
                log.info("SGIS token renewal successful - Token ID: {}, Expires at: {}", 
                        newToken.getTokenId(), newToken.getExpiresAt());
                        
            } else {
                log.info("SGIS token is still valid - skipping renewal");
            }
            
        } catch (IllegalStateException e) {
            // 자격증명 관련 오류 (설정 문제)
            log.error("SGIS token renewal failed due to credential configuration issue: {}", e.getMessage());
            log.error("Please check SGIS credential configuration in the database");
            
        } catch (RuntimeException e) {
            // API 호출 실패, 네트워크 오류 등
            log.error("SGIS token renewal failed due to runtime error: {}", e.getMessage(), e);
            log.error("Will retry in the next scheduled execution (3.5 hours)");
            
        } catch (Exception e) {
            // 예상하지 못한 오류
            log.error("Unexpected error during SGIS token renewal: {}", e.getMessage(), e);
            log.error("Scheduler will continue running for next execution");
            
        } finally {
            log.info("=== SGIS Token Renewal Scheduler Completed ===");
        }
    }

    /**
     * SGIS 토큰 상태 모니터링 스케줄러 (선택사항)
     * 
     * 매 30분마다 실행되어 SGIS 토큰의 상태를 모니터링하고 로그로 기록
     * 토큰 갱신 스케줄러와는 별도로 동작하여 토큰 상태를 주기적으로 확인
     * 
     * 주요 목적:
     * - 토큰 만료 시간 추적
     * - 토큰 갱신이 정상적으로 이루어지는지 모니터링
     * - 문제 발생 시 조기 감지 및 알림
     * 
     * @throws Exception 모니터링 중 발생하는 예외 (로그 기록 후 계속 진행)
     */
    @Scheduled(fixedRate = 1800000) // 30분 = 30 * 60 * 1000 = 1,800,000ms
    public void monitorSgisToken() {
        try {
            log.debug("=== SGIS Token Status Monitoring ===");
            
            // 현재 토큰 상태 조회
            var tokenStatus = sgisTokenService.getTokenStatus();
            
            if (tokenStatus.isPresent()) {
                ApiToken token = tokenStatus.get();
                boolean isValid = sgisTokenService.isTokenValid();
                boolean isExpiringWithin30Minutes = sgisTokenService.isTokenExpiringWithin(TOKEN_RENEWAL_CHECK_MINUTES);
                
                log.debug("SGIS Token Status - ID: {}, Valid: {}, Expires at: {}, Expiring within 30min: {}", 
                        token.getTokenId(), isValid, token.getExpiresAt(), isExpiringWithin30Minutes);
                
                // 만료 임박 경고
                if (isExpiringWithin30Minutes && isValid) {
                    log.warn("SGIS token is expiring within {} minutes! Next renewal scheduled at next interval.", 
                            TOKEN_RENEWAL_CHECK_MINUTES);
                }
                
            } else {
                log.warn("No SGIS token found in database - will be issued at next renewal cycle");
            }
            
        } catch (Exception e) {
            log.debug("Error during SGIS token monitoring: {}", e.getMessage());
            // 모니터링 실패는 치명적이지 않으므로 DEBUG 레벨로 기록
        }
    }
}