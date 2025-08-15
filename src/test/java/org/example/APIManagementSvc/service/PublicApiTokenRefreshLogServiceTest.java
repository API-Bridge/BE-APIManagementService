package org.example.APIManagementSvc.service;

import org.example.APIManagementSvc.domain.Entity.PublicApiTokenRefreshLog;
import org.example.APIManagementSvc.domain.enums.RefreshStatus;
import org.example.APIManagementSvc.repository.PublicApiTokenRefreshLogRepository;
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
 * PublicApiTokenRefreshLogService TDD 테스트
 */
@ExtendWith(MockitoExtension.class)
class PublicApiTokenRefreshLogServiceTest {

    @Mock
    private PublicApiTokenRefreshLogRepository publicApiTokenRefreshLogRepository;

    @InjectMocks
    private PublicApiTokenRefreshLogService publicApiTokenRefreshLogService;

    private PublicApiTokenRefreshLog testLog;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
        testLog = PublicApiTokenRefreshLog.builder()
                .logId(1L)
                .providerName("test-provider")
                .refreshStatus(RefreshStatus.SUCCESS)
                .refreshTime(testTime)
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("성공적인 Token Refresh 로그를 성공적으로 기록할 수 있어야 한다")
    void shouldRecordSuccessfulRefresh() {
        // Given
        String providerName = "test-provider";

        when(publicApiTokenRefreshLogRepository.save(any(PublicApiTokenRefreshLog.class)))
                .thenReturn(testLog);

        // When
        PublicApiTokenRefreshLog result = publicApiTokenRefreshLogService.recordSuccessfulRefresh(providerName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProviderName()).isEqualTo(providerName);
        assertThat(result.getRefreshStatus()).isEqualTo(RefreshStatus.SUCCESS);
        assertThat(result.getDeleted()).isFalse();

        verify(publicApiTokenRefreshLogRepository, times(1)).save(any(PublicApiTokenRefreshLog.class));
    }

    @Test
    @DisplayName("실패한 Token Refresh 로그를 성공적으로 기록할 수 있어야 한다")
    void shouldRecordFailedRefresh() {
        // Given
        String providerName = "test-provider";
        String errorMessage = "Token refresh failed";

        PublicApiTokenRefreshLog failedLog = testLog.toBuilder()
                .refreshStatus(RefreshStatus.FAILED)
                .errorMessage(errorMessage)
                .build();

        when(publicApiTokenRefreshLogRepository.save(any(PublicApiTokenRefreshLog.class)))
                .thenReturn(failedLog);

        // When
        PublicApiTokenRefreshLog result = publicApiTokenRefreshLogService.recordFailedRefresh(
                providerName, errorMessage);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProviderName()).isEqualTo(providerName);
        assertThat(result.getRefreshStatus()).isEqualTo(RefreshStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo(errorMessage);

        verify(publicApiTokenRefreshLogRepository, times(1)).save(any(PublicApiTokenRefreshLog.class));
    }

    @Test
    @DisplayName("진행 중인 Token Refresh 로그를 성공적으로 기록할 수 있어야 한다")
    void shouldRecordInProgressRefresh() {
        // Given
        String providerName = "test-provider";

        PublicApiTokenRefreshLog inProgressLog = testLog.toBuilder()
                .refreshStatus(RefreshStatus.IN_PROGRESS)
                .build();

        when(publicApiTokenRefreshLogRepository.save(any(PublicApiTokenRefreshLog.class)))
                .thenReturn(inProgressLog);

        // When
        PublicApiTokenRefreshLog result = publicApiTokenRefreshLogService.recordInProgressRefresh(providerName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProviderName()).isEqualTo(providerName);
        assertThat(result.getRefreshStatus()).isEqualTo(RefreshStatus.IN_PROGRESS);

        verify(publicApiTokenRefreshLogRepository, times(1)).save(any(PublicApiTokenRefreshLog.class));
    }

    @Test
    @DisplayName("만료된 Token 로그를 성공적으로 기록할 수 있어야 한다")
    void shouldRecordExpiredToken() {
        // Given
        String providerName = "test-provider";

        PublicApiTokenRefreshLog expiredLog = testLog.toBuilder()
                .refreshStatus(RefreshStatus.EXPIRED)
                .build();

        when(publicApiTokenRefreshLogRepository.save(any(PublicApiTokenRefreshLog.class)))
                .thenReturn(expiredLog);

        // When
        PublicApiTokenRefreshLog result = publicApiTokenRefreshLogService.recordExpiredToken(providerName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProviderName()).isEqualTo(providerName);
        assertThat(result.getRefreshStatus()).isEqualTo(RefreshStatus.EXPIRED);

        verify(publicApiTokenRefreshLogRepository, times(1)).save(any(PublicApiTokenRefreshLog.class));
    }

