package org.example.APIManagementSvc.service;

import org.example.APIManagementSvc.domain.Entity.PublicApiRateLimitLog;
import org.example.APIManagementSvc.domain.enums.RateLimitStatus;
import org.example.APIManagementSvc.repository.PublicApiRateLimitLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PublicApiRateLimitLogService TDD 테스트
 */
@ExtendWith(MockitoExtension.class)
class PublicApiRateLimitLogServiceTest {

    @Mock
    private PublicApiRateLimitLogRepository publicApiRateLimitLogRepository;

    @InjectMocks
    private PublicApiRateLimitLogService publicApiRateLimitLogService;

    private PublicApiRateLimitLog testLog;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
        testLog = PublicApiRateLimitLog.builder()
                .logId(1L)
                .apiName("test-api")
                .userId("test-user")
                .ipAddress("192.168.1.1")
                .requestTime(testTime)
                .rateLimitStatus(RateLimitStatus.ALLOWED)
                .currentUsageCount(5)
                .maxAllowedCount(100)
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("Rate Limit 허용 로그를 성공적으로 기록할 수 있어야 한다")
    void shouldRecordAllowedRequest() {
        // Given
        String apiName = "test-api";
        String userId = "test-user";
        String ipAddress = "192.168.1.1";
        Integer currentUsage = 5;
        Integer maxAllowed = 100;

        when(publicApiRateLimitLogRepository.save(any(PublicApiRateLimitLog.class)))
                .thenReturn(testLog);

        // When
        PublicApiRateLimitLog result = publicApiRateLimitLogService.recordAllowedRequest(
                apiName, userId, ipAddress, currentUsage, maxAllowed);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getApiName()).isEqualTo(apiName);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getIpAddress()).isEqualTo(ipAddress);
        assertThat(result.getRateLimitStatus()).isEqualTo(RateLimitStatus.ALLOWED);
        assertThat(result.getCurrentUsageCount()).isEqualTo(currentUsage);
        assertThat(result.getMaxAllowedCount()).isEqualTo(maxAllowed);
        assertThat(result.getDeleted()).isFalse();

        verify(publicApiRateLimitLogRepository, times(1)).save(any(PublicApiRateLimitLog.class));
    }

    @Test
    @DisplayName("Rate Limit 경고 로그를 성공적으로 기록할 수 있어야 한다")
    void shouldRecordWarningRequest() {
        // Given
        String apiName = "test-api";
        String userId = "test-user";
        String ipAddress = "192.168.1.1";
        Integer currentUsage = 90;
        Integer maxAllowed = 100;

        PublicApiRateLimitLog warningLog = testLog.toBuilder()
                .rateLimitStatus(RateLimitStatus.WARNING)
                .currentUsageCount(currentUsage)
                .build();

        when(publicApiRateLimitLogRepository.save(any(PublicApiRateLimitLog.class)))
                .thenReturn(warningLog);

        // When
        PublicApiRateLimitLog result = publicApiRateLimitLogService.recordWarningRequest(
                apiName, userId, ipAddress, currentUsage, maxAllowed);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRateLimitStatus()).isEqualTo(RateLimitStatus.WARNING);
        assertThat(result.getCurrentUsageCount()).isEqualTo(currentUsage);
        assertThat(result.getMaxAllowedCount()).isEqualTo(maxAllowed);

        verify(publicApiRateLimitLogRepository, times(1)).save(any(PublicApiRateLimitLog.class));
    }

    @Test
    @DisplayName("Rate Limit 초과 로그를 성공적으로 기록할 수 있어야 한다")
    void shouldRecordExceededRequest() {
        // Given
        String apiName = "test-api";
        String userId = "test-user";
        String ipAddress = "192.168.1.1";
        Integer currentUsage = 110;
        Integer maxAllowed = 100;
        LocalDateTime resetTime = testTime.plusHours(1);
        String errorMessage = "Rate limit exceeded";

        PublicApiRateLimitLog exceededLog = testLog.toBuilder()
                .rateLimitStatus(RateLimitStatus.EXCEEDED)
                .currentUsageCount(currentUsage)
                .resetTime(resetTime)
                .errorMessage(errorMessage)
                .build();

        when(publicApiRateLimitLogRepository.save(any(PublicApiRateLimitLog.class)))
                .thenReturn(exceededLog);

        // When
        PublicApiRateLimitLog result = publicApiRateLimitLogService.recordExceededRequest(
                apiName, userId, ipAddress, currentUsage, maxAllowed, resetTime, errorMessage);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRateLimitStatus()).isEqualTo(RateLimitStatus.EXCEEDED);
        assertThat(result.getCurrentUsageCount()).isEqualTo(currentUsage);
        assertThat(result.getResetTime()).isEqualTo(resetTime);
        assertThat(result.getErrorMessage()).isEqualTo(errorMessage);

        verify(publicApiRateLimitLogRepository, times(1)).save(any(PublicApiRateLimitLog.class));
    }

    @Test
    @DisplayName("Rate Limit 차단 로그를 성공적으로 기록할 수 있어야 한다")
    void shouldRecordBlockedRequest() {
        // Given
        String apiName = "test-api";
        String userId = "test-user";
        String ipAddress = "192.168.1.1";
        String errorMessage = "IP blocked due to suspicious activity";

        PublicApiRateLimitLog blockedLog = testLog.toBuilder()
                .rateLimitStatus(RateLimitStatus.BLOCKED)
                .errorMessage(errorMessage)
                .build();

        when(publicApiRateLimitLogRepository.save(any(PublicApiRateLimitLog.class)))
                .thenReturn(blockedLog);

        // When
        PublicApiRateLimitLog result = publicApiRateLimitLogService.recordBlockedRequest(
                apiName, userId, ipAddress, errorMessage);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRateLimitStatus()).isEqualTo(RateLimitStatus.BLOCKED);
        assertThat(result.getErrorMessage()).isEqualTo(errorMessage);

        verify(publicApiRateLimitLogRepository, times(1)).save(any(PublicApiRateLimitLog.class));
    }

    @Test
    @DisplayName("Rate Limit 리셋 로그를 성공적으로 기록할 수 있어야 한다")
    void shouldRecordResetLog() {
        // Given
        String apiName = "test-api";
        String userId = "test-user";
        String ipAddress = "192.168.1.1";

        PublicApiRateLimitLog resetLog = testLog.toBuilder()
                .rateLimitStatus(RateLimitStatus.RESET)
                .build();

        when(publicApiRateLimitLogRepository.save(any(PublicApiRateLimitLog.class)))
                .thenReturn(resetLog);

        // When
        PublicApiRateLimitLog result = publicApiRateLimitLogService.recordResetLog(
                apiName, userId, ipAddress);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRateLimitStatus()).isEqualTo(RateLimitStatus.RESET);

        verify(publicApiRateLimitLogRepository, times(1)).save(any(PublicApiRateLimitLog.class));
    }

    @Test
    @DisplayName("ID로 로그를 조회할 수 있어야 한다")
    void shouldGetLogById() {
        // Given
        Long logId = 1L;
        when(publicApiRateLimitLogRepository.findByLogIdAndDeletedFalse(logId))
                .thenReturn(Optional.of(testLog));

        // When
        Optional<PublicApiRateLimitLog> result = publicApiRateLimitLogService.getLogById(logId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getLogId()).isEqualTo(logId);
        verify(publicApiRateLimitLogRepository, times(1)).findByLogIdAndDeletedFalse(logId);
    }

    @Test
    @DisplayName("API 이름으로 로그를 조회할 수 있어야 한다")
    void shouldGetLogsByApiName() {
        // Given
        String apiName = "test-api";
        List<PublicApiRateLimitLog> expectedLogs = List.of(testLog);
        when(publicApiRateLimitLogRepository.findByApiNameAndDeletedFalse(apiName))
                .thenReturn(expectedLogs);

        // When
        List<PublicApiRateLimitLog> result = publicApiRateLimitLogService.getLogsByApiName(apiName);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApiName()).isEqualTo(apiName);
        verify(publicApiRateLimitLogRepository, times(1)).findByApiNameAndDeletedFalse(apiName);
    }

    @Test
    @DisplayName("사용자 ID로 로그를 조회할 수 있어야 한다")
    void shouldGetLogsByUserId() {
        // Given
        String userId = "test-user";
        List<PublicApiRateLimitLog> expectedLogs = List.of(testLog);
        when(publicApiRateLimitLogRepository.findByUserIdAndDeletedFalse(userId))
                .thenReturn(expectedLogs);

        // When
        List<PublicApiRateLimitLog> result = publicApiRateLimitLogService.getLogsByUserId(userId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
        verify(publicApiRateLimitLogRepository, times(1)).findByUserIdAndDeletedFalse(userId);
    }

    @Test
    @DisplayName("페이징으로 로그를 조회할 수 있어야 한다")
    void shouldGetLogsWithPaging() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<PublicApiRateLimitLog> expectedPage = new PageImpl<>(List.of(testLog));
        when(publicApiRateLimitLogRepository.findByDeletedFalse(pageable))
                .thenReturn(expectedPage);

        // When
        Page<PublicApiRateLimitLog> result = publicApiRateLimitLogService.getLogsWithPaging(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(publicApiRateLimitLogRepository, times(1)).findByDeletedFalse(pageable);
    }

    @Test
    @DisplayName("Rate Limit 상태별 통계를 조회할 수 있어야 한다")
    void shouldGetRateLimitStatusStatistics() {
        // Given
        Object[] stat1 = {RateLimitStatus.ALLOWED, 10L};
        Object[] stat2 = {RateLimitStatus.EXCEEDED, 2L};
        List<Object[]> expectedStats = List.of(stat1, stat2);
        
        when(publicApiRateLimitLogRepository.findRateLimitStatusStatistics())
                .thenReturn(expectedStats);

        // When
        Map<RateLimitStatus, Long> result = publicApiRateLimitLogService.getRateLimitStatusStatistics();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(RateLimitStatus.ALLOWED)).isEqualTo(10L);
        assertThat(result.get(RateLimitStatus.EXCEEDED)).isEqualTo(2L);
        verify(publicApiRateLimitLogRepository, times(1)).findRateLimitStatusStatistics();
    }

    @Test
    @DisplayName("사용자별 통계를 조회할 수 있어야 한다")
    void shouldGetUserStatistics() {
        // Given
        String userId = "test-user";
        when(publicApiRateLimitLogRepository.countByUserIdAndDeletedFalse(userId))
                .thenReturn(10L);
        when(publicApiRateLimitLogRepository.countByUserIdAndRateLimitStatusAndDeletedFalse(userId, RateLimitStatus.ALLOWED))
                .thenReturn(8L);
        when(publicApiRateLimitLogRepository.countByUserIdAndRateLimitStatusAndDeletedFalse(userId, RateLimitStatus.WARNING))
                .thenReturn(1L);
        when(publicApiRateLimitLogRepository.countByUserIdAndRateLimitStatusAndDeletedFalse(userId, RateLimitStatus.EXCEEDED))
                .thenReturn(1L);
        when(publicApiRateLimitLogRepository.countByUserIdAndRateLimitStatusAndDeletedFalse(userId, RateLimitStatus.BLOCKED))
                .thenReturn(0L);

        // When
        Map<String, Object> result = publicApiRateLimitLogService.getUserStatistics(userId);

        // Then
        assertThat(result).containsKeys("userId", "totalRequests", "allowedRequests", "successRate");
        assertThat(result.get("userId")).isEqualTo(userId);
        assertThat(result.get("totalRequests")).isEqualTo(10L);
        assertThat(result.get("allowedRequests")).isEqualTo(8L);
        assertThat(result.get("successRate")).isEqualTo(0.8);
    }

    @Test
    @DisplayName("API별 통계를 조회할 수 있어야 한다")
    void shouldGetApiStatistics() {
        // Given
        String apiName = "test-api";
        when(publicApiRateLimitLogRepository.countByApiNameAndDeletedFalse(apiName))
                .thenReturn(15L);
        when(publicApiRateLimitLogRepository.countByApiNameAndRateLimitStatusAndDeletedFalse(apiName, RateLimitStatus.ALLOWED))
                .thenReturn(12L);
        when(publicApiRateLimitLogRepository.countByApiNameAndRateLimitStatusAndDeletedFalse(apiName, RateLimitStatus.WARNING))
                .thenReturn(2L);
        when(publicApiRateLimitLogRepository.countByApiNameAndRateLimitStatusAndDeletedFalse(apiName, RateLimitStatus.EXCEEDED))
                .thenReturn(1L);
        when(publicApiRateLimitLogRepository.countByApiNameAndRateLimitStatusAndDeletedFalse(apiName, RateLimitStatus.BLOCKED))
                .thenReturn(0L);

        // When
        Map<String, Object> result = publicApiRateLimitLogService.getApiStatistics(apiName);

        // Then
        assertThat(result).containsKeys("apiName", "totalRequests", "allowedRequests", "successRate");
        assertThat(result.get("apiName")).isEqualTo(apiName);
        assertThat(result.get("totalRequests")).isEqualTo(15L);
        assertThat(result.get("allowedRequests")).isEqualTo(12L);
        assertThat(result.get("successRate")).isEqualTo(0.8);
    }

    @Test
    @DisplayName("전체 통계를 조회할 수 있어야 한다")
    void shouldGetOverallStatistics() {
        // Given
        when(publicApiRateLimitLogRepository.countByDeletedFalse())
                .thenReturn(100L);
        when(publicApiRateLimitLogRepository.countByRateLimitStatusAndDeletedFalse(RateLimitStatus.ALLOWED))
                .thenReturn(80L);
        when(publicApiRateLimitLogRepository.countByRateLimitStatusAndDeletedFalse(RateLimitStatus.WARNING))
                .thenReturn(15L);
        when(publicApiRateLimitLogRepository.countByRateLimitStatusAndDeletedFalse(RateLimitStatus.EXCEEDED))
                .thenReturn(3L);
        when(publicApiRateLimitLogRepository.countByRateLimitStatusAndDeletedFalse(RateLimitStatus.BLOCKED))
                .thenReturn(2L);

        // When
        Map<String, Object> result = publicApiRateLimitLogService.getOverallStatistics();

        // Then
        assertThat(result).containsKeys("totalRequests", "allowedRequests", "successRate");
        assertThat(result.get("totalRequests")).isEqualTo(100L);
        assertThat(result.get("allowedRequests")).isEqualTo(80L);
        assertThat(result.get("successRate")).isEqualTo(0.8);
    }

    @Test
    @DisplayName("로그를 소프트 삭제할 수 있어야 한다")
    void shouldSoftDeleteLog() {
        // Given
        Long logId = 1L;
        when(publicApiRateLimitLogRepository.findById(logId))
                .thenReturn(Optional.of(testLog));
        when(publicApiRateLimitLogRepository.save(any(PublicApiRateLimitLog.class)))
                .thenReturn(testLog);

        // When
        publicApiRateLimitLogService.deleteLog(logId);

        // Then
        verify(publicApiRateLimitLogRepository, times(1)).findById(logId);
        verify(publicApiRateLimitLogRepository, times(1)).save(any(PublicApiRateLimitLog.class));
        assertThat(testLog.getDeleted()).isTrue();
    }

    @Test
    @DisplayName("로그를 하드 삭제할 수 있어야 한다")
    void shouldHardDeleteLog() {
        // Given
        Long logId = 1L;

        // When
        publicApiRateLimitLogService.hardDeleteLog(logId);

        // Then
        verify(publicApiRateLimitLogRepository, times(1)).deleteById(logId);
    }
}
