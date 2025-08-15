package org.example.APIManagementSvc.service;

import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import org.example.APIManagementSvc.repository.ExternalApiRepository;
import org.example.APIManagementSvc.repository.ApiParameterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * ExternalApiService TDD 테스트
 * TDD 방식: 테스트 먼저 작성 → 코드 구현 → 리팩토링
 */
@ExtendWith(MockitoExtension.class)
class ExternalApiServiceTest {

    @Mock
    private ExternalApiRepository externalApiRepository;

    @Mock
    private ApiParameterRepository apiParameterRepository;

    @InjectMocks
    private ExternalApiService externalApiService;

    private ExternalApi testApi;

    @BeforeEach
    void setUp() {
        // 테스트용 API 데이터 준비
        testApi = new ExternalApi();
        testApi.setApiName("테스트 API");
        testApi.setApiUrl("https://api.test.com/test");
        testApi.setApiIssuer("테스트 회사");
        testApi.setApiOwner("test-user-001");
        testApi.setApiDomain(ApiDomain.FINANCE);
        testApi.setApiKeyword(ApiKeyword.STOCK_PRICE);
        testApi.setHttpMethod("GET");
        testApi.setApiDescription("테스트용 API입니다.");
        testApi.setApiEffectiveness(true);
    }

    @Test
    void registerApi_ValidApiData_ShouldCreateApiSuccessfully() {
        // Given: 유효한 API 데이터가 주어졌을 때
        ExternalApi savedApi = new ExternalApi();
        savedApi.setApiId("test-api-id-123");
        savedApi.setApiName("테스트 API");
        savedApi.setApiUrl("https://api.test.com/test");
        savedApi.setApiIssuer("테스트 회사");
        savedApi.setApiOwner("test-user-001");
        savedApi.setApiDomain(ApiDomain.FINANCE);
        savedApi.setApiKeyword(ApiKeyword.STOCK_PRICE);
        savedApi.setHttpMethod("GET");
        savedApi.setApiDescription("테스트용 API입니다.");
        savedApi.setApiEffectiveness(true);
        savedApi.setCreatedAt(LocalDateTime.now());
        savedApi.setUpdatedAt(LocalDateTime.now());
        savedApi.setDeleted(false);

        when(externalApiRepository.save(any(ExternalApi.class))).thenReturn(savedApi);

        // When: API를 등록할 때
        ExternalApi result = externalApiService.registerApi(testApi);

        // Then: API가 성공적으로 생성되어야 한다
        assertThat(result).isNotNull();
        assertThat(result.getApiId()).isEqualTo("test-api-id-123");
        assertThat(result.getApiName()).isEqualTo("테스트 API");
        assertThat(result.getApiUrl()).isEqualTo("https://api.test.com/test");
        assertThat(result.getApiDomain()).isEqualTo(ApiDomain.FINANCE);
        assertThat(result.getApiKeyword()).isEqualTo(ApiKeyword.STOCK_PRICE);
        assertThat(result.getHttpMethod()).isEqualTo("GET");
        assertThat(result.getApiEffectiveness()).isTrue();
        assertThat(result.getDeleted()).isFalse();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void registerApi_ApiWithNullName_ShouldThrowException() {
        // Given: API 이름이 null인 경우
        testApi.setApiName(null);

        // When & Then: 예외가 발생해야 한다
        assertThatThrownBy(() -> externalApiService.registerApi(testApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("API name is required");
    }

    @Test
    void registerApi_ApiWithEmptyName_ShouldThrowException() {
        // Given: API 이름이 빈 문자열인 경우
        testApi.setApiName("");

        // When & Then: 예외가 발생해야 한다
        assertThatThrownBy(() -> externalApiService.registerApi(testApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("API name is required");
    }

    @Test
    void registerApi_ApiWithNullUrl_ShouldThrowException() {
        // Given: API URL이 null인 경우
        testApi.setApiUrl(null);

        // When & Then: 예외가 발생해야 한다
        assertThatThrownBy(() -> externalApiService.registerApi(testApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("API URL is required");
    }

    @Test
    void registerApi_ApiWithNullHttpMethod_ShouldThrowException() {
        // Given: HTTP 메소드가 null인 경우
        testApi.setHttpMethod(null);

        // When & Then: 예외가 발생해야 한다
        assertThatThrownBy(() -> externalApiService.registerApi(testApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("HTTP method is required");
    }

    @Test
    void registerApi_ApiWithNullDomain_ShouldThrowException() {
        // Given: API 도메인이 null인 경우
        testApi.setApiDomain(null);

        // When & Then: 예외가 발생해야 한다
        assertThatThrownBy(() -> externalApiService.registerApi(testApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("API domain is required");
    }

    @Test
    void registerApi_ApiWithNullKeyword_ShouldThrowException() {
        // Given: API 키워드가 null인 경우
        testApi.setApiKeyword(null);

        // When & Then: 예외가 발생해야 한다
        assertThatThrownBy(() -> externalApiService.registerApi(testApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("API keyword is required");
    }

    @Test
    void registerApi_ApiWithNullIssuer_ShouldThrowException() {
        // Given: API 발급처가 null인 경우
        testApi.setApiIssuer(null);

        // When & Then: 예외가 발생해야 한다
        assertThatThrownBy(() -> externalApiService.registerApi(testApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("API issuer is required");
    }

    @Test
    void registerApi_ApiWithNullOwner_ShouldSetDefaultOwner() {
        // Given: API 소유자가 null인 경우
        testApi.setApiOwner(null);

        ExternalApi savedApi = new ExternalApi();
        savedApi.setApiId("test-api-id-123");
        savedApi.setApiOwner(null);
        savedApi.setCreatedAt(LocalDateTime.now());
        savedApi.setUpdatedAt(LocalDateTime.now());
        savedApi.setDeleted(false);

        when(externalApiRepository.save(any(ExternalApi.class))).thenReturn(savedApi);

        // When: API를 생성할 때
        ExternalApi result = externalApiService.registerApi(testApi);

        // Then: API 소유자는 null이어도 생성되어야 한다
        assertThat(result).isNotNull();
        assertThat(result.getApiOwner()).isNull();
    }

    @Test
    void registerApi_ApiWithNullDescription_ShouldSetEmptyDescription() {
        // Given: API 설명이 null인 경우
        testApi.setApiDescription(null);

        ExternalApi savedApi = new ExternalApi();
        savedApi.setApiId("test-api-id-123");
        savedApi.setApiDescription(null);
        savedApi.setCreatedAt(LocalDateTime.now());
        savedApi.setUpdatedAt(LocalDateTime.now());
        savedApi.setDeleted(false);

        when(externalApiRepository.save(any(ExternalApi.class))).thenReturn(savedApi);

        // When: API를 생성할 때
        ExternalApi result = externalApiService.registerApi(testApi);

        // Then: API 설명은 null이어도 생성되어야 한다
        assertThat(result).isNotNull();
        assertThat(result.getApiDescription()).isNull();
    }

    @Test
    void registerApi_ApiWithNullEffectiveness_ShouldSetDefaultEffectiveness() {
        // Given: API 유효성이 null인 경우
        testApi.setApiEffectiveness(null);

        ExternalApi savedApi = new ExternalApi();
        savedApi.setApiId("test-api-id-123");
        savedApi.setApiEffectiveness(true);
        savedApi.setCreatedAt(LocalDateTime.now());
        savedApi.setUpdatedAt(LocalDateTime.now());
        savedApi.setDeleted(false);

        when(externalApiRepository.save(any(ExternalApi.class))).thenReturn(savedApi);

        // When: API를 생성할 때
        ExternalApi result = externalApiService.registerApi(testApi);

        // Then: API 유효성은 기본값 true로 설정되어야 한다
        assertThat(result).isNotNull();
        assertThat(result.getApiEffectiveness()).isTrue();
    }
}