    @Test
    @DisplayName("수동 갱신 로그를 성공적으로 기록할 수 있어야 한다")
    void shouldRecordManualRefresh() {
        // Given
        String providerName = "test-provider";

        PublicApiTokenRefreshLog manualLog = testLog.toBuilder()
                .refreshStatus(RefreshStatus.MANUAL_REFRESH)
                .build();

        when(publicApiTokenRefreshLogRepository.save(any(PublicApiTokenRefreshLog.class)))
                .thenReturn(manualLog);

        // When
        PublicApiTokenRefreshLog result = publicApiTokenRefreshLogService.recordManualRefresh(providerName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProviderName()).isEqualTo(providerName);
        assertThat(result.getRefreshStatus()).isEqualTo(RefreshStatus.MANUAL_REFRESH);

        verify(publicApiTokenRefreshLogRepository, times(1)).save(any(PublicApiTokenRefreshLog.class));
    }

    @Test
    @DisplayName("ID로 로그를 조회할 수 있어야 한다")
    void shouldGetLogById() {
        // Given
        Long logId = 1L;
        when(publicApiTokenRefreshLogRepository.findByLogIdAndDeletedFalse(logId))
                .thenReturn(Optional.of(testLog));

        // When
        Optional<PublicApiTokenRefreshLog> result = publicApiTokenRefreshLogService.getLogById(logId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getLogId()).isEqualTo(logId);
        verify(publicApiTokenRefreshLogRepository, times(1)).findByLogIdAndDeletedFalse(logId);
    }

