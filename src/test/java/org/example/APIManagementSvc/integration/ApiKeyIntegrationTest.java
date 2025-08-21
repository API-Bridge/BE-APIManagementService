package org.example.APIManagementSvc.integration;

import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ApiKey;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;
import org.example.APIManagementSvc.dto.apikey.ApiKeyRegistrationRequest;
import org.example.APIManagementSvc.repository.ApiKeyRepository;
import org.example.APIManagementSvc.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("ApiKey 통합 테스트")
class ApiKeyIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api-keys";
    }

    @Test
    @DisplayName("API 키 전체 생명주기 테스트")
    void apiKeyLifecycleTest() {
        // 1. API 키 등록
        ApiKey newApiKey = createTestApiKey();
        ApiKey savedApiKey = apiKeyService.registerApiKey(
                newApiKey.getOrganizationName(),
                newApiKey.getOrganizationCode(),
                newApiKey.getContactEmail(),
                newApiKey.getContactPhone(),
                newApiKey.getApiServiceName(),
                newApiKey.getApiServiceUrl(),
                newApiKey.getApiKey(),
                newApiKey.getSecretKey(),
                newApiKey.getDailyLimit(),
                newApiKey.getMonthlyLimit(),
                newApiKey.getExpiresAt(),
                newApiKey.getDescription(),
                newApiKey.getRequestedApis()
        );

        assertThat(savedApiKey).isNotNull();
        assertThat(savedApiKey.getKeyId()).isNotNull();
        assertThat(savedApiKey.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);

        // 2. API 키 조회
        String keyId = savedApiKey.getKeyId();
        var foundApiKey = apiKeyService.getApiKey(keyId);
        assertThat(foundApiKey).isPresent();
        assertThat(foundApiKey.get().getOrganizationName()).isEqualTo("통합 테스트 조직");

        // 3. API 키 수정
        ApiKey updateData = ApiKey.builder()
                .description("수정된 설명")
                .dailyLimit(2000)
                .monthlyLimit(60000)
                .build();

        ApiKey updatedApiKey = apiKeyService.updateApiKey(keyId, updateData);
        assertThat(updatedApiKey.getDescription()).isEqualTo("수정된 설명");
        assertThat(updatedApiKey.getDailyLimit()).isEqualTo(2000);

        // 4. API 키 상태 변경
        ApiKey statusChangedApiKey = apiKeyService.updateApiKeyStatus(keyId, ApiKeyStatus.SUSPENDED);
        assertThat(statusChangedApiKey.getStatus()).isEqualTo(ApiKeyStatus.SUSPENDED);

        // 5. API 키 사용량 증가
        apiKeyService.incrementUsage(keyId);
        var apiKeyAfterUsage = apiKeyService.getApiKey(keyId);
        assertThat(apiKeyAfterUsage).isPresent();
        assertThat(apiKeyAfterUsage.get().getCurrentDailyUsage()).isEqualTo(1);
        assertThat(apiKeyAfterUsage.get().getCurrentMonthlyUsage()).isEqualTo(1);

        // 6. API 키 삭제
        apiKeyService.deleteApiKey(keyId);
        var deletedApiKey = apiKeyService.getApiKey(keyId);
        assertThat(deletedApiKey).isEmpty();
    }

    @Test
    @DisplayName("HTTP 엔드포인트 통합 테스트")
    void httpEndpointsIntegrationTest() {
        // 1. API 키 등록 HTTP 요청
        String registrationUrl = baseUrl + "/register";
        var registrationRequest = createRegistrationRequest();
        
        ResponseEntity<String> registrationResponse = restTemplate.postForEntity(
                registrationUrl, registrationRequest, String.class);
        
        log.info("Registration URL: {}", registrationUrl);
        log.info("Response Status: {}", registrationResponse.getStatusCode());
        log.info("Response Body: {}", registrationResponse.getBody());
        
        assertThat(registrationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registrationResponse.getBody()).contains("success");

        // 2. API 키 목록 조회 HTTP 요청
        String listUrl = baseUrl + "?page=0&size=10";
        ResponseEntity<String> listResponse = restTemplate.getForEntity(listUrl, String.class);
        
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).contains("success");

        // 3. 활성 API 키 조회 HTTP 요청
        String activeUrl = baseUrl + "/active";
        ResponseEntity<String> activeResponse = restTemplate.getForEntity(activeUrl, String.class);
        
        assertThat(activeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(activeResponse.getBody()).contains("success");
    }

    @Test
    @DisplayName("데이터베이스 연동 테스트")
    void databaseIntegrationTest() {
        // 1. 데이터 저장
        ApiKey testApiKey = createTestApiKey();
        ApiKey savedApiKey = apiKeyRepository.save(testApiKey);
        assertThat(savedApiKey.getKeyId()).isNotNull();

        // 2. 데이터 조회
        var foundApiKey = apiKeyRepository.findById(savedApiKey.getKeyId());
        assertThat(foundApiKey).isPresent();
        assertThat(foundApiKey.get().getOrganizationName()).isEqualTo("통합 테스트 조직");

        // 3. 데이터 수정
        savedApiKey.setDescription("데이터베이스 테스트 설명");
        ApiKey updatedApiKey = apiKeyRepository.save(savedApiKey);
        assertThat(updatedApiKey.getDescription()).isEqualTo("데이터베이스 테스트 설명");

        // 4. 데이터 삭제
        apiKeyRepository.deleteById(savedApiKey.getKeyId());
        var deletedApiKey = apiKeyRepository.findById(savedApiKey.getKeyId());
        assertThat(deletedApiKey).isEmpty();
    }

    @Test
    @DisplayName("서비스 계층 통합 테스트")
    void serviceLayerIntegrationTest() {
        // 1. API 키 등록
        ApiKey testApiKey = createTestApiKey();
        ApiKey savedApiKey = apiKeyService.registerApiKey(
                testApiKey.getOrganizationName(),
                testApiKey.getOrganizationCode(),
                testApiKey.getContactEmail(),
                testApiKey.getContactPhone(),
                testApiKey.getApiServiceName(),
                testApiKey.getApiServiceUrl(),
                testApiKey.getApiKey(),
                testApiKey.getSecretKey(),
                testApiKey.getDailyLimit(),
                testApiKey.getMonthlyLimit(),
                testApiKey.getExpiresAt(),
                testApiKey.getDescription(),
                testApiKey.getRequestedApis()
        );

        // 2. 조직별 API 키 조회
        var organizationKeys = apiKeyService.getApiKeysByOrganization("통합 테스트 조직");
        assertThat(organizationKeys).hasSize(1);
        assertThat(organizationKeys.get(0).getKeyId()).isEqualTo(savedApiKey.getKeyId());

        // 3. 서비스별 API 키 조회
        var serviceKeys = apiKeyService.getApiKeysByService("INTEGRATION_TEST_API");
        assertThat(serviceKeys).hasSize(1);
        assertThat(serviceKeys.get(0).getKeyId()).isEqualTo(savedApiKey.getKeyId());

        // 4. 활성 API 키 조회
        var activeKeys = apiKeyService.getAllActiveApiKeys();
        assertThat(activeKeys).hasSize(1);
        assertThat(activeKeys.get(0).getKeyId()).isEqualTo(savedApiKey.getKeyId());
    }

    private ApiKey createTestApiKey() {
        return ApiKey.builder()
                .keyId("integration-test-key")
                .organizationName("통합 테스트 조직")
                .apiServiceName("INTEGRATION_TEST_API")
                .apiKey("integration-test-api-key")
                .secretKey("integration-test-secret-key")
                .contactEmail("integration@example.com")
                .contactPhone("010-9999-9999")
                .dailyLimit(5000)
                .monthlyLimit(150000)
                .currentDailyUsage(0)
                .currentMonthlyUsage(0)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .status(ApiKeyStatus.ACTIVE)
                .description("통합 테스트용 API 키")
                .requestedApis("integration,test,api")
                .build();
    }

    private ApiKeyRegistrationRequest createRegistrationRequest() {
        ApiKeyRegistrationRequest request = new ApiKeyRegistrationRequest();
        request.setOrganizationName("HTTP 테스트 조직");
        request.setOrganizationCode("HTTP001");
        request.setContactEmail("http@example.com");
        request.setContactPhone("010-8888-8888");
        request.setApiServiceName("HTTP_TEST_API");
        request.setApiServiceUrl("https://http.test.com");
        request.setApiKey("http-test-api-key");
        request.setSecretKey("http-test-secret-key");
        request.setDailyLimit(3000);
        request.setMonthlyLimit(90000);
        request.setExpiresAt(LocalDateTime.now().plusYears(1));
        request.setDescription("HTTP 테스트용 API 키");
        request.setRequestedApis("http,test,api");
        return request;
    }
}
