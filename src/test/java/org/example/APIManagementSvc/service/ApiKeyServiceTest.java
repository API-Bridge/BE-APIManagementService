package org.example.APIManagementSvc.service;

import org.example.APIManagementSvc.domain.Entity.ApiKey;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;
import org.example.APIManagementSvc.repository.ApiKeyRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyService 테스트")
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private ApiKey testApiKey;
    private ApiKey expiredApiKey;
    private ApiKey inactiveApiKey;

    @BeforeEach
    void setUp() {
        testApiKey = ApiKey.builder()
                .keyId("test-key-001")
                .organizationName("테스트 조직")
                .apiServiceName("TEST_API")
                .apiKey("test-api-key-12345")
                .secretKey("test-secret-key-67890")
                .contactEmail("test@example.com")
                .contactPhone("010-1234-5678")
                .dailyLimit(1000)
                .monthlyLimit(30000)
                .currentDailyUsage(0)
                .currentMonthlyUsage(0)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .status(ApiKeyStatus.ACTIVE)
                .build();

        expiredApiKey = ApiKey.builder()
                .keyId("expired-key-001")
                .organizationName("만료된 조직")
                .apiServiceName("EXPIRED_API")
                .apiKey("expired-api-key")
                .secretKey("expired-secret-key")
                .contactEmail("expired@example.com")
                .contactPhone("010-9999-9999")
                .dailyLimit(1000)
                .monthlyLimit(30000)
                .currentDailyUsage(0)
                .currentMonthlyUsage(0)
                .issuedAt(LocalDateTime.now().minusYears(1))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .status(ApiKeyStatus.EXPIRED)
                .build();

        inactiveApiKey = ApiKey.builder()
                .keyId("inactive-key-001")
                .organizationName("비활성 조직")
                .apiServiceName("INACTIVE_API")
                .apiKey("inactive-api-key")
                .secretKey("inactive-secret-key")
                .contactEmail("inactive@example.com")
                .contactPhone("010-8888-8888")
                .dailyLimit(1000)
                .monthlyLimit(30000)
                .currentDailyUsage(0)
                .currentMonthlyUsage(0)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .status(ApiKeyStatus.INACTIVE)
                .build();
    }

    @Test
    @DisplayName("API 키 등록 성공")
    void registerApiKey_Success() {
        // given
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        // when
        ApiKey result = apiKeyService.registerApiKey(
            "테스트 조직", "ORG001", "test@example.com", "010-1234-5678",
            "TEST_API", "https://api.test.com", "test-api-key-12345", "test-secret-key-67890",
            1000, 30000, LocalDateTime.now().plusYears(1), "테스트용 API 키", "test,api,service"
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getKeyId()).isEqualTo("test-key-001");
        assertThat(result.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("API 키 등록 실패 - 필수 필드 누락")
    void registerApiKey_Failure_MissingRequiredFields() {
        // when & then
        assertThatThrownBy(() -> apiKeyService.registerApiKey(
            null, "ORG001", "test@example.com", "010-1234-5678",
            "TEST_API", "https://api.test.com", "test-api-key-12345", "test-secret-key-67890",
            1000, 30000, LocalDateTime.now().plusYears(1), "테스트용 API 키", "test,api,service"
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("API 키 등록에 실패했습니다: 기관명은 필수입니다.");
    }

    @Test
    @DisplayName("API 키 조회 성공")
    void getApiKey_Success() {
        // given
        when(apiKeyRepository.findByKeyIdAndDeletedFalse("test-key-001"))
                .thenReturn(Optional.of(testApiKey));

        // when
        Optional<ApiKey> result = apiKeyService.getApiKey("test-key-001");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getKeyId()).isEqualTo("test-key-001");
        verify(apiKeyRepository).findByKeyIdAndDeletedFalse("test-key-001");
    }

    @Test
    @DisplayName("API 키 조회 실패 - 존재하지 않는 키")
    void getApiKey_Failure_NotFound() {
        // given
        when(apiKeyRepository.findByKeyIdAndDeletedFalse("non-existent-key"))
                .thenReturn(Optional.empty());

        // when
        Optional<ApiKey> result = apiKeyService.getApiKey("non-existent-key");

        // then
        assertThat(result).isEmpty();
        verify(apiKeyRepository).findByKeyIdAndDeletedFalse("non-existent-key");
    }

    @Test
    @DisplayName("조직별 API 키 조회 성공")
    void getApiKeysByOrganization_Success() {
        // given
        String organizationName = "테스트 조직";
        List<ApiKey> expectedApiKeys = Arrays.asList(testApiKey);
        when(apiKeyRepository.findByOrganizationNameAndDeletedFalse(organizationName))
                .thenReturn(expectedApiKeys);

        // when
        List<ApiKey> result = apiKeyService.getApiKeysByOrganization(organizationName);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrganizationName()).isEqualTo(organizationName);
        verify(apiKeyRepository).findByOrganizationNameAndDeletedFalse(organizationName);
    }

    @Test
    @DisplayName("모든 API 키 조회 (페이지네이션) 성공")
    void getAllApiKeys_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ApiKey> expectedPage = new PageImpl<>(Arrays.asList(testApiKey), pageable, 1);
        when(apiKeyRepository.findByDeletedFalse(pageable)).thenReturn(expectedPage);

        // when
        Page<ApiKey> result = apiKeyService.getAllApiKeys(pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(apiKeyRepository).findByDeletedFalse(pageable);
    }

    @Test
    @DisplayName("API 키 수정 성공")
    void updateApiKey_Success() {
        // given
        ApiKey updateData = ApiKey.builder()
                .keyId("test-key-001")
                .description("수정된 설명")
                .dailyLimit(2000)
                .monthlyLimit(60000)
                .build();

        when(apiKeyRepository.findByKeyIdAndDeletedFalse("test-key-001"))
                .thenReturn(Optional.of(testApiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        // when
        ApiKey result = apiKeyService.updateApiKey("test-key-001", updateData);

        // then
        assertThat(result).isNotNull();
        verify(apiKeyRepository).findByKeyIdAndDeletedFalse("test-key-001");
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("API 키 수정 실패 - 존재하지 않는 키")
    void updateApiKey_Failure_NotFound() {
        // given
        ApiKey updateData = ApiKey.builder().build();
        when(apiKeyRepository.findByKeyIdAndDeletedFalse("non-existent-key"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> apiKeyService.updateApiKey("non-existent-key", updateData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("API key not found: non-existent-key");
    }

    @Test
    @DisplayName("API 키 상태 변경 성공")
    void updateApiKeyStatus_Success() {
        // given
        when(apiKeyRepository.findByKeyIdAndDeletedFalse("test-key-001"))
                .thenReturn(Optional.of(testApiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        // when
        ApiKey result = apiKeyService.updateApiKeyStatus("test-key-001", ApiKeyStatus.SUSPENDED);

        // then
        assertThat(result).isNotNull();
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("API 키 삭제 성공")
    void deleteApiKey_Success() {
        // given
        when(apiKeyRepository.findByKeyIdAndDeletedFalse("test-key-001"))
                .thenReturn(Optional.of(testApiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        // when
        apiKeyService.deleteApiKey("test-key-001");

        // then
        verify(apiKeyRepository).save(any(ApiKey.class));
        assertThat(testApiKey.getDeleted()).isTrue();
    }

    @Test
    @DisplayName("API 키 사용량 증가 성공")
    void incrementUsage_Success() {
        // given
        when(apiKeyRepository.findByKeyIdAndDeletedFalse("test-key-001"))
                .thenReturn(Optional.of(testApiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        // when
        apiKeyService.incrementUsage("test-key-001");

        // then
        assertThat(testApiKey.getCurrentDailyUsage()).isEqualTo(1);
        assertThat(testApiKey.getCurrentMonthlyUsage()).isEqualTo(1);
        assertThat(testApiKey.getLastUsedAt()).isNotNull();
        verify(apiKeyRepository).save(testApiKey);
    }

    @Test
    @DisplayName("API 키 사용량 증가 - 일일 한도에 도달")
    void incrementUsage_DailyLimitReached() {
        // given
        testApiKey.setCurrentDailyUsage(1000); // 일일 한도에 도달
        when(apiKeyRepository.findByKeyIdAndDeletedFalse("test-key-001"))
                .thenReturn(Optional.of(testApiKey));

        // when
        apiKeyService.incrementUsage("test-key-001");

        // then
        assertThat(testApiKey.getCurrentDailyUsage()).isEqualTo(1001); // 한도를 초과해도 증가
        verify(apiKeyRepository).save(testApiKey);
    }

    @Test
    @DisplayName("API 키 사용량 증가 - 월간 한도에 도달")
    void incrementUsage_MonthlyLimitReached() {
        // given
        testApiKey.setCurrentMonthlyUsage(30000); // 월간 한도에 도달
        when(apiKeyRepository.findByKeyIdAndDeletedFalse("test-key-001"))
                .thenReturn(Optional.of(testApiKey));

        // when
        apiKeyService.incrementUsage("test-key-001");

        // then
        assertThat(testApiKey.getCurrentMonthlyUsage()).isEqualTo(30001); // 한도를 초과해도 증가
        verify(apiKeyRepository).save(testApiKey);
    }

    @Test
    @DisplayName("활성 API 키 조회")
    void getActiveApiKeys_Success() {
        // given
        List<ApiKey> expectedKeys = Arrays.asList(testApiKey);
        when(apiKeyRepository.findByStatusAndDeletedFalse(ApiKeyStatus.ACTIVE))
                .thenReturn(expectedKeys);

        // when
        List<ApiKey> result = apiKeyService.getAllActiveApiKeys();

        // then
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(key -> key.getStatus() == ApiKeyStatus.ACTIVE);
        verify(apiKeyRepository).findByStatusAndDeletedFalse(ApiKeyStatus.ACTIVE);
    }

    @Test
    @DisplayName("만료된 API 키 조회")
    void getExpiredApiKeys_Success() {
        // given
        List<ApiKey> expectedKeys = Arrays.asList(expiredApiKey);
        when(apiKeyRepository.findExpiredApiKeys(any(LocalDateTime.class))).thenReturn(expectedKeys);

        // when
        List<ApiKey> result = apiKeyRepository.findExpiredApiKeys(LocalDateTime.now());

        // then
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(key -> key.getStatus() == ApiKeyStatus.EXPIRED);
        verify(apiKeyRepository).findExpiredApiKeys(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("API 키 상태 확인 - 유효한 키")
    void checkApiKeyStatus_ValidKey() {
        // given
        when(apiKeyRepository.findByKeyIdAndDeletedFalse("test-key-001"))
                .thenReturn(Optional.of(testApiKey));

        // when
        Optional<ApiKey> result = apiKeyService.getApiKey("test-key-001");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
    }

    @Test
    @DisplayName("API 키 상태 확인 - 만료된 키")
    void checkApiKeyStatus_ExpiredKey() {
        // given
        when(apiKeyRepository.findByKeyIdAndDeletedFalse("expired-key-001"))
                .thenReturn(Optional.of(expiredApiKey));

        // when
        Optional<ApiKey> result = apiKeyService.getApiKey("expired-key-001");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(ApiKeyStatus.EXPIRED);
    }

    @Test
    @DisplayName("API 키 상태 확인 - 비활성 키")
    void checkApiKeyStatus_InactiveKey() {
        // given
        when(apiKeyRepository.findByKeyIdAndDeletedFalse("inactive-key-001"))
                .thenReturn(Optional.of(inactiveApiKey));

        // when
        Optional<ApiKey> result = apiKeyService.getApiKey("inactive-key-001");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(ApiKeyStatus.INACTIVE);
    }

    @Test
    @DisplayName("API 키 상태 확인 - 존재하지 않는 키")
    void checkApiKeyStatus_NonExistentKey() {
        // given
        when(apiKeyRepository.findByKeyIdAndDeletedFalse("non-existent-key"))
                .thenReturn(Optional.empty());

        // when
        Optional<ApiKey> result = apiKeyService.getApiKey("non-existent-key");

        // then
        assertThat(result).isEmpty();
    }
}
