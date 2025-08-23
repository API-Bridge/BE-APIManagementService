package org.example.APIManagementSvc.service;

import org.example.APIManagementSvc.dto.cache.ApiHealthStatusDto;
import org.example.APIManagementSvc.dto.cache.ApiStatusCacheDto;
import org.example.APIManagementSvc.dto.cache.CacheKeyDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisCacheService 단위 테스트")
class RedisCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private ZSetOperations<String, Object> zSetOps;

    @InjectMocks
    private RedisCacheService redisCacheService;

    private static final String API_ID = "test-api-001";
    private ApiStatusCacheDto apiStatusCacheDto;
    private CacheKeyDto apiStatusCacheKey;

    @BeforeEach
    void setUp() {
        // Mock redisTemplate.opsForValue() and redisTemplate.opsForZSet()
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

        apiStatusCacheDto = ApiStatusCacheDto.builder()
                .apiId(API_ID)
                .status("HEALTHY")
                .ttlSeconds(100L)
                .build();
        apiStatusCacheKey = CacheKeyDto.create(CacheKeyDto.CacheKeyType.API_STATUS, API_ID, 100L);
    }

    // ==================== API 상태 정보 캐싱 테스트 ====================

    @Test
    @DisplayName("API 상태 정보 캐시 저장 성공")
    void cacheApiStatus_Success() {
        // Given
        doNothing().when(valueOps).set(eq(apiStatusCacheKey.getKey()), eq(apiStatusCacheDto), eq(100L), eq(TimeUnit.SECONDS));

        // When
        boolean result = redisCacheService.cacheApiStatus(API_ID, apiStatusCacheDto);

        // Then
        assertThat(result).isTrue();
        verify(valueOps).set(eq(apiStatusCacheKey.getKey()), eq(apiStatusCacheDto), eq(100L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("API 상태 정보 캐시 저장 실패 - 예외 발생")
    void cacheApiStatus_Failure_Exception() {
        // Given
        doThrow(new RuntimeException("Redis error")).when(valueOps)
                .set(anyString(), any(ApiStatusCacheDto.class), anyLong(), any(TimeUnit.class));

        // When
        boolean result = redisCacheService.cacheApiStatus(API_ID, apiStatusCacheDto);

        // Then
        assertThat(result).isFalse();
        verify(valueOps).set(anyString(), any(ApiStatusCacheDto.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("API 상태 정보 조회 성공 - 캐시 히트")
    void getApiStatus_Success_CacheHit() {
        // Given
        when(valueOps.get(apiStatusCacheKey.getKey())).thenReturn(apiStatusCacheDto);

        // When
        ApiStatusCacheDto result = redisCacheService.getApiStatus(API_ID);

        // Then
        assertThat(result).isEqualTo(apiStatusCacheDto);
        verify(valueOps).get(apiStatusCacheKey.getKey());
    }

    @Test
    @DisplayName("API 상태 정보 조회 성공 - 캐시 미스")
    void getApiStatus_Success_CacheMiss() {
        // Given
        when(valueOps.get(apiStatusCacheKey.getKey())).thenReturn(null);

        // When
        ApiStatusCacheDto result = redisCacheService.getApiStatus(API_ID);

        // Then
        assertThat(result).isNull();
        verify(valueOps).get(apiStatusCacheKey.getKey());
    }

    @Test
    @DisplayName("API 상태 정보 조회 실패 - 예외 발생")
    void getApiStatus_Failure_Exception() {
        // Given
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("Redis error"));

        // When
        ApiStatusCacheDto result = redisCacheService.getApiStatus(API_ID);

        // Then
        assertThat(result).isNull();
        verify(valueOps).get(anyString());
    }

    @Test
    @DisplayName("여러 API 상태 정보 일괄 캐시 성공")
    void cacheApiStatusBatch_Success() {
        // Given
        Map<String, ApiStatusCacheDto> apiStatusMap = new HashMap<>();
        apiStatusMap.put(API_ID, apiStatusCacheDto);
        apiStatusMap.put("test-api-002", ApiStatusCacheDto.builder().apiId("test-api-002").status("DOWN").build());

        doNothing().when(valueOps).set(anyString(), any(ApiStatusCacheDto.class), anyLong(), any(TimeUnit.class));

        // When
        int successCount = redisCacheService.cacheApiStatusBatch(apiStatusMap);

        // Then
        assertThat(successCount).isEqualTo(2);
        verify(valueOps, times(2)).set(anyString(), any(ApiStatusCacheDto.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("여러 API 상태 정보 일괄 캐시 부분 성공")
    void cacheApiStatusBatch_PartialSuccess() {
        // Given
        Map<String, ApiStatusCacheDto> apiStatusMap = new HashMap<>();
        apiStatusMap.put(API_ID, apiStatusCacheDto);
        apiStatusMap.put("test-api-002", ApiStatusCacheDto.builder().apiId("test-api-002").status("DOWN").build());

        doNothing().when(valueOps).set(eq(apiStatusCacheKey.getKey()), any(ApiStatusCacheDto.class), anyLong(), any(TimeUnit.class));
        doThrow(new RuntimeException("Redis error")).when(valueOps)
                .set(eq(CacheKeyDto.CacheKeyType.API_STATUS.buildKey("test-api-002")), any(ApiStatusCacheDto.class), anyLong(), any(TimeUnit.class));

        // When
        int successCount = redisCacheService.cacheApiStatusBatch(apiStatusMap);

        // Then
        assertThat(successCount).isEqualTo(1);
        verify(valueOps, times(2)).set(anyString(), any(ApiStatusCacheDto.class), anyLong(), any(TimeUnit.class));
    }

    // ==================== API 사용량 통계 캐싱 테스트 ====================

    @Test
    @DisplayName("API 사용량 통계 캐시 저장 성공")
    void cacheApiUsageStats_Success() {
        // Given
        String apiId = "usage-api-001";
        Map<String, Object> usageStats = new HashMap<>();
        usageStats.put("daily", 100);
        usageStats.put("monthly", 1000);
        String cacheKey = CacheKeyDto.CacheKeyType.API_USAGE_STATS.buildKey(apiId);

        doNothing().when(valueOps).set(eq(cacheKey), eq(usageStats), anyLong(), eq(TimeUnit.SECONDS));

        // When
        boolean result = redisCacheService.cacheApiUsageStats(apiId, usageStats);

        // Then
        assertThat(result).isTrue();
        verify(valueOps).set(eq(cacheKey), eq(usageStats), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("API 사용량 통계 조회 성공 - 캐시 히트")
    void getApiUsageStats_Success_CacheHit() {
        // Given
        String apiId = "usage-api-001";
        Map<String, Object> usageStats = new HashMap<>();
        usageStats.put("daily", 100);
        String cacheKey = CacheKeyDto.CacheKeyType.API_USAGE_STATS.buildKey(apiId);

        when(valueOps.get(cacheKey)).thenReturn(usageStats);

        // When
        Map<String, Object> result = redisCacheService.getApiUsageStats(apiId);

        // Then
        assertThat(result).isEqualTo(usageStats);
        verify(valueOps).get(cacheKey);
    }

    @Test
    @DisplayName("API 사용량 통계 조회 성공 - 캐시 미스")
    void getApiUsageStats_Success_CacheMiss() {
        // Given
        String apiId = "usage-api-001";
        String cacheKey = CacheKeyDto.CacheKeyType.API_USAGE_STATS.buildKey(apiId);

        when(valueOps.get(cacheKey)).thenReturn(null);

        // When
        Map<String, Object> result = redisCacheService.getApiUsageStats(apiId);

        // Then
        assertThat(result).isNull();
        verify(valueOps).get(cacheKey);
    }

    // ==================== 캐시 관리 및 모니터링 테스트 ====================

    @Test
    @DisplayName("특정 API 캐시 무효화 성공")
    void invalidateApiCache_Success() {
        // Given
        String statusKey = CacheKeyDto.CacheKeyType.API_STATUS.buildKey(API_ID);
        String usageKey = CacheKeyDto.CacheKeyType.API_USAGE_STATS.buildKey(API_ID);

        when(valueOps.getAndDelete(statusKey)).thenReturn(apiStatusCacheDto);
        when(valueOps.getAndDelete(usageKey)).thenReturn(new HashMap<>());

        // When
        boolean result = redisCacheService.invalidateApiCache(API_ID);

        // Then
        assertThat(result).isTrue();
        verify(valueOps).getAndDelete(statusKey);
        verify(valueOps).getAndDelete(usageKey);
    }

    @Test
    @DisplayName("패턴 기반 캐시 무효화 성공")
    void invalidateCacheByPattern_Success() {
        // Given
        String pattern = "test:*";
        Set<String> keys = new HashSet<>();
        keys.add("test:key1");
        keys.add("test:key2");

        when(redisTemplate.keys(pattern)).thenReturn(keys);
        when(redisTemplate.delete(keys)).thenReturn(2L); // 2 keys deleted

        // When
        int invalidatedCount = redisCacheService.invalidateCacheByPattern(pattern);

        // Then
        assertThat(invalidatedCount).isEqualTo(2);
        verify(redisTemplate).keys(pattern);
        verify(redisTemplate).delete(keys);
    }

    @Test
    @DisplayName("캐시 통계 정보 조회 성공")
    void getCacheStatistics_Success() {
        // Given
        Set<String> allKeys = new HashSet<>();
        allKeys.add("api:status:1");
        allKeys.add("api:usage:1");
        allKeys.add("api-key:1");

        Set<String> statusKeys = new HashSet<>();
        statusKeys.add("api:status:1");

        Set<String> usageKeys = new HashSet<>();
        usageKeys.add("api:usage:1");

        when(redisTemplate.keys("*")).thenReturn(allKeys);
        when(redisTemplate.keys("api:status:*")).thenReturn(statusKeys);
        when(redisTemplate.keys("api:usage:*")).thenReturn(usageKeys);

        // When
        Map<String, Object> stats = redisCacheService.getCacheStatistics();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.get("totalKeys")).isEqualTo(3);
        assertThat(stats.get("apiStatusKeys")).isEqualTo(1);
        assertThat(stats.get("apiUsageKeys")).isEqualTo(1);
        assertThat(stats.get("memoryUsage")).isEqualTo("N/A"); // Redis INFO 명령어로 조회 가능
    }

    @Test
    @DisplayName("캐시 TTL 연장 성공")
    void extendCacheTTL_Success() {
        // Given
        String apiId = API_ID;
        long additionalSeconds = 600; // 10분

        String statusKey = CacheKeyDto.CacheKeyType.API_STATUS.buildKey(apiId);
        String usageKey = CacheKeyDto.CacheKeyType.API_USAGE_STATS.buildKey(apiId);

        when(redisTemplate.getExpire(statusKey)).thenReturn(1800L); // 30분
        when(redisTemplate.getExpire(usageKey)).thenReturn(7200L); // 2시간

        doNothing().when(redisTemplate).expire(eq(statusKey), eq(1800L + additionalSeconds), eq(TimeUnit.SECONDS));
        doNothing().when(redisTemplate).expire(eq(usageKey), eq(7200L + additionalSeconds), eq(TimeUnit.SECONDS));

        // When
        boolean result = redisCacheService.extendCacheTTL(apiId, additionalSeconds);

        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).expire(eq(statusKey), eq(1800L + additionalSeconds), eq(TimeUnit.SECONDS));
        verify(redisTemplate).expire(eq(usageKey), eq(7200L + additionalSeconds), eq(TimeUnit.SECONDS));
    }

    // ==================== API 키 캐싱 테스트 ====================

    @Test
    @DisplayName("API 키 캐시 저장 성공")
    void cacheApiKey_Success() {
        // Given
        String keyId = "api-key-123";
        String apiKeyContent = "some-api-key-value";
        int ttl = 300;
        String cacheKey = "api-key:" + keyId;

        doNothing().when(valueOps).set(eq(cacheKey), eq(apiKeyContent), eq((long) ttl), eq(TimeUnit.SECONDS));

        // When
        boolean result = redisCacheService.cacheApiKey(keyId, apiKeyContent, ttl);

        // Then
        assertThat(result).isTrue();
        verify(valueOps).set(eq(cacheKey), eq(apiKeyContent), eq((long) ttl), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("API 키 조회 성공 - 캐시 히트")
    void getApiKey_Success_CacheHit() {
        // Given
        String keyId = "api-key-123";
        String apiKeyContent = "some-api-key-value";
        String cacheKey = "api-key:" + keyId;

        when(valueOps.get(cacheKey)).thenReturn(apiKeyContent);

        // When
        Object result = redisCacheService.getApiKey(keyId);

        // Then
        assertThat(result).isEqualTo(apiKeyContent);
        verify(valueOps).get(cacheKey);
    }

    @Test
    @DisplayName("API 키 조회 성공 - 캐시 미스")
    void getApiKey_Success_CacheMiss() {
        // Given
        String keyId = "api-key-123";
        String cacheKey = "api-key:" + keyId;

        when(valueOps.get(cacheKey)).thenReturn(null);

        // When
        Object result = redisCacheService.getApiKey(keyId);

        // Then
        assertThat(result).isNull();
        verify(valueOps).get(cacheKey);
    }

    @Test
    @DisplayName("API 키 목록 캐시 저장 성공")
    void cacheApiKeyList_Success() {
        // Given
        String cacheKey = "api-keys:page:0:size:10";
        String apiKeyListContent = "[\"key1\", \"key2\"]";
        int ttl = 600;

        doNothing().when(valueOps).set(eq(cacheKey), eq(apiKeyListContent), eq((long) ttl), eq(TimeUnit.SECONDS));

        // When
        boolean result = redisCacheService.cacheApiKeyList(cacheKey, apiKeyListContent, ttl);

        // Then
        assertThat(result).isTrue();
        verify(valueOps).set(eq(cacheKey), eq(apiKeyListContent), eq((long) ttl), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("API 키 목록 조회 성공 - 캐시 히트")
    void getApiKeyList_Success_CacheHit() {
        // Given
        String cacheKey = "api-keys:page:0:size:10";
        String apiKeyListContent = "[\"key1\", \"key2\"]";

        when(valueOps.get(cacheKey)).thenReturn(apiKeyListContent);

        // When
        Object result = redisCacheService.getApiKeyList(cacheKey);

        // Then
        assertThat(result).isEqualTo(apiKeyListContent);
        verify(valueOps).get(cacheKey);
    }

    @Test
    @DisplayName("API 키 목록 조회 성공 - 캐시 미스")
    void getApiKeyList_Success_CacheMiss() {
        // Given
        String cacheKey = "api-keys:page:0:size:10";

        when(valueOps.get(cacheKey)).thenReturn(null);

        // When
        Object result = redisCacheService.getApiKeyList(cacheKey);

        // Then
        assertThat(result).isNull();
        verify(valueOps).get(cacheKey);
    }

    @Test
    @DisplayName("특정 API 키 캐시 무효화 성공")
    void invalidateApiKeyCache_Success() {
        // Given
        String keyId = "api-key-123";
        String cacheKey = "api-key:" + keyId;

        when(valueOps.getAndDelete(cacheKey)).thenReturn("some-api-key-value");

        // When
        boolean result = redisCacheService.invalidateApiKeyCache(keyId);

        // Then
        assertThat(result).isTrue();
        verify(valueOps).getAndDelete(cacheKey);
    }

    @Test
    @DisplayName("API 키 목록 캐시 모두 무효화 성공")
    void invalidateApiKeyListCache_Success() {
        // Given
        Set<String> keys = new HashSet<>();
        keys.add("api-keys:page:0:size:10");
        keys.add("api-keys:page:1:size:10");

        when(redisTemplate.keys("api-keys:*")).thenReturn(keys);
        when(redisTemplate.delete(keys)).thenReturn(2L);

        // When
        int invalidatedCount = redisCacheService.invalidateApiKeyListCache();

        // Then
        assertThat(invalidatedCount).isEqualTo(2);
        verify(redisTemplate).keys("api-keys:*");
        verify(redisTemplate).delete(keys);
    }

    @Test
    @DisplayName("API 키 전체 캐시 무효화 성공")
    void invalidateAllApiKeyCache_Success() {
        // Given
        Set<String> keys = new HashSet<>();
        keys.add("api-key:123");
        keys.add("api-key:456");

        when(redisTemplate.keys("api-key:*")).thenReturn(keys);
        when(redisTemplate.delete(keys)).thenReturn(2L);

        // When
        int invalidatedCount = redisCacheService.invalidateAllApiKeyCache();

        // Then
        assertThat(invalidatedCount).isEqualTo(2);
        verify(redisTemplate).keys("api-key:*");
        verify(redisTemplate).delete(keys);
    }
}