    @Test
    @DisplayName("제공자 이름으로 로그를 조회할 수 있어야 한다")
    void shouldGetLogsByProvider() {
        // Given
        String providerName = "test-provider";
        List<PublicApiTokenRefreshLog> expectedLogs = List.of(testLog);
        when(publicApiTokenRefreshLogRepository.findByProviderNameAndDeletedFalse(providerName))
                .thenReturn(expectedLogs);

        // When
        List<PublicApiTokenRefreshLog> result = publicApiTokenRefreshLogService.getLogsByProvider(providerName);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProviderName()).isEqualTo(providerName);
        verify(publicApiTokenRefreshLogRepository, times(1)).findByProviderNameAndDeletedFalse(providerName);
    }

    @Test
    @DisplayName("Refresh 상태로 로그를 조회할 수 있어야 한다")
    void shouldGetLogsByRefreshStatus() {
        // Given
        RefreshStatus status = RefreshStatus.SUCCESS;
        List<PublicApiTokenRefreshLog> expectedLogs = List.of(testLog);
        when(publicApiTokenRefreshLogRepository.findByRefreshStatusAndDeletedFalse(status))
                .thenReturn(expectedLogs);

        // When
        List<PublicApiTokenRefreshLog> result = publicApiTokenRefreshLogService.getLogsByRefreshStatus(status);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRefreshStatus()).isEqualTo(status);
        verify(publicApiTokenRefreshLogRepository, times(1)).findByRefreshStatusAndDeletedFalse(status);
    }

    @Test
    @DisplayName("페이징으로 로그를 조회할 수 있어야 한다")
    void shouldGetLogsWithPaging() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<PublicApiTokenRefreshLog> expectedPage = new PageImpl<>(List.of(testLog));
        when(publicApiTokenRefreshLogRepository.findByDeletedFalse(pageable))
                .thenReturn(expectedPage);

        // When
        Page<PublicApiTokenRefreshLog> result = publicApiTokenRefreshLogService.getLogsWithPaging(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(publicApiTokenRefreshLogRepository, times(1)).findByDeletedFalse(pageable);
    }

    @Test
    @DisplayName("Refresh 상태별 통계를 조회할 수 있어야 한다")
    void shouldGetRefreshStatusStatistics() {
        // Given
        Object[] stat1 = {RefreshStatus.SUCCESS, 10L};
        Object[] stat2 = {RefreshStatus.FAILED, 2L};
        List<Object[]> expectedStats = List.of(stat1, stat2);
        
        when(publicApiTokenRefreshLogRepository.findRefreshStatusStatistics())
                .thenReturn(expectedStats);

        // When
        Map<RefreshStatus, Long> result = publicApiTokenRefreshLogService.getRefreshStatusStatistics();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(RefreshStatus.SUCCESS)).isEqualTo(10L);
        assertThat(result.get(RefreshStatus.FAILED)).isEqualTo(2L);
        verify(publicApiTokenRefreshLogRepository, times(1)).findRefreshStatusStatistics();
    }

    @Test
    @DisplayName("제공자별 통계를 조회할 수 있어야 한다")
    void shouldGetProviderStatistics() {
        // Given
        String providerName = "test-provider";
        when(publicApiTokenRefreshLogRepository.countByProviderNameAndDeletedFalse(providerName))
                .thenReturn(15L);
        when(publicApiTokenRefreshLogRepository.countByProviderNameAndRefreshStatusAndDeletedFalse(providerName, RefreshStatus.SUCCESS))
                .thenReturn(12L);
        when(publicApiTokenRefreshLogRepository.countByProviderNameAndRefreshStatusAndDeletedFalse(providerName, RefreshStatus.FAILED))
                .thenReturn(2L);
        when(publicApiTokenRefreshLogRepository.countByProviderNameAndRefreshStatusAndDeletedFalse(providerName, RefreshStatus.IN_PROGRESS))
                .thenReturn(1L);
        when(publicApiTokenRefreshLogRepository.countByProviderNameAndRefreshStatusAndDeletedFalse(providerName, RefreshStatus.EXPIRED))
                .thenReturn(0L);
        when(publicApiTokenRefreshLogRepository.countByProviderNameAndRefreshStatusAndDeletedFalse(providerName, RefreshStatus.MANUAL_REFRESH))
                .thenReturn(0L);

        // When
        Map<String, Object> result = publicApiTokenRefreshLogService.getProviderStatistics(providerName);

        // Then
        assertThat(result).containsKeys("providerName", "totalRefreshes", "successfulRefreshes", "successRate");
        assertThat(result.get("providerName")).isEqualTo(providerName);
        assertThat(result.get("totalRefreshes")).isEqualTo(15L);
        assertThat(result.get("successfulRefreshes")).isEqualTo(12L);
        assertThat(result.get("successRate")).isEqualTo(0.8);
    }

    @Test
    @DisplayName("전체 통계를 조회할 수 있어야 한다")
    void shouldGetOverallStatistics() {
        // Given
        when(publicApiTokenRefreshLogRepository.countByDeletedFalse())
                .thenReturn(100L);
        when(publicApiTokenRefreshLogRepository.countByRefreshStatusAndDeletedFalse(RefreshStatus.SUCCESS))
                .thenReturn(80L);
        when(publicApiTokenRefreshLogRepository.countByRefreshStatusAndDeletedFalse(RefreshStatus.FAILED))
                .thenReturn(15L);
        when(publicApiTokenRefreshLogRepository.countByRefreshStatusAndDeletedFalse(RefreshStatus.IN_PROGRESS))
                .thenReturn(3L);
        when(publicApiTokenRefreshLogRepository.countByRefreshStatusAndDeletedFalse(RefreshStatus.EXPIRED))
                .thenReturn(1L);
        when(publicApiTokenRefreshLogRepository.countByRefreshStatusAndDeletedFalse(RefreshStatus.MANUAL_REFRESH))
                .thenReturn(1L);

        // When
        Map<String, Object> result = publicApiTokenRefreshLogService.getOverallStatistics();

        // Then
        assertThat(result).containsKeys("totalRefreshes", "successfulRefreshes", "successRate");
        assertThat(result.get("totalRefreshes")).isEqualTo(100L);
        assertThat(result.get("successfulRefreshes")).isEqualTo(80L);
        assertThat(result.get("successRate")).isEqualTo(0.8);
    }

    @Test
    @DisplayName("로그를 소프트 삭제할 수 있어야 한다")
    void shouldSoftDeleteLog() {
        // Given
        Long logId = 1L;
        when(publicApiTokenRefreshLogRepository.findById(logId))
                .thenReturn(Optional.of(testLog));
        when(publicApiTokenRefreshLogRepository.save(any(PublicApiTokenRefreshLog.class)))
                .thenReturn(testLog);

        // When
        publicApiTokenRefreshLogService.deleteLog(logId);

        // Then
        verify(publicApiTokenRefreshLogRepository, times(1)).findById(logId);
        verify(publicApiTokenRefreshLogRepository, times(1)).save(any(PublicApiTokenRefreshLog.class));
        assertThat(testLog.getDeleted()).isTrue();
    }

    @Test
    @DisplayName("로그를 하드 삭제할 수 있어야 한다")
    void shouldHardDeleteLog() {
        // Given
        Long logId = 1L;

        // When
        publicApiTokenRefreshLogService.hardDeleteLog(logId);

        // Then
        verify(publicApiTokenRefreshLogRepository, times(1)).deleteById(logId);
    }

    @Test
    @DisplayName("최근 실패한 제공자들을 조회할 수 있어야 한다")
    void shouldGetRecentlyFailedProviders() {
        // Given
        int hours = 24;
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        
        PublicApiTokenRefreshLog failedLog1 = testLog.toBuilder()
                .providerName("provider1")
                .refreshStatus(RefreshStatus.FAILED)
                .build();
        PublicApiTokenRefreshLog failedLog2 = testLog.toBuilder()
                .providerName("provider2")
                .refreshStatus(RefreshStatus.FAILED)
                .build();
        
        List<PublicApiTokenRefreshLog> failedLogs = List.of(failedLog1, failedLog2);
        
        when(publicApiTokenRefreshLogRepository.findByRefreshStatusAndTimeRangeAndDeletedFalse(
                eq(RefreshStatus.FAILED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(failedLogs);

        // When
        List<String> result = publicApiTokenRefreshLogService.getRecentlyFailedProviders(hours);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains("provider1", "provider2");
    }
}
