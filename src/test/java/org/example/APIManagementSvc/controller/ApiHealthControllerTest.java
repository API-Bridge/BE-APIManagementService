package org.example.APIManagementSvc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.dto.cache.ApiHealthStatusDto;
import org.example.APIManagementSvc.service.ApiHealthCheckService;
import org.example.APIManagementSvc.service.ApiManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ApiHealthController 테스트 클래스
 */
@DisplayName("ApiHealthController 테스트")
class ApiHealthControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ApiHealthCheckService apiHealthCheckService;

    @Mock
    private ApiManagementService apiManagementService;

    private ExternalApi testApi;
    private ApiHealthStatusDto testHealthStatus;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        ApiHealthController controller = new ApiHealthController(apiHealthCheckService, apiManagementService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
        
        objectMapper = new ObjectMapper();

        // 테스트용 API 설정
        testApi = new ExternalApi();
        testApi.setApiId("test-api-001");
        testApi.setApiName("테스트 API");
        testApi.setApiUrl("https://api.test.com/health");
        testApi.setHttpMethod("GET");
        testApi.setApiDomain(org.example.APIManagementSvc.domain.enums.ApiDomain.WEATHER);
        testApi.setApiKeyword(org.example.APIManagementSvc.domain.enums.ApiKeyword.CURRENT_WEATHER);
        testApi.setApiIssuer("테스트 기관");
        testApi.setApiEffectiveness(true);
        testApi.setDeleted(false);

        // 테스트용 헬스체크 상태 설정
        testHealthStatus = ApiHealthStatusDto.builder()
                .apiId("test-api-001")
                .status("HEALTHY")
                .responseTime(150)
                .httpStatus(200)
                .checkedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("특정 API 헬스체크 강제 갱신")
    void refreshApiHealth() throws Exception {
        // given
        when(apiManagementService.getApiById("test-api-001"))
                .thenReturn(java.util.Optional.of(testApi));
        when(apiHealthCheckService.getApiHealthStatus("test-api-001"))
                .thenReturn(testHealthStatus);

        // when & then
        mockMvc.perform(post("/api-health/refresh/{apiId}", "test-api-001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.apiId").value("test-api-001"));
    }

    @Test
    @DisplayName("API 헬스체크 상태 조회")
    void getApiHealthStatus() throws Exception {
        // given
        when(apiHealthCheckService.getApiHealthStatus("test-api-001"))
                .thenReturn(testHealthStatus);

        // when & then
        mockMvc.perform(get("/api-health/status/{apiId}", "test-api-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.apiId").value("test-api-001"));
    }

    @Test
    @DisplayName("전체 API 헬스체크 일괄 수행")
    void checkAllApisHealth() throws Exception {
        // given
        List<ExternalApi> apis = Arrays.asList(testApi);
        when(externalApiService.getAllActiveApis()).thenReturn(apis);

        // when & then
        mockMvc.perform(post("/api-health/check-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("도메인별 헬스 상태 요약 조회")
    void getHealthSummaryByDomain() throws Exception {
        // given
        List<ExternalApi> apis = Arrays.asList(testApi);
        when(apiManagementService.searchApis(contains("도메인:WEATHER")))
                .thenReturn(apis);
        when(apiHealthCheckService.getApiHealthStatus("test-api-001"))
                .thenReturn(testHealthStatus);

        // when & then
        mockMvc.perform(get("/api-health/summary/domain/WEATHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.domain").value("WEATHER"));
    }

    @Test
    @DisplayName("전체 API 헬스체크 결과 조회 (페이징)")
    void getAllApiHealthStatus() throws Exception {
        // given
        List<ExternalApi> apis = Arrays.asList(testApi);
        when(apiManagementService.getAllActiveApis()).thenReturn(apis);
        when(apiHealthCheckService.getApiHealthStatus("test-api-001"))
                .thenReturn(testHealthStatus);

        // when & then
        mockMvc.perform(get("/api-health/health/status/all")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("사용 불가능한 API 목록 조회")
    void getUnavailableApis() throws Exception {
        // given
        List<String> unavailableApis = Arrays.asList("test-api-001", "test-api-002");
        when(apiHealthCheckService.getUnavailableApisFromCache())
                .thenReturn(unavailableApis);

        // when & then
        mockMvc.perform(get("/api-health/health/unavailable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("사용 불가능한 API 개수 조회")
    void getUnavailableApisCount() throws Exception {
        // given
        when(apiHealthCheckService.getUnavailableApisCount()).thenReturn(3);

        // when & then
        mockMvc.perform(get("/api-health/health/unavailable/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    @DisplayName("특정 API 사용 불가능 여부 확인")
    void isApiUnavailable() throws Exception {
        // given
        when(apiHealthCheckService.isApiUnavailable("test-api-001")).thenReturn(true);

        // when & then
        mockMvc.perform(get("/api-health/health/unavailable/{apiId}", "test-api-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("사용 불가능한 API 목록 캐시 강제 갱신")
    void refreshUnavailableApisCache() throws Exception {
        // given
        List<ExternalApi> apis = Arrays.asList(testApi);
        when(apiManagementService.getAllActiveApis()).thenReturn(apis);

        // when & then
        mockMvc.perform(post("/api-health/health/unavailable/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("사용 불가능한 API 목록 캐시가 성공적으로 갱신되었습니다."));
    }

    @Test
    @DisplayName("API 헬스체크 통계 조회")
    void getHealthStatistics() throws Exception {
        // given
        when(apiHealthCheckService.getUnavailableApisCount()).thenReturn(3);
        when(apiHealthCheckService.getUnavailableApisFromCache())
                .thenReturn(Arrays.asList("test-api-001", "test-api-002", "test-api-003"));

        // when & then
        mockMvc.perform(get("/api-health/health/unavailable/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(3));
    }
}
