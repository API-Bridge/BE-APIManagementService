package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.dto.log.LogEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🔥 로그 수집 서비스
 * 다른 MSA에서 전송된 로그를 ELK 스택으로 전달
 * 
 * 중요: 이 서비스는 로그를 데이터베이스에 저장하지 않고 ELK 엔드포인트로만 전송합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogCollectionService {

    private final RestTemplate restTemplate;

    // 🔥 ELK 스택 설정 (application.yml에서 주입)
    @Value("${elk.logstash.host:localhost}")
    private String logstashHost;

    @Value("${elk.logstash.port:5000}")
    private int logstashPort;

    @Value("${elk.logstash.endpoint:/}")
    private String logstashEndpoint;

    @Value("${elk.enabled:false}")
    private boolean elkEnabled;

    // 🔥 로그 수집 통계를 위한 인메모리 카운터
    private final Map<String, AtomicLong> logLevelCounters = new ConcurrentHashMap<>();
    private final AtomicLong totalLogsCollected = new AtomicLong(0);
    private final AtomicLong lastCollectionTime = new AtomicLong(System.currentTimeMillis());

    /**
     * 🔥 단일 로그 수집
     * @param logEntry 수집할 로그 엔트리
     */
    public void collectLog(LogEntry logEntry) {
        if (logEntry == null) {
            log.warn("수집할 로그 엔트리가 null입니다");
            return;
        }

        try {
            // 🔥 로그 수집 통계 업데이트
            updateCollectionStats(logEntry);

            // 🔥 ELK 스택으로 로그 전송
            if (elkEnabled) {
                sendToElk(logEntry);
            } else {
                log.info("ELK 스택이 비활성화되어 있습니다. 로그를 로컬에만 출력합니다: {}", logEntry.getMessage());
            }

            log.debug("로그 수집 완료: {} - {}", logEntry.getLevel(), logEntry.getMessage());

        } catch (Exception e) {
            log.error("로그 수집 중 오류 발생: {}", logEntry.getMessage(), e);
            // 🔥 로그 수집 실패 시에도 로컬에 기록
            logLocal(logEntry, e);
        }
    }

    /**
     * 🔥 배치 로그 수집
     * @param logEntries 수집할 로그 엔트리 목록
     */
    public void collectBatchLogs(List<LogEntry> logEntries) {
        if (logEntries == null || logEntries.isEmpty()) {
            log.warn("수집할 배치 로그가 비어있습니다");
            return;
        }

        log.info("배치 로그 수집 시작: {}개 로그", logEntries.size());

        try {
            // 🔥 각 로그를 개별적으로 처리
            for (LogEntry logEntry : logEntries) {
                collectLog(logEntry);
            }

            log.info("배치 로그 수집 완료: {}개 로그", logEntries.size());

        } catch (Exception e) {
            log.error("배치 로그 수집 중 오류 발생: {}개 로그", logEntries.size(), e);
        }
    }

    /**
     * 🔥 ELK 스택으로 로그 전송
     * @param logEntry 전송할 로그 엔트리
     */
    private void sendToElk(LogEntry logEntry) {
        try {
            String elkUrl = String.format("http://%s:%d%s", logstashHost, logstashPort, logstashEndpoint);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<LogEntry> request = new HttpEntity<>(logEntry, headers);
            
            // 🔥 ELK 스택으로 HTTP POST 요청
            restTemplate.postForEntity(elkUrl, request, String.class);
            
            log.debug("로그를 ELK 스택으로 전송 완료: {}", elkUrl);
            
        } catch (Exception e) {
            log.error("ELK 스택으로 로그 전송 실패: {}", logEntry.getMessage(), e);
            throw new RuntimeException("ELK 스택 로그 전송 실패", e);
        }
    }

    /**
     * 🔥 로그 수집 통계 업데이트
     * @param logEntry 수집된 로그 엔트리
     */
    private void updateCollectionStats(LogEntry logEntry) {
        // 🔥 로그 레벨별 카운터 업데이트
        String level = logEntry.getLevel() != null ? logEntry.getLevel().toUpperCase() : "UNKNOWN";
        logLevelCounters.computeIfAbsent(level, k -> new AtomicLong(0)).incrementAndGet();
        
        // 🔥 전체 로그 수 증가
        totalLogsCollected.incrementAndGet();
        
        // 🔥 마지막 수집 시간 업데이트
        lastCollectionTime.set(System.currentTimeMillis());
    }

    /**
     * 🔥 로컬 로그 출력 (ELK 전송 실패 시)
     * @param logEntry 원본 로그 엔트리
     * @param exception 발생한 예외
     */
    private void logLocal(LogEntry logEntry, Exception exception) {
        String level = logEntry.getLevel() != null ? logEntry.getLevel().toUpperCase() : "INFO";
        
        switch (level) {
            case "ERROR":
                log.error("로컬 로그 출력 - {}: {}", logEntry.getLocation(), logEntry.getMessage(), exception);
                break;
            case "WARN":
                log.warn("로컬 로그 출력 - {}: {}", logEntry.getLocation(), logEntry.getMessage());
                break;
            case "DEBUG":
                log.debug("로컬 로그 출력 - {}: {}", logEntry.getLocation(), logEntry.getMessage());
                break;
            default:
                log.info("로컬 로그 출력 - {}: {}", logEntry.getLocation(), logEntry.getMessage());
                break;
        }
    }

    /**
     * 🔥 로그 수집 서비스 상태 확인
     * @return 서비스가 정상 동작 중인지 여부
     */
    public boolean isServiceHealthy() {
        try {
            if (!elkEnabled) {
                log.warn("ELK 스택이 비활성화되어 있습니다");
                return true; // ELK가 비활성화되어도 서비스는 정상
            }

            // 🔥 ELK 스택 연결 상태 확인
            String elkUrl = String.format("http://%s:%d%s", logstashHost, logstashPort, logstashEndpoint);
            restTemplate.getForEntity(elkUrl, String.class);
            
            return true;
            
        } catch (Exception e) {
            log.error("로그 수집 서비스 상태 확인 실패", e);
            return false;
        }
    }

    /**
     * 🔥 로그 수집 통계 정보 조회
     * @return 수집 통계 정보
     */
    public Map<String, Object> getCollectionStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        // 🔥 로그 레벨별 통계
        Map<String, Long> levelStats = new ConcurrentHashMap<>();
        logLevelCounters.forEach((level, counter) -> levelStats.put(level, counter.get()));
        stats.put("logLevelCounts", levelStats);
        
        // 🔥 전체 통계
        stats.put("totalLogsCollected", totalLogsCollected.get());
        stats.put("lastCollectionTime", lastCollectionTime.get());
        stats.put("elkEnabled", elkEnabled);
        stats.put("elkEndpoint", String.format("http://%s:%d%s", logstashHost, logstashPort, logstashEndpoint));
        
        return stats;
    }

    /**
     * 🔥 로그 수집 통계 초기화
     */
    public void resetCollectionStats() {
        logLevelCounters.clear();
        totalLogsCollected.set(0);
        lastCollectionTime.set(System.currentTimeMillis());
        
        log.info("로그 수집 통계가 초기화되었습니다");
    }
}
