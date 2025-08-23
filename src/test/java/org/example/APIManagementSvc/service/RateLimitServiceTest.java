package org.example.APIManagementSvc.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService 테스트")
class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
        // Clear the cache for each test to ensure isolation
        // This is important because ConcurrentHashMap retains state across test methods
        try {
            java.lang.reflect.Field cacheField = RateLimitService.class.getDeclaredField("rateLimitCache");
            cacheField.setAccessible(true);
            Map<?, ?> cache = (Map<?, ?>) cacheField.get(rateLimitService);
            cache.clear();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("새로운 키에 대한 Rate Limiting - 허용")
    void isAllowed_NewKey_ShouldAllow() {
        String key = "test-key-001";
        int limit = 5;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        boolean result = rateLimitService.isAllowed(key, limit, timeUnit);

        assertThat(result).isTrue();
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);
        assertThat(status.getCurrentCount()).isEqualTo(1); // First call increments to 1
        assertThat(status.getLimit()).isEqualTo(limit);
        assertThat(status.getRemaining()).isEqualTo(limit - 1);
        assertThat(status.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("기존 키에 대한 Rate Limiting - 한도 내에서 허용")
    void isAllowed_ExistingKey_WithinLimit_ShouldAllow() {
        String key = "test-key-002";
        int limit = 3;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        boolean result1 = rateLimitService.isAllowed(key, limit, timeUnit); // count = 1
        boolean result2 = rateLimitService.isAllowed(key, limit, timeUnit); // count = 2
        
        assertThat(result1).isTrue();
        assertThat(result2).isTrue();

        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);
        assertThat(status.getCurrentCount()).isEqualTo(2);
        assertThat(status.getLimit()).isEqualTo(limit);
        assertThat(status.getRemaining()).isEqualTo(limit - 2);
        assertThat(status.isAllowed()).isTrue(); // Still allowed if current < limit
        
        boolean result3 = rateLimitService.isAllowed(key, limit, timeUnit); // count = 3
        assertThat(result3).isTrue();

        status = rateLimitService.getRateLimitStatus(key);
        assertThat(status.getCurrentCount()).isEqualTo(3);
        assertThat(status.getLimit()).isEqualTo(limit);
        assertThat(status.getRemaining()).isEqualTo(0);
        assertThat(status.isAllowed()).isFalse(); // At limit, so not allowed for next call
    }

    @Test
    @DisplayName("기존 키에 대한 Rate Limiting - 한도 초과 시 거부")
    void isAllowed_ExistingKey_ExceedLimit_ShouldDeny() {
        String key = "test-key-003";
        int limit = 2;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        boolean result1 = rateLimitService.isAllowed(key, limit, timeUnit); // count = 1
        boolean result2 = rateLimitService.isAllowed(key, limit, timeUnit); // count = 2
        boolean result3 = rateLimitService.isAllowed(key, limit, timeUnit); // Denied (count is already 2)

        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
        assertThat(result3).isFalse(); 
        
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);
        assertThat(status.getCurrentCount()).isEqualTo(2); // Count remains at limit
        assertThat(status.getLimit()).isEqualTo(limit);
        assertThat(status.getRemaining()).isEqualTo(0);
        assertThat(status.isAllowed()).isFalse();
    }

    @Test
    @DisplayName("초 단위 Rate Limiting - 시간 경계에서 리셋")
    void isAllowed_Seconds_ShouldResetAtBoundary() throws InterruptedException {
        String key = "test-key-seconds";
        int limit = 1; // Set a low limit to easily hit
        TimeUnit timeUnit = TimeUnit.SECONDS;

        // First second: hit limit
        boolean result1 = rateLimitService.isAllowed(key, limit, timeUnit); // count = 1
        boolean result2 = rateLimitService.isAllowed(key, limit, timeUnit); // denied

        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
        assertThat(rateLimitService.getRateLimitStatus(key).getCurrentCount()).isEqualTo(1);

        // Wait for time to pass into the next second
        Thread.sleep(1001); // Sleep a bit more than 1 second to ensure new second

        // Next second: should be allowed again as counter resets
        boolean result3 = rateLimitService.isAllowed(key, limit, timeUnit); // count = 1 (after reset)

        assertThat(result3).isTrue();
        assertThat(rateLimitService.getRateLimitStatus(key).getCurrentCount()).isEqualTo(1);
    }

    // This test doesn't truly simulate minute reset because it doesn't wait for a minute.
    // Given the current implementation, it's better to verify initial behavior.
    @Test
    @DisplayName("분 단위 Rate Limiting")
    void isAllowed_Minutes_ShouldWorkCorrectly() {
        String key = "test-key-minutes";
        int limit = 5;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        boolean result = rateLimitService.isAllowed(key, limit, timeUnit);

        assertThat(result).isTrue();
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);
        assertThat(status.getCurrentCount()).isEqualTo(1);
        assertThat(status.getLimit()).isEqualTo(limit);
        assertThat(status.getRemaining()).isEqualTo(limit - 1);
        assertThat(status.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("시간 단위 Rate Limiting")
    void isAllowed_Hours_ShouldWorkCorrectly() {
        String key = "test-key-hours";
        int limit = 10;
        TimeUnit timeUnit = TimeUnit.HOURS;

        boolean result = rateLimitService.isAllowed(key, limit, timeUnit);

        assertThat(result).isTrue();
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);
        assertThat(status.getCurrentCount()).isEqualTo(1);
        assertThat(status.getLimit()).isEqualTo(limit);
        assertThat(status.getRemaining()).isEqualTo(limit - 1);
        assertThat(status.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("일 단위 Rate Limiting")
    void isAllowed_Days_ShouldWorkCorrectly() {
        String key = "test-key-days";
        int limit = 100;
        TimeUnit timeUnit = TimeUnit.DAYS;

        boolean result = rateLimitService.isAllowed(key, limit, timeUnit);

        assertThat(result).isTrue();
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);
        assertThat(status.getCurrentCount()).isEqualTo(1);
        assertThat(status.getLimit()).isEqualTo(limit);
        assertThat(status.getRemaining()).isEqualTo(limit - 1);
        assertThat(status.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("여러 키에 대한 독립적인 Rate Limiting")
    void isAllowed_MultipleKeys_ShouldBeIndependent() {
        String key1 = "key-1";
        String key2 = "key-2";
        int limit = 1; // Small limit to easily test
        TimeUnit timeUnit = TimeUnit.MINUTES;

        // Key1 calls
        boolean result1_1 = rateLimitService.isAllowed(key1, limit, timeUnit); // key1 count = 1
        boolean result1_2 = rateLimitService.isAllowed(key1, limit, timeUnit); // key1 denied

        // Key2 calls
        boolean result2_1 = rateLimitService.isAllowed(key2, limit, timeUnit); // key2 count = 1
        boolean result2_2 = rateLimitService.isAllowed(key2, limit, timeUnit); // key2 denied

        assertThat(result1_1).isTrue();
        assertThat(result1_2).isFalse();
        
        assertThat(result2_1).isTrue();
        assertThat(result2_2).isFalse();
        
        RateLimitService.RateLimitStatus status1 = rateLimitService.getRateLimitStatus(key1);
        assertThat(status1.getCurrentCount()).isEqualTo(1);
        assertThat(status1.isAllowed()).isFalse(); // At limit
        
        RateLimitService.RateLimitStatus status2 = rateLimitService.getRateLimitStatus(key2);
        assertThat(status2.getCurrentCount()).isEqualTo(1);
        assertThat(status2.isAllowed()).isFalse(); // At limit
    }

    @Test
    @DisplayName("Rate Limit 상태 조회 - 존재하지 않는 키")
    void getRateLimitStatus_NonExistentKey_ShouldReturnDefaultStatus() {
        String key = "non-existent-key";
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);

        assertThat(status).isNotNull();
        assertThat(status.getCurrentCount()).isEqualTo(0);
        assertThat(status.getLimit()).isEqualTo(0);
        assertThat(status.getRemaining()).isEqualTo(0);
        assertThat(status.isAllowed()).isTrue(); // Default status is allowed (no limit has been set yet) 즉 아직 할당되지 않음
    }

    @Test
    @DisplayName("Rate Limit 상태 조회 - 존재하는 키")
    void getRateLimitStatus_ExistingKey_ShouldReturnCorrectStatus() {
        String key = "existing-key";
        int limit = 5;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        rateLimitService.isAllowed(key, limit, timeUnit); // count = 1
        rateLimitService.isAllowed(key, limit, timeUnit); // count = 2
        
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);

        assertThat(status).isNotNull();
        assertThat(status.getCurrentCount()).isEqualTo(2);
        assertThat(status.getLimit()).isEqualTo(limit);
        assertThat(status.getRemaining()).isEqualTo(limit - 2);
        assertThat(status.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("Rate Limit 상태 조회 - 한도에 도달한 키")
    void getRateLimitStatus_KeyAtLimit_ShouldReturnCorrectStatus() {
        String key = "limit-key";
        int limit = 3;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        rateLimitService.isAllowed(key, limit, timeUnit); // count = 1
        rateLimitService.isAllowed(key, limit, timeUnit); // count = 2
        rateLimitService.isAllowed(key, limit, timeUnit); // count = 3
        
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);

        assertThat(status).isNotNull();
        assertThat(status.getCurrentCount()).isEqualTo(3);
        assertThat(status.getLimit()).isEqualTo(limit);
        assertThat(status.getRemaining()).isEqualTo(0);
        assertThat(status.isAllowed()).isFalse(); // Not allowed for next call
    }

    @Test
    @DisplayName("캐시 정리 - 오래된 데이터 제거")
    void cleanupCache_ShouldRemoveOldData() {
        String key1 = "old-key";
        String key2 = "recent-key";
        int limit = 5;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        // Simulate an old entry by manually adding to the cache (via reflection)
        // This is a bit hacky but necessary to test cleanup of old data
        try {
            java.lang.reflect.Field cacheField = RateLimitService.class.getDeclaredField("rateLimitCache");
            cacheField.setAccessible(true);
            Map<String, Object> cache = (Map<String, Object>) cacheField.get(rateLimitService);

            // Create an old RateLimitInfo (e.g., 2 days ago)
            Class<?> rateLimitInfoClass = Class.forName("org.example.APIManagementSvc.service.RateLimitService$RateLimitInfo");
            java.lang.reflect.Constructor<?> constructor = rateLimitInfoClass.getDeclaredConstructor(LocalDateTime.class, int.class);
            constructor.setAccessible(true);
            Object oldInfo = constructor.newInstance(LocalDateTime.now().minusDays(2), 10);
            
            cache.put(key1, oldInfo); // Add an old entry
            rateLimitService.isAllowed(key2, limit, timeUnit); // Add a recent entry
            
            // Verify initial state
            assertThat(rateLimitService.getRateLimitStatus(key1).getCurrentCount()).isEqualTo(0); // Old entry not incremented by isAllowed
            assertThat(rateLimitService.getRateLimitStatus(key2).getCurrentCount()).isEqualTo(1);

            // Perform cleanup
            rateLimitService.cleanupCache();

            // Verify cleanup
            // Old key should be removed, recent key should remain
            RateLimitService.RateLimitStatus status1 = rateLimitService.getRateLimitStatus(key1);
            RateLimitService.RateLimitStatus status2 = rateLimitService.getRateLimitStatus(key2);

            assertThat(status1.getCurrentCount()).isEqualTo(0); // Old key should be gone (default status)
            assertThat(status1.isAllowed()).isTrue(); // Default status is allowed

            assertThat(status2.getCurrentCount()).isEqualTo(1); // Recent key should still be there
            assertThat(status2.isAllowed()).isTrue();

        } catch (Exception e) {
            fail("Reflection error during cleanupCache test: " + e.getMessage());
        }
    }

    // These boundary tests rely on Thread.sleep which is flaky.
    // They are testing the `calculateBoundary` method which is private.
    // It's better to trust truncatedTo or make calculateBoundary public/testable if needed for precise control.
    // For now, let's keep them as they are, but note their flakiness due to Thread.sleep.

    @Test
    @DisplayName("경계 시간 계산 - 초 단위")
    void calculateBoundary_Seconds_ShouldTruncateToSeconds() throws InterruptedException {
        String key = "boundary-test-seconds";
        int limit = 1;
        TimeUnit timeUnit = TimeUnit.SECONDS;

        boolean result1 = rateLimitService.isAllowed(key, limit, timeUnit); // count=1
        
        Thread.sleep(500); // within the same second
        
        boolean result2 = rateLimitService.isAllowed(key, limit, timeUnit); // should be denied
        
        assertThat(result1).isTrue();
        assertThat(result2).isFalse(); 
        
        Thread.sleep(1001); // pass to next second
        
        boolean result3 = rateLimitService.isAllowed(key, limit, timeUnit); // should be allowed (reset)
        assertThat(result3).isTrue();
    }

    @Test
    @DisplayName("경계 시간 계산 - 분 단위")
    void calculateBoundary_Minutes_ShouldTruncateToMinutes() throws InterruptedException {
        String key = "boundary-test-minutes";
        int limit = 1;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        boolean result1 = rateLimitService.isAllowed(key, limit, timeUnit); // count=1
        
        Thread.sleep(30 * 1000); // within the same minute
        
        boolean result2 = rateLimitService.isAllowed(key, limit, timeUnit); // should be denied
        
        assertThat(result1).isTrue();
        assertThat(result2).isFalse(); 
        
        Thread.sleep(31 * 1000); // pass to next minute
        
        boolean result3 = rateLimitService.isAllowed(key, limit, timeUnit); // should be allowed (reset)
        assertThat(result3).isTrue();
    }

    @Test
    @DisplayName("Rate Limiting 정확성 - 정확한 한도 적용")
    void isAllowed_ExactLimit_ShouldBeAccurate() {
        String key = "exact-limit-test";
        int limit = 1;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        boolean result1 = rateLimitService.isAllowed(key, limit, timeUnit); // count=1, allowed
        boolean result2 = rateLimitService.isAllowed(key, limit, timeUnit); // denied

        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
        
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);
        assertThat(status.getCurrentCount()).isEqualTo(1); // Count should be at limit
        assertThat(status.getLimit()).isEqualTo(limit);
        assertThat(status.getRemaining()).isEqualTo(0);
        assertThat(status.isAllowed()).isFalse();
    }

    @Test
    @DisplayName("Rate Limiting 정확성 - 0 한도 설정")
    void isAllowed_ZeroLimit_ShouldDenyAll() {
        String key = "zero-limit-test";
        int limit = 0;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        boolean result = rateLimitService.isAllowed(key, limit, timeUnit);

        assertThat(result).isFalse();
        
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);
        assertThat(status.getCurrentCount()).isEqualTo(0); // Should remain 0
        assertThat(status.getLimit()).isEqualTo(limit);
        assertThat(status.getRemaining()).isEqualTo(0);
        assertThat(status.isAllowed()).isFalse();
    }

    @Test
    @DisplayName("Rate Limiting 정확성 - 음수 한도 설정")
    void isAllowed_NegativeLimit_ShouldDenyAll() {
        String key = "negative-limit-test";
        int limit = -1;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        boolean result = rateLimitService.isAllowed(key, limit, timeUnit);

        assertThat(result).isFalse();
        
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);
        assertThat(status.getCurrentCount()).isEqualTo(0); // Should remain 0
        assertThat(status.getLimit()).isEqualTo(limit);
        assertThat(status.getRemaining()).isEqualTo(0);
        assertThat(status.isAllowed()).isFalse();
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 동시 호출")
    void isAllowed_ConcurrentCalls_ShouldHandleCorrectly() throws InterruptedException {
        String key = "concurrent-test";
        int limit = 5;
        TimeUnit timeUnit = TimeUnit.SECONDS; // Use seconds for faster test completion
        int threadCount = 10;
        boolean[] results = new boolean[threadCount];

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = rateLimitService.isAllowed(key, limit, timeUnit);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        int allowedCount = 0;
        int deniedCount = 0;
        for (boolean result : results) {
            if (result) {
                allowedCount++;
            } else {
                deniedCount++;
            }
        }

        // Due to concurrent execution and `Thread.sleep` (even if removed),
        // there might be slight variations.
        // We assert that the allowed count does not exceed the limit.
        assertThat(allowedCount).isEqualTo(limit); 
        assertThat(deniedCount).isEqualTo(threadCount - limit); 
        
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);
        assertThat(status.getCurrentCount()).isEqualTo(limit);
        assertThat(status.getLimit()).isEqualTo(limit);
        assertThat(status.getRemaining()).isEqualTo(0);
        assertThat(status.isAllowed()).isFalse();
    }

    @Test
    @DisplayName("메모리 효율성 - 많은 키 생성 후 상태 조회")
    void getRateLimitStatus_ManyKeys_ShouldHandleCorrectly() {
        int keyCount = 1000;
        int limit = 10;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        for (int i = 0; i < keyCount; i++) {
            String key = "key-" + i;
            rateLimitService.isAllowed(key, limit, timeUnit);
        }

        for (int i = 0; i < keyCount; i++) {
            String key = "key-" + i;
            RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key);
            
            assertThat(status).isNotNull();
            assertThat(status.getCurrentCount()).isEqualTo(1);
            assertThat(status.getLimit()).isEqualTo(limit);
            assertThat(status.getRemaining()).isEqualTo(limit - 1);
            assertThat(status.isAllowed()).isTrue();
        }
    }
}
