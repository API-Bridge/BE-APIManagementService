package org.example.APIManagementSvc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.APIManagementSvc.domain.Entity.ApiKey;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;
import org.example.APIManagementSvc.dto.apikey.ApiKeyRegistrationRequest;
import org.example.APIManagementSvc.repository.ApiKeyRepository;
import org.example.APIManagementSvc.service.ApiKeyService;
import org.example.APIManagementSvc.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ApiKeyController 테스트")
class ApiKeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApiKeyService apiKeyService;

    @Autowired
    private ObjectMapper objectMapper;

    private ApiKey testApiKey;
    private ApiKey testApiKey2;

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

        testApiKey2 = ApiKey.builder()
                .keyId("test-key-002")
                .organizationName("테스트 조직 2")
                .apiServiceName("TEST_API_2")
                .apiKey("test-api-key-67890")
                .secretKey("test-secret-key-12345")
                .contactEmail("test2@example.com")
                .contactPhone("010-8765-4321")
                .dailyLimit(2000)
                .monthlyLimit(60000)
                .currentDailyUsage(0)
                .currentMonthlyUsage(0)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .status(ApiKeyStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("API 키 등록 성공")
    void registerApiKey_Success() throws Exception {
        // given
        when(apiKeyService.registerApiKey(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), any(LocalDateTime.class), anyString(), anyString()
        )).thenReturn(testApiKey);

        // when & then
        mockMvc.perform(post("/api-keys/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRegistrationRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.keyId").value("test-key-001"))
                .andExpect(jsonPath("$.data.organizationName").value("테스트 조직"));
    }

    @Test
    @DisplayName("API 키 등록 실패 - 유효성 검증 오류")
    void registerApiKey_ValidationError() throws Exception {
        // given
        when(apiKeyService.registerApiKey(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), any(LocalDateTime.class), anyString(), anyString()
        )).thenThrow(new IllegalArgumentException("기관명은 필수입니다"));

        // when & then
        mockMvc.perform(post("/api-keys/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createInvalidRegistrationRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API 키 조회 성공")
    void getApiKey_Success() throws Exception {
        // given
        when(apiKeyService.getApiKey("test-key-001")).thenReturn(Optional.of(testApiKey));

        // when & then
        mockMvc.perform(get("/api-keys/test-key-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.keyId").value("test-key-001"))
                .andExpect(jsonPath("$.data.organizationName").value("테스트 조직"));
    }

    @Test
    @DisplayName("API 키 조회 실패 - 존재하지 않는 키")
    void getApiKey_NotFound() throws Exception {
        // given
        when(apiKeyService.getApiKey("non-existent-key")).thenReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api-keys/non-existent-key"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("조직별 API 키 조회")
    void getApiKeysByOrganization_Success() throws Exception {
        // given
        List<ApiKey> apiKeys = Arrays.asList(testApiKey, testApiKey2);
        when(apiKeyService.getApiKeysByOrganization("테스트 조직")).thenReturn(apiKeys);

        // when & then
        mockMvc.perform(get("/api-keys/organization/테스트 조직"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("서비스별 API 키 조회")
    void getApiKeysByService_Success() throws Exception {
        // given
        List<ApiKey> apiKeys = Arrays.asList(testApiKey);
        when(apiKeyService.getApiKeysByService("TEST_API")).thenReturn(apiKeys);

        // when & then
        mockMvc.perform(get("/api-keys/service/TEST_API"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("페이지네이션으로 API 키 목록 조회")
    void getAllApiKeys_WithPagination_Success() throws Exception {
        // given
        Page<ApiKey> apiKeyPage = new PageImpl<>(
                Arrays.asList(testApiKey, testApiKey2),
                PageRequest.of(0, 10),
                2
        );
        when(apiKeyService.getApiKeysWithPaging(any())).thenReturn(apiKeyPage);

        // when & then
        mockMvc.perform(get("/api-keys")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("API 키 수정 성공")
    void updateApiKey_Success() throws Exception {
        // given
        when(apiKeyService.updateApiKey(eq("test-key-001"), any(ApiKey.class)))
                .thenReturn(testApiKey);

        // when & then
        mockMvc.perform(put("/api-keys/test-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUpdateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.keyId").value("test-key-001"));
    }

    @Test
    @DisplayName("API 키 수정 실패 - 존재하지 않는 키")
    void updateApiKey_NotFound() throws Exception {
        // given
        when(apiKeyService.updateApiKey(eq("non-existent-key"), any(ApiKey.class)))
                .thenThrow(new IllegalArgumentException("API key not found: non-existent-key"));

        // when & then
        mockMvc.perform(put("/api-keys/non-existent-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUpdateRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API 키 상태 변경 성공")
    void updateApiKeyStatus_Success() throws Exception {
        // given
        when(apiKeyService.updateApiKeyStatus("test-key-001", ApiKeyStatus.SUSPENDED))
                .thenReturn(testApiKey);

        // when & then
        mockMvc.perform(patch("/api-keys/test-key-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("API 키 삭제 성공")
    void deleteApiKey_Success() throws Exception {
        // given
        // deleteApiKey는 void를 반환하므로 doNothing 사용
        org.mockito.Mockito.doNothing().when(apiKeyService).deleteApiKey("test-key-001");

        // when & then
        mockMvc.perform(delete("/api-keys/test-key-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("활성 API 키 조회")
    void getActiveApiKeys_Success() throws Exception {
        // given
        List<ApiKey> activeKeys = Arrays.asList(testApiKey, testApiKey2);
        when(apiKeyService.getAllActiveApiKeys()).thenReturn(activeKeys);

        // when & then
        mockMvc.perform(get("/api-keys/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    private ApiKeyRegistrationRequest createRegistrationRequest() {
        ApiKeyRegistrationRequest request = new ApiKeyRegistrationRequest();
        request.setOrganizationName("테스트 조직");
        request.setOrganizationCode("ORG001");
        request.setContactEmail("test@example.com");
        request.setContactPhone("010-1234-5678");
        request.setApiServiceName("TEST_API");
        request.setApiServiceUrl("https://api.test.com");
        request.setApiKey("test-api-key-12345");
        request.setSecretKey("test-secret-key-67890");
        request.setDailyLimit(1000);
        request.setMonthlyLimit(30000);
        request.setExpiresAt(LocalDateTime.now().plusYears(1));
        request.setDescription("테스트용 API 키");
        request.setRequestedApis("test,api,service");
        return request;
    }

    private Object createInvalidRegistrationRequest() {
        return new Object() {
            public final String organizationName = null; // 유효성 검증 실패
            public final String contactEmail = "test@example.com";
            public final String apiServiceName = "TEST_API";
            public final String apiKey = "test-api-key-12345";
        };
    }

    private Object createUpdateRequest() {
        return new Object() {
            public final String description = "수정된 설명";
            public final Integer dailyLimit = 1500;
            public final Integer monthlyLimit = 45000;
        };
    }
}
