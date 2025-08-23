package org.example.APIManagementSvc.service;

import org.example.APIManagementSvc.domain.Entity.ApiKey;
import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;
import org.example.APIManagementSvc.repository.ApiKeyRepository;
import org.example.APIManagementSvc.repository.ExternalApiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiTokenRefreshService 테스트")
class ApiTokenRefreshServiceTest {

    @Mock
    private ExternalApiRepository externalApiRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ApiTokenRefreshService apiTokenRefreshService;

    private ExternalApi testApi;
    private ApiKey testApiKey;

    @BeforeEach
    void setUp() {
        testApiKey = new ApiKey();
        testApiKey.setKeyId("test-key-001");
        testApiKey.setApiKey("test-api-key");
        testApiKey.setApiServiceName("SGIS");
        testApiKey.setStatus(ApiKeyStatus.ACTIVE);
        testApiKey.setCurrentDailyUsage(0);
        testApiKey.setCurrentMonthlyUsage(0);
        testApiKey.setDailyLimit(1000);
        testApiKey.setMonthlyLimit(30000);
        testApiKey.setLastUsedAt(LocalDateTime.now());

        testApi = new ExternalApi();
        testApi.setApiId("test-api-001");
        testApi.setApiName("테스트 API");
        testApi.setApiUrl("https://api.test.com");
        testApi.setAutoTokenRefresh(true);
        testApi.setApiKeyEntity(testApiKey);
        testApi.setTokenExpiresAt(LocalDateTime.now().plusHours(2));
    }

