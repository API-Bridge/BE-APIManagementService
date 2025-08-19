package org.example.APIManagementSvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.dto.log.LogEntry;
import org.example.APIManagementSvc.service.LogCollectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 🔥 로그 수집 컨트롤러
 * 다른 MSA에서 ELK 스택으로 로그를 수집할 수 있도록 제공
 * 
 * 중요: 이 컨트롤러는 로그를 데이터베이스에 저장하지 않고 ELK 엔드포인트로만 전송합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogCollectionController {

    private final LogCollectionService logCollectionService;

    /**
     * 🔥 단일 로그 수집 엔드포인트
     * 다른 MSA에서 단일 로그 엔트리를 전송
     * 
     * @param logEntry 전송할 로그 엔트리
     * @return 수집 완료 응답
     */
    @PostMapping("/collect")
    public ResponseEntity<ApiResponse<String>> collectSingleLog(@Valid @RequestBody LogEntry logEntry) {
        log.info("단일 로그 수집 요청: {} - {}", logEntry.getLevel(), logEntry.getMessage());
        
        try {
            logCollectionService.collectLog(logEntry);
            
            return ResponseEntity.ok(ApiResponse.success(
                "로그가 성공적으로 수집되었습니다",
                "로그 수집 완료"
            ));
            
        } catch (Exception e) {
            log.error("로그 수집 실패", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("로그 수집에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 🔥 배치 로그 수집 엔드포인트
     * 다른 MSA에서 여러 로그 엔트리를 한 번에 전송
     * 
     * @param logEntries 전송할 로그 엔트리 목록
     * @return 수집 완료 응답
     */
    @PostMapping("/collect-batch")
    public ResponseEntity<ApiResponse<String>> collectBatchLogs(@Valid @RequestBody List<LogEntry> logEntries) {
        log.info("배치 로그 수집 요청: {}개 로그", logEntries.size());
        
        try {
            logCollectionService.collectBatchLogs(logEntries);
            
            return ResponseEntity.ok(ApiResponse.success(
                String.format("%d개의 로그가 성공적으로 수집되었습니다", logEntries.size()),
                "배치 로그 수집 완료"
            ));
            
        } catch (Exception e) {
            log.error("배치 로그 수집 실패", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("배치 로그 수집에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 🔥 로그 수집 서비스 상태 확인
     * ELK 연동 상태 및 서비스 동작 여부 확인
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> getLogServiceHealth() {
        log.info("로그 수집 서비스 상태 확인 요청");
        
        try {
            boolean isHealthy = logCollectionService.isServiceHealthy();
            
            if (isHealthy) {
                return ResponseEntity.ok(ApiResponse.success(
                    "로그 수집 서비스가 정상적으로 동작 중입니다. ELK 스택과 연동되어 로그를 수집할 수 있습니다.",
                    "서비스 상태: 정상"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("로그 수집 서비스에 문제가 있습니다. ELK 스택 연결을 확인해주세요."));
            }
            
        } catch (Exception e) {
            log.error("로그 수집 서비스 상태 확인 실패", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("로그 수집 서비스 상태 확인에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 🔥 로그 수집 통계 정보
     * 수집된 로그 개수, 레벨별 분포 등 통계 정보 제공
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getLogCollectionStats() {
        log.info("로그 수집 통계 요청");
        
        try {
            var stats = logCollectionService.getCollectionStats();
            
            return ResponseEntity.ok(ApiResponse.success(stats, "로그 수집 통계"));
            
        } catch (Exception e) {
            log.error("로그 수집 통계 조회 실패", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("로그 수집 통계 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 🔥 로그 수집 가이드
     * 다른 MSA에서 로그를 전송하는 방법과 형식 안내
     */
    @GetMapping("/guide")
    public ResponseEntity<ApiResponse<String>> getLogCollectionGuide() {
        log.info("로그 수집 가이드 요청");
        
        String guide = """
            🔥 로그 수집 가이드
            
            📋 엔드포인트:
            - POST /api/v1/logs/collect: 단일 로그 수집
            - POST /api/v1/logs/collect-batch: 배치 로그 수집
            - GET /api/v1/logs/health: 서비스 상태 확인
            - GET /api/v1/logs/stats: 수집 통계
            - GET /api/v1/logs/guide: 이 가이드
            
            📝 로그 형식:
            - level: 로그 레벨 (INFO, ERROR, WARN, DEBUG, TRACE)
            - message: 로그 메시지
            - timestamp: 타임스탬프 (자동 설정)
            - location: 로그 발생 위치
            - requestId: 요청 ID (MSA 간 트랜잭션 추적용)
            - userId: 사용자 ID
            - endpoint: API 엔드포인트
            - httpMethod: HTTP 메소드
            - statusCode: HTTP 상태 코드
            - responseTime: 응답 시간 (밀리초)
            - clientIp: 클라이언트 IP
            - userAgent: User-Agent
            - metadata: 추가 메타데이터 (Map)
            
            💾 저장 방식:
            - 데이터베이스에 저장하지 않음
            - ELK 스택으로 직접 전송
            - 로그 분석 및 모니터링에 활용
            
            🔗 연동 방법:
            - HTTP POST 요청으로 로그 전송
            - JSON 형식으로 데이터 전달
            - 인증 및 권한 확인 필요
            """;
        
        return ResponseEntity.ok(ApiResponse.success(guide, "로그 수집 가이드"));
    }
}