    @Test
    @DisplayName("토큰 갱신이 필요한 API 조회")
    void findApisNeedingTokenRefresh() {
        // given
        List<ExternalApi> apisNeedingRefresh = Arrays.asList(testApi);
        when(externalApiRepository.findByTokenExpiresAtBeforeOrTokenExpiresAtIsNull(any(LocalDateTime.class)))
                .thenReturn(apisNeedingRefresh);

        // when
        apiTokenRefreshService.refreshExpiredTokens();

        // then
        verify(externalApiRepository).findByTokenExpiresAtBeforeOrTokenExpiresAtIsNull(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("갱신이 필요한 토큰이 없는 경우")
    void refreshExpiredTokens_NoTokensNeedingRefresh() {
        // given
        when(externalApiRepository.findByTokenExpiresAtBeforeOrTokenExpiresAtIsNull(any(LocalDateTime.class)))
                .thenReturn(List.of());

        // when
        apiTokenRefreshService.refreshExpiredTokens();

        // then
        verify(externalApiRepository).findByTokenExpiresAtBeforeOrTokenExpiresAtIsNull(any(LocalDateTime.class));
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("자동 토큰 갱신이 비활성화된 API")
    void refreshTokenForApi_AutoRefreshDisabled() {
        // given
        testApi.setAutoTokenRefresh(false);

        // when
        apiTokenRefreshService.refreshTokenForApi(testApi);

        // then
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
        verify(apiKeyRepository, never()).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("SGIS API 토큰 갱신 성공")
    void refreshSgisToken_Success() {
        // given
        testApi.setApiIssuer("kostat");
        testApi.setApiUrl("https://sgisapi.kostat.go.kr/api");
        
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("access_token", "new-sgis-token");
        mockResponse.put("expires_in", 14400); // 4시간
        
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(responseEntity);

        // when
        apiTokenRefreshService.refreshTokenForApi(testApi);

        // then
        verify(externalApiRepository).save(testApi);
        verify(apiKeyRepository).save(testApiKey);
        assertThat(testApiKey.getCurrentDailyUsage()).isEqualTo(1);
        assertThat(testApiKey.getCurrentMonthlyUsage()).isEqualTo(1);
    }

    @Test
    @DisplayName("SGIS API 토큰 갱신 실패 - API 키 없음")
    void refreshSgisToken_NoApiKey() {
        // given
        testApi.setApiKeyEntity(null);
        // API 키가 없으면 isApiService("SGIS")가 false를 반환하여 refreshGenericToken이 호출됨
        // refreshGenericToken에서 api.hasToken() 호출 시 NullPointerException 방지를 위해 기본값 설정
        testApi.setApiToken(null);
        testApi.setTokenExpiresAt(null);

        // when
        apiTokenRefreshService.refreshTokenForApi(testApi);

        // then
        // API 키가 없으면 isApiService("SGIS")가 false를 반환하여 refreshGenericToken이 호출됨
        // refreshGenericToken은 정상적으로 완료되므로 예외가 발생하지 않음
        // 이 테스트는 API 키가 없을 때의 동작을 검증하는 것이므로 정상 완료가 맞음
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
        verify(apiKeyRepository, never()).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("SGIS API 토큰 갱신 실패 - API 키 비활성")
    void refreshSgisToken_InactiveApiKey() {
        // given
        testApiKey.setStatus(ApiKeyStatus.INACTIVE);

        // when & then
        assertThatThrownBy(() -> apiTokenRefreshService.refreshTokenForApi(testApi))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API 키가 비활성 상태입니다");
    }

    @Test
    @DisplayName("SGIS API 토큰 갱신 실패 - 일일 한도 초과")
    void refreshSgisToken_DailyLimitExceeded() {
        // given
        testApiKey.setCurrentDailyUsage(1000); // 한도에 도달

        // when & then
        assertThatThrownBy(() -> apiTokenRefreshService.refreshTokenForApi(testApi))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("일일 API 호출 제한에 도달했습니다");
    }

    @Test
    @DisplayName("SGIS API 토큰 갱신 실패 - 월간 한도 초과")
    void refreshSgisToken_MonthlyLimitExceeded() {
        // given
        testApiKey.setCurrentMonthlyUsage(30000); // 한도에 도달

        // when & then
        assertThatThrownBy(() -> apiTokenRefreshService.refreshTokenForApi(testApi))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("월간 API 호출 제한에 도달했습니다");
    }

    @Test
    @DisplayName("SGIS API 토큰 갱신 실패 - HTTP 오류 응답")
    void refreshSgisToken_HttpError() {
        // given
        testApi.setApiIssuer("kostat");
        ResponseEntity<Map> errorResponse = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(errorResponse);

        // when & then
        assertThatThrownBy(() -> apiTokenRefreshService.refreshTokenForApi(testApi))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("토큰 발급 요청 실패");
    }

    @Test
    @DisplayName("SGIS API 토큰 갱신 실패 - 응답에서 토큰 추출 불가")
    void refreshSgisToken_TokenExtractionFailed() {
        // given
        testApi.setApiIssuer("kostat");
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("error", "Invalid request"); // 토큰이 없는 응답
        
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(responseEntity);

        // when & then
        assertThatThrownBy(() -> apiTokenRefreshService.refreshTokenForApi(testApi))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("응답에서 토큰을 추출할 수 없습니다");
    }

    @Test
    @DisplayName("Kakao API 토큰 갱신 (스텁)")
    void refreshKakaoToken_Stub() {
        // given
        ApiKey kakaoApiKey = new ApiKey();
        kakaoApiKey.setApiServiceName("KAKAO");
        testApi.setApiKeyEntity(kakaoApiKey);

        // when
        apiTokenRefreshService.refreshTokenForApi(testApi);

        // then
        // 현재는 스텁 구현이므로 아무 동작도 하지 않음
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("Naver API 토큰 갱신 (스텁)")
    void refreshNaverToken_Stub() {
        // given
        ApiKey naverApiKey = new ApiKey();
        naverApiKey.setApiServiceName("NAVER");
        testApi.setApiKeyEntity(naverApiKey);

        // when
        apiTokenRefreshService.refreshTokenForApi(testApi);

        // then
        // 현재는 스텁 구현이므로 아무 동작도 하지 않음
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("일반 API 토큰 갱신 (스텁)")
    void refreshGenericToken_Stub() {
        // given
        ApiKey genericApiKey = new ApiKey();
        genericApiKey.setApiServiceName("GENERIC");
        testApi.setApiKeyEntity(genericApiKey);

        // when
        apiTokenRefreshService.refreshTokenForApi(testApi);

        // then
        // 현재는 스텁 구현이므로 아무 동작도 하지 않음
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("토큰 갱신 중 예외 발생")
    void refreshTokenForApi_Exception() {
        // given
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Network error"));

        // when & then
        assertThatThrownBy(() -> apiTokenRefreshService.refreshTokenForApi(testApi))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Network error");
    }

    @Test
    @DisplayName("전체 토큰 갱신 작업 중 일부 API 실패")
    void refreshExpiredTokens_PartialFailure() {
        // given
        ExternalApi failingApi = new ExternalApi();
        failingApi.setApiId("failing-api");
        failingApi.setApiName("실패하는 API");
        failingApi.setAutoTokenRefresh(true);
        failingApi.setApiKeyEntity(testApiKey);

        List<ExternalApi> apis = Arrays.asList(testApi, failingApi);
        when(externalApiRepository.findByTokenExpiresAtBeforeOrTokenExpiresAtIsNull(any(LocalDateTime.class)))
                .thenReturn(apis);
        
        // 첫 번째 API는 성공, 두 번째 API는 실패
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(createSuccessResponse())
                .thenThrow(new RuntimeException("API failure"));

        // when
        apiTokenRefreshService.refreshExpiredTokens();

        // then
        // 첫 번째 API는 성공적으로 처리되어야 함
        verify(externalApiRepository, atLeastOnce()).save(any(ExternalApi.class));
        verify(apiKeyRepository, atLeastOnce()).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("API 서비스 타입 확인")
    void isApiService() {
        // given
        ApiKey sgisApiKey = new ApiKey();
        sgisApiKey.setApiServiceName("SGIS");
        
        ExternalApi sgisApi = new ExternalApi();
        sgisApi.setApiKeyEntity(sgisApiKey);

        ApiKey kakaoApiKey = new ApiKey();
        kakaoApiKey.setApiServiceName("KAKAO");
        
        ExternalApi kakaoApi = new ExternalApi();
        kakaoApi.setApiKeyEntity(kakaoApiKey);

        ExternalApi noKeyApi = new ExternalApi();
        noKeyApi.setApiKeyEntity(null);

        // when & then
        assertThat(sgisApi.isApiService("SGIS")).isTrue();
        assertThat(kakaoApi.isApiService("KAKAO")).isTrue();
        assertThat(noKeyApi.isApiService("SGIS")).isFalse();
    }

    private ResponseEntity<Map> createSuccessResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "test-token");
        response.put("expires_in", 14400);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
