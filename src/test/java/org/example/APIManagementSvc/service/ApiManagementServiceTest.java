package org.example.APIManagementSvc.service;

import org.example.APIManagementSvc.domain.Entity.AiClassification;
import org.example.APIManagementSvc.domain.Entity.ApiKey;
import org.example.APIManagementSvc.domain.Entity.ApiParameter;
import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import org.example.APIManagementSvc.dto.externalapi.ExternalApiUpdateRequest;
import org.example.APIManagementSvc.repository.ApiParameterRepository;
import org.example.APIManagementSvc.repository.ExternalApiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.example.APIManagementSvc.dto.externalapi.ExternalApiRegisterRequest;
import org.example.APIManagementSvc.dto.externalapi.ApiParameterRegisterRequest;
import org.example.APIManagementSvc.dto.cache.ApiHealthStatusDto;
import org.example.APIManagementSvc.dto.ai.AiClassificationResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiManagementService 테스트")
class ApiManagementServiceTest {

    @Mock
    private ExternalApiService externalApiService;

    @Mock
    private ApiParameterService apiParameterService;

    @Mock
    private ExternalApiRepository externalApiRepository;

    @Mock
    private ApiParameterRepository apiParameterRepository;

    @Mock
    private AiClassificationService aiClassificationService;

    @Mock
    private ApiHealthCheckService apiHealthCheckService;

    @Mock
    private ApiTokenRefreshService apiTokenRefreshService;

    @Mock
    private ApiKeyService apiKeyService;

    @InjectMocks
    private ApiManagementService apiManagementService;

    private ExternalApi testApi;
    private ApiParameter testParameter1;
    private ApiParameter testParameter2;
    private List<ApiParameter> testParameters;
    private AiClassification testClassification;
    private ApiKey testApiKey;
    private ExternalApiUpdateRequest testUpdateRequest;

    @BeforeEach
    void setUp() {
        // ExternalApi 테스트 데이터
        testApi = new ExternalApi();
        testApi.setApiId("test-api-001");
        testApi.setApiName("테스트 API");
        testApi.setApiDescription("테스트용 API");
        testApi.setApiUrl("https://api.test.com");
        testApi.setHttpMethod("GET");
        testApi.setApiIssuer("테스트 조직");
        testApi.setApiOwner("테스트 팀");
        testApi.setApiDomain(null); // AI 분류를 위해 null로 설정
        testApi.setApiKeyword(null); // AI 분류를 위해 null로 설정
        testApi.setCreatedAt(LocalDateTime.now());
        testApi.setUpdatedAt(LocalDateTime.now());
        testApi.setDeleted(false);

        // ApiParameter 테스트 데이터
        testParameter1 = new ApiParameter();
        testParameter1.setParameterId("param-001");
        testParameter1.setApiId("test-api-001");
        testParameter1.setParamName("userId");
        testParameter1.setParamType("String");
        testParameter1.setIsRequired(true);
        testParameter1.setDefaultValue("");
        testParameter1.setCreatedAt(LocalDateTime.now());
        testParameter1.setUpdatedAt(LocalDateTime.now());

        testParameter2 = new ApiParameter();
        testParameter2.setParameterId("param-002");
        testParameter2.setApiId("test-api-001");
        testParameter2.setParamName("limit");
        testParameter2.setParamType("Integer");
        testParameter2.setIsRequired(false);
        testParameter2.setDefaultValue("10");
        testParameter2.setCreatedAt(LocalDateTime.now());
        testParameter2.setUpdatedAt(LocalDateTime.now());

        testParameters = Arrays.asList(testParameter1, testParameter2);

        // AiClassification 테스트 데이터
        testClassification = new AiClassification();
        testClassification.setClassificationId("classification-001");
        testClassification.setApiId("test-api-001");
        testClassification.setClassifiedDomain(ApiDomain.TECHNOLOGY);
        testClassification.setClassifiedKeyword(ApiKeyword.API_DOCUMENT);
        testClassification.setClassifiedAt(LocalDateTime.now());
        testClassification.setDeleted(false);

        // ApiKey 테스트 데이터
        testApiKey = new ApiKey();
        testApiKey.setKeyId("test-key-001");
        testApiKey.setOrganizationName("테스트 조직");
        testApiKey.setApiKey("test-api-key-12345");
        testApiKey.setStatus(ApiKeyStatus.ACTIVE);

        // ExternalApiUpdateRequest 테스트 데이터
        testUpdateRequest = new ExternalApiUpdateRequest();
        testUpdateRequest.setApiName("수정된 API");
        testUpdateRequest.setApiDescription("수정된 설명");
        testUpdateRequest.setApiUrl("https://api.updated.com");
        testUpdateRequest.setHttpMethod("POST");
        testUpdateRequest.setApiIssuer("수정된 조직");
        testUpdateRequest.setApiEffectiveness(true);
    }

    @Test
    @DisplayName("API와 파라미터를 함께 등록 성공 - 인증 정보 포함")
    void registerApiWithParametersAndAuth_Success() {
        // given
        String apiKey = "test-api-key";
        String apiToken = "test-token";
        
        when(aiClassificationService.classifyApi(anyString(), anyString(), anyString(), anyString(), isNull()))
                .thenReturn(testClassification);
        when(externalApiService.registerApiWithAuth(any(ExternalApi.class), eq(apiKey), eq(apiToken)))
                .thenReturn(testApi);
        when(apiParameterService.saveParameter(any(ApiParameter.class)))
                .thenReturn(testParameter1)
                .thenReturn(testParameter2);
        when(apiHealthCheckService.checkApiHealth(any(ExternalApi.class))).thenReturn(
                org.example.APIManagementSvc.dto.cache.ApiHealthStatusDto.builder()
                        .apiId("test-api-001")
                        .status("HEALTHY")
                        .responseTime(100L)
                        .httpStatus(200)
                        .checkedAt(LocalDateTime.now())
                        .build());

        // when
        ExternalApi result = apiManagementService.registerApiWithParametersAndAuth(testApi, testParameters, apiKey, apiToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getApiId()).isEqualTo("test-api-001");
        
        verify(aiClassificationService).classifyApi(anyString(), anyString(), anyString(), anyString(), isNull());
        verify(externalApiService).registerApiWithAuth(any(ExternalApi.class), eq(apiKey), eq(apiToken));
        verify(apiParameterService, times(2)).saveParameter(any(ApiParameter.class));
        verify(apiHealthCheckService).checkApiHealth(any(ExternalApi.class));
    }

    @Test
    @DisplayName("API와 파라미터를 함께 등록 성공 - 기본")
    void registerApiWithParameters_Success() {
        // given
        when(aiClassificationService.classifyApi(anyString(), anyString(), anyString(), anyString(), isNull()))
                .thenReturn(testClassification);
        when(externalApiService.registerApi(any(ExternalApi.class))).thenReturn(testApi);
        when(apiParameterService.saveParameter(any(ApiParameter.class)))
                .thenReturn(testParameter1)
                .thenReturn(testParameter2);

        // when
        ExternalApi result = apiManagementService.registerApiWithParameters(testApi, testParameters);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getApiId()).isEqualTo("test-api-001");
        
        verify(aiClassificationService).classifyApi(anyString(), anyString(), anyString(), anyString(), isNull());
        verify(externalApiService).registerApi(any(ExternalApi.class));
        verify(apiParameterService, times(2)).saveParameter(any(ApiParameter.class));
    }

    @Test
    @DisplayName("API 등록 성공 - 파라미터 없이")
    void registerApiWithParameters_Success_NoParameters() {
        // given
        when(aiClassificationService.classifyApi(anyString(), anyString(), anyString(), anyString(), isNull()))
                .thenReturn(testClassification);
        when(externalApiService.registerApi(any(ExternalApi.class))).thenReturn(testApi);

        // when
        ExternalApi result = apiManagementService.registerApiWithParameters(testApi, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getApiId()).isEqualTo("test-api-001");
        
        verify(aiClassificationService).classifyApi(anyString(), anyString(), anyString(), anyString(), isNull());
        verify(externalApiService).registerApi(any(ExternalApi.class));
        verify(apiParameterService, never()).saveParameter(any(ApiParameter.class));
    }

    @Test
    @DisplayName("API 등록 성공 - 도메인과 키워드가 이미 설정된 경우")
    void registerApiWithParameters_Success_DomainAndKeywordAlreadySet() {
        // given
        ExternalApi apiWithDomainKeyword = new ExternalApi();
        apiWithDomainKeyword.setApiId("test-api-002");
        apiWithDomainKeyword.setApiName("설정된 API");
        apiWithDomainKeyword.setApiDescription("이미 분류된 API");
        apiWithDomainKeyword.setApiUrl("https://api.finance.com");
        apiWithDomainKeyword.setHttpMethod("GET");
        apiWithDomainKeyword.setApiDomain(ApiDomain.FINANCE);
        apiWithDomainKeyword.setApiKeyword(ApiKeyword.STOCK_PRICE);
        
        when(externalApiService.registerApi(any(ExternalApi.class))).thenReturn(apiWithDomainKeyword);

        // when
        ExternalApi result = apiManagementService.registerApiWithParameters(apiWithDomainKeyword, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getApiDomain()).isEqualTo(ApiDomain.FINANCE);
        assertThat(result.getApiKeyword()).isEqualTo(ApiKeyword.STOCK_PRICE);
        
        verify(aiClassificationService, never()).classifyApi(anyString(), anyString(), anyString(), anyString(), isNull());
        verify(externalApiService).registerApi(any(ExternalApi.class));
    }

    @Test
    @DisplayName("API 등록 성공 - AI 분류 실패 시 기본값 사용")
    void registerApiWithParameters_Success_WithDefaultValuesWhenAIClassificationFailed() {
        // given
        when(aiClassificationService.classifyApi(anyString(), anyString(), anyString(), anyString(), isNull()))
                .thenThrow(new RuntimeException("AI 분류 실패"));
        when(externalApiService.registerApi(any(ExternalApi.class))).thenReturn(testApi);

        // when
        ExternalApi result = apiManagementService.registerApiWithParameters(testApi, testParameters);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getApiDomain()).isEqualTo(ApiDomain.OTHERS);
        assertThat(result.getApiKeyword()).isEqualTo(ApiKeyword.API_DOCUMENT);
        
        verify(externalApiService).registerApi(any(ExternalApi.class));
        verify(apiParameterService, times(2)).saveParameter(any(ApiParameter.class));
    }

    @Test
    @DisplayName("API와 파라미터 함께 조회")
    void getApiWithParameters() {
        // given
        String apiId = "test-api-001";
        ExternalApi testApi = new ExternalApi();
        testApi.setApiId(apiId);
        testApi.setApiName("테스트 API");
        
        when(externalApiService.getApiById(apiId)).thenReturn(Optional.of(testApi));
        when(apiParameterService.getParametersByApiId(apiId)).thenReturn(testParameters);

        // when
        Object result = apiManagementService.getApiWithParameters(apiId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(ApiManagementService.ApiWithParameters.class);
        
        ApiManagementService.ApiWithParameters apiWithParams = (ApiManagementService.ApiWithParameters) result;
        assertThat(apiWithParams.getApi()).isEqualTo(testApi);
        assertThat(apiWithParams.getParameters()).hasSize(2);
        
        verify(externalApiService).getApiById(apiId);
        verify(apiParameterService).getParametersByApiId(apiId);
    }

    @Test
    @DisplayName("API와 파라미터 함께 조회 실패 - API 없음")
    void getApiWithParameters_Failure_ApiNotFound() {
        // given
        String apiId = "non-existent-api";
        when(externalApiService.getApiById(apiId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> apiManagementService.getApiWithParameters(apiId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API not found");
        
        verify(externalApiService).getApiById(apiId);
        verify(apiParameterService, never()).getParametersByApiId(anyString());
    }

    @Test
    @DisplayName("API와 파라미터 함께 수정 성공")
    void updateApiWithParameters_Success() {
        // given
        String apiId = "test-api-001";
        ExternalApi updatedApi = new ExternalApi();
        updatedApi.setApiId(apiId);
        updatedApi.setApiName("수정된 API");
        
        when(externalApiService.updateApi(eq(apiId), any(ExternalApi.class))).thenReturn(updatedApi);
        when(apiParameterService.saveParameter(any(ApiParameter.class)))
                .thenReturn(testParameter1)
                .thenReturn(testParameter2);
        doNothing().when(apiParameterService).deleteAllParametersByApiId(apiId);

        // when
        ExternalApi result = apiManagementService.updateApiWithParameters(apiId, testUpdateRequest, testParameters);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getApiName()).isEqualTo("수정된 API");
        
        verify(externalApiService).updateApi(eq(apiId), any(ExternalApi.class));
        verify(apiParameterService).deleteAllParametersByApiId(apiId);
        verify(apiParameterService, times(2)).saveParameter(any(ApiParameter.class));
    }

    @Test
    @DisplayName("API와 파라미터 함께 수정 실패 - API 수정 실패")
    void updateApiWithParameters_Failure_ApiUpdateFailed() {
        // given
        String apiId = "test-api-001";
        when(externalApiService.updateApi(eq(apiId), any(ExternalApi.class)))
                .thenThrow(new RuntimeException("API 수정 실패"));

        // when & then
        assertThatThrownBy(() -> apiManagementService.updateApiWithParameters(apiId, testUpdateRequest, testParameters))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API update failed");
        
        verify(apiParameterService, never()).deleteAllParametersByApiId(anyString());
        verify(apiParameterService, never()).saveParameter(any(ApiParameter.class));
    }

    @Test
    @DisplayName("API와 파라미터 함께 삭제 성공")
    void deleteApiWithParameters_Success() {
        // given
        String apiId = "test-api-001";
        doNothing().when(apiParameterService).deleteAllParametersByApiId(apiId);
        doNothing().when(externalApiService).deleteApi(apiId);

        // when
        apiManagementService.deleteApiWithParameters(apiId);

        // then
        verify(apiParameterService).deleteAllParametersByApiId(apiId);
        verify(externalApiService).deleteApi(apiId);
    }

    @Test
    @DisplayName("API와 파라미터 함께 삭제 실패 - 파라미터 삭제 실패")
    void deleteApiWithParameters_Failure_ParameterDeletionFailed() {
        // given
        String apiId = "test-api-001";
        doThrow(new RuntimeException("파라미터 삭제 실패"))
                .when(apiParameterService).deleteAllParametersByApiId(apiId);

        // when & then
        assertThatThrownBy(() -> apiManagementService.deleteApiWithParameters(apiId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API deletion failed");
        
        verify(externalApiService, never()).deleteApi(anyString());
    }

    @Test
    @DisplayName("API 유효성 검증 성공 - 유효한 API")
    void validateApi_Success_ValidApi() {
        // given
        String apiId = "test-api-001";
        when(externalApiService.getApiById(apiId)).thenReturn(Optional.of(testApi));
        when(apiParameterService.getParametersByApiId(apiId)).thenReturn(testParameters);

        // when
        ApiManagementService.ApiValidationResult result = apiManagementService.validateApi(apiId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorMessage()).isEmpty();
        assertThat(result.getApi()).isEqualTo(testApi);
        assertThat(result.getParameters()).hasSize(2);
        
        verify(externalApiService).getApiById(apiId);
        verify(apiParameterService).getParametersByApiId(apiId);
    }

    @Test
    @DisplayName("API 유효성 검증 실패 - URL 누락")
    void validateApi_Failure_MissingUrl() {
        // given
        String apiId = "test-api-001";
        testApi.setApiUrl(null);
        
        when(externalApiService.getApiById(apiId)).thenReturn(Optional.of(testApi));
        when(apiParameterService.getParametersByApiId(apiId)).thenReturn(testParameters);

        // when
        ApiManagementService.ApiValidationResult result = apiManagementService.validateApi(apiId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("API URL is required");
    }

    @Test
    @DisplayName("API 유효성 검증 실패 - HTTP 메소드 누락")
    void validateApi_Failure_MissingHttpMethod() {
        // given
        String apiId = "test-api-001";
        testApi.setHttpMethod("");
        
        when(externalApiService.getApiById(apiId)).thenReturn(Optional.of(testApi));
        when(apiParameterService.getParametersByApiId(apiId)).thenReturn(testParameters);

        // when
        ApiManagementService.ApiValidationResult result = apiManagementService.validateApi(apiId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("HTTP Method is required");
    }

    @Test
    @DisplayName("API 유효성 검증 실패 - API 없음")
    void validateApi_Failure_ApiNotFound() {
        // given
        String apiId = "non-existent-api";
        when(externalApiService.getApiById(apiId)).thenReturn(Optional.empty());

        // when
        ApiManagementService.ApiValidationResult result = apiManagementService.validateApi(apiId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Validation failed");
    }

    @Test
    @DisplayName("API 복사 성공")
    void copyApi_Success() {
        // given
        String originalApiId = "original-api-001";
        String newApiName = "복사된 API";
        
        when(externalApiService.getApiById(originalApiId)).thenReturn(Optional.of(testApi));
        when(externalApiService.registerApi(any(ExternalApi.class))).thenReturn(testApi);
        when(apiParameterService.getParametersByApiId(originalApiId)).thenReturn(testParameters);
        when(apiParameterService.saveParameter(any(ApiParameter.class)))
                .thenReturn(testParameter1)
                .thenReturn(testParameter2);

        // when
        ExternalApi result = apiManagementService.copyApi(originalApiId, newApiName);

        // then
        assertThat(result).isNotNull();
        
        verify(externalApiService).getApiById(originalApiId);
        verify(externalApiService).registerApi(any(ExternalApi.class));
        verify(apiParameterService).getParametersByApiId(originalApiId);
        verify(apiParameterService, times(2)).saveParameter(any(ApiParameter.class));
    }

    @Test
    @DisplayName("API 복사 실패 - 원본 API 없음")
    void copyApi_Failure_OriginalApiNotFound() {
        // given
        String originalApiId = "non-existent-api";
        String newApiName = "복사된 API";
        
        when(externalApiService.getApiById(originalApiId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> apiManagementService.copyApi(originalApiId, newApiName))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API copy failed");
        
        verify(externalApiService, never()).registerApi(any(ExternalApi.class));
    }

    @Test
    @DisplayName("복합 검색 - 도메인과 키워드 조합")
    void searchApisWithParameters_DomainAndKeyword() {
        // given
        String domain = "TECHNOLOGY";
        String keyword = "API_DOCUMENT";
        String searchTerm = null;
        ExternalApi searchApi = new ExternalApi();
        searchApi.setApiId("search-api-001");
        searchApi.setApiName("검색 API");
        searchApi.setApiDomain(ApiDomain.TECHNOLOGY);
        searchApi.setApiKeyword(ApiKeyword.API_DOCUMENT);
        
        List<ExternalApi> apis = Arrays.asList(searchApi);
        
        // repository mock 설정
        when(externalApiRepository.findByApiDomainAndApiKeywordAndDeletedFalse(ApiDomain.TECHNOLOGY, ApiKeyword.API_DOCUMENT))
                .thenReturn(apis);
        when(apiParameterService.getParametersByApiId(searchApi.getApiId())).thenReturn(testParameters);

        // when
        List<ApiManagementService.ApiWithParameters> result = apiManagementService.searchApisWithParameters(domain, keyword, searchTerm);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        
        // verification
        verify(externalApiRepository).findByApiDomainAndApiKeywordAndDeletedFalse(ApiDomain.TECHNOLOGY, ApiKeyword.API_DOCUMENT);
        verify(apiParameterService).getParametersByApiId(searchApi.getApiId());
    }

    @Test
    @DisplayName("복합 검색 - 도메인만")
    void searchApisWithParameters_DomainOnly() {
        // given
        String domain = "TECHNOLOGY";
        String keyword = null;
        String searchTerm = null;
        ExternalApi techApi = new ExternalApi();
        techApi.setApiId("tech-api-001");
        techApi.setApiName("기술 API");
        techApi.setApiDomain(ApiDomain.TECHNOLOGY);
        
        List<ExternalApi> apis = Arrays.asList(techApi);
        
        // repository mock 설정
        when(externalApiRepository.findByApiDomainAndDeletedFalse(ApiDomain.TECHNOLOGY))
                .thenReturn(apis);
        when(apiParameterService.getParametersByApiId(techApi.getApiId())).thenReturn(testParameters);

        // when
        List<ApiManagementService.ApiWithParameters> result = apiManagementService.searchApisWithParameters(domain, keyword, searchTerm);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        
        // verification
        verify(externalApiRepository).findByApiDomainAndDeletedFalse(ApiDomain.TECHNOLOGY);
        verify(apiParameterService).getParametersByApiId(techApi.getApiId());
    }

    @Test
    @DisplayName("복합 검색 - 키워드만")
    void searchApisWithParameters_KeywordOnly() {
        // given
        String domain = null;
        String keyword = "API_DOCUMENT";
        String searchTerm = null;
        ExternalApi docApi = new ExternalApi();
        docApi.setApiId("doc-api-001");
        docApi.setApiName("문서 API");
        docApi.setApiKeyword(ApiKeyword.API_DOCUMENT);
        
        List<ExternalApi> apis = Arrays.asList(docApi);
        
        // repository mock 설정
        when(externalApiRepository.findByApiKeywordAndDeletedFalse(ApiKeyword.API_DOCUMENT))
                .thenReturn(apis);
        when(apiParameterService.getParametersByApiId(docApi.getApiId())).thenReturn(testParameters);

        // when
        List<ApiManagementService.ApiWithParameters> result = apiManagementService.searchApisWithParameters(domain, keyword, searchTerm);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        
        // verification
        verify(externalApiRepository).findByApiKeywordAndDeletedFalse(ApiKeyword.API_DOCUMENT);
        verify(apiParameterService).getParametersByApiId(docApi.getApiId());
    }

    @Test
    @DisplayName("복합 검색 - 검색어만")
    void searchApisWithParameters_SearchTermOnly() {
        // given
        String domain = null;
        String keyword = null;
        String searchTerm = "테스트";
        ExternalApi searchApi = new ExternalApi();
        searchApi.setApiId("search-api-001");
        searchApi.setApiName("테스트 검색 API");
        searchApi.setApiDescription("테스트용 API입니다");
        
        List<ExternalApi> apis = Arrays.asList(searchApi);
        
        when(externalApiService.searchApis(searchTerm)).thenReturn(apis);
        when(apiParameterService.getParametersByApiId(searchApi.getApiId())).thenReturn(testParameters);

        // when
        List<ApiManagementService.ApiWithParameters> result = apiManagementService.searchApisWithParameters(domain, keyword, searchTerm);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        
        verify(externalApiService).searchApis(searchTerm);
        verify(apiParameterService).getParametersByApiId(searchApi.getApiId());
    }

    @Test
    @DisplayName("복합 검색 - 도메인과 검색어 조합")
    void searchApisWithParameters_DomainAndSearchTerm() {
        // given
        String domain = "TECHNOLOGY";
        String keyword = null;
        String searchTerm = "API";
        ExternalApi techApi = new ExternalApi();
        techApi.setApiId("tech-api-001");
        techApi.setApiName("기술 API");
        techApi.setApiDomain(ApiDomain.TECHNOLOGY);
        techApi.setApiDescription("기술 관련 API입니다");
        
        List<ExternalApi> apis = Arrays.asList(techApi);
        
        // repository mock 설정
        when(externalApiRepository.findByApiDomainAndDeletedFalse(ApiDomain.TECHNOLOGY))
                .thenReturn(apis);
        when(apiParameterService.getParametersByApiId(techApi.getApiId())).thenReturn(testParameters);

        // when
        List<ApiManagementService.ApiWithParameters> result = apiManagementService.searchApisWithParameters(domain, keyword, searchTerm);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        
        // verification
        verify(externalApiRepository).findByApiDomainAndDeletedFalse(ApiDomain.TECHNOLOGY);
        verify(apiParameterService).getParametersByApiId(techApi.getApiId());
    }

    @Test
    @DisplayName("API 키 연결 성공")
    void linkApiKey_Success() {
        // given
        String apiId = "test-api-001";
        String apiKeyId = "test-key-001";
        
        when(externalApiService.getApiById(apiId)).thenReturn(Optional.of(testApi));
        when(apiKeyService.getApiKey(apiKeyId)).thenReturn(Optional.of(testApiKey));
        when(externalApiRepository.save(any(ExternalApi.class))).thenReturn(testApi);

        // when
        ExternalApi result = apiManagementService.linkApiKey(apiId, apiKeyId);

        // then
        assertThat(result).isNotNull();
        
        verify(externalApiService).getApiById(apiId);
        verify(apiKeyService).getApiKey(apiKeyId);
        verify(externalApiRepository).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("API 키 연결 실패 - API 없음")
    void linkApiKey_Failure_ApiNotFound() {
        // given
        String apiId = "non-existent-api";
        String apiKeyId = "test-key-001";
        
        when(externalApiService.getApiById(apiId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> apiManagementService.linkApiKey(apiId, apiKeyId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API 키 연결 실패");
        
        verify(apiKeyService, never()).getApiKey(anyString());
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("API 키 연결 실패 - API 키 없음")
    void linkApiKey_Failure_ApiKeyNotFound() {
        // given
        String apiId = "test-api-001";
        String apiKeyId = "non-existent-key";
        
        when(externalApiService.getApiById(apiId)).thenReturn(Optional.of(testApi));
        when(apiKeyService.getApiKey(apiKeyId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> apiManagementService.linkApiKey(apiId, apiKeyId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API 키 연결 실패");
        
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("API 키 연결 실패 - 비활성 API 키")
    void linkApiKey_Failure_InactiveApiKey() {
        // given
        String apiId = "test-api-001";
        String apiKeyId = "test-key-001";
        testApiKey.setStatus(ApiKeyStatus.INACTIVE);
        
        when(externalApiService.getApiById(apiId)).thenReturn(Optional.of(testApi));
        when(apiKeyService.getApiKey(apiKeyId)).thenReturn(Optional.of(testApiKey));

        // when & then
        assertThatThrownBy(() -> apiManagementService.linkApiKey(apiId, apiKeyId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API 키 연결 실패");
        
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("API 키 연결 해제 성공")
    void unlinkApiKey_Success() {
        // given
        String apiId = "test-api-001";
        
        when(externalApiService.getApiById(apiId)).thenReturn(Optional.of(testApi));
        when(externalApiRepository.save(any(ExternalApi.class))).thenReturn(testApi);

        // when
        ExternalApi result = apiManagementService.unlinkApiKey(apiId);

        // then
        assertThat(result).isNotNull();
        
        verify(externalApiService).getApiById(apiId);
        verify(externalApiRepository).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("API 키 연결 해제 실패 - API 없음")
    void unlinkApiKey_Failure_ApiNotFound() {
        // given
        String apiId = "non-existent-api";
        
        when(externalApiService.getApiById(apiId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> apiManagementService.unlinkApiKey(apiId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API 키 연결 해제 실패");
        
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("ExternalApiRegisterRequest로 API 등록 성공")
    void registerApiWithParametersAndAuth_FromRequest_Success() {
        // given
        ExternalApiRegisterRequest request = ExternalApiRegisterRequest.builder()
                .apiName("테스트 API")
                .apiUrl("https://api.test.com/test")
                .apiIssuer("테스트 기관")
                .apiOwner("테스트팀")
                .httpMethod("GET")
                .apiDescription("테스트용 API")
                .parameters(Arrays.asList(
                        ApiParameterRegisterRequest.builder()
                                .paramName("city")
                                .paramType("STRING")
                                .isRequired(true)
                                .defaultValue("Seoul")
                                .description("도시명")
                                .build(),
                        ApiParameterRegisterRequest.builder()
                                .paramName("year")
                                .paramType("INTEGER")
                                .isRequired(false)
                                .defaultValue("2024")
                                .description("연도")
                                .build()
                ))
                .apiToken("test_token_12345")
                .autoTokenRefresh(true)
                .build();

        AiClassification classification = new AiClassification();
        classification.setClassifiedDomain(ApiDomain.WEATHER);
        classification.setClassifiedKeyword(ApiKeyword.CURRENT_WEATHER);
        
        when(aiClassificationService.classifyApi(anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(classification);

        // testApi에 AI 분류 결과와 토큰을 설정
        testApi.setApiDomain(ApiDomain.WEATHER);
        testApi.setApiKeyword(ApiKeyword.CURRENT_WEATHER);
        testApi.setApiToken("test_token_12345");
        
        doReturn(testApi).when(externalApiService).registerApiWithAuth(any(ExternalApi.class), isNull(), anyString());

        when(apiParameterService.saveParameter(any(ApiParameter.class)))
                .thenReturn(testParameter1)
                .thenReturn(testParameter2);

        ApiHealthStatusDto healthStatus = ApiHealthStatusDto.builder()
                .apiId(testApi.getApiId())
                .status("HEALTHY")
                .responseTime(100L)
                .checkedAt(LocalDateTime.now())
                .build();
                
        when(apiHealthCheckService.checkApiHealth(any(ExternalApi.class)))
                .thenReturn(healthStatus);

        // when
        ExternalApi result = apiManagementService.registerApiWithParametersAndAuth(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getApiName()).isEqualTo("테스트 API");
        assertThat(result.getApiDomain()).isEqualTo(ApiDomain.WEATHER);
        assertThat(result.getApiKeyword()).isEqualTo(ApiKeyword.CURRENT_WEATHER);
        assertThat(result.getApiToken()).isEqualTo("test_token_12345");
        assertThat(result.getAutoTokenRefresh()).isTrue();

        // 파라미터가 올바르게 등록되었는지 확인
        verify(apiParameterService, times(2)).saveParameter(any(ApiParameter.class));
        
        // AI 분류가 호출되었는지 확인
        verify(aiClassificationService).classifyApi(anyString(), anyString(), anyString(), anyString(), any());
        
        // 헬스체크가 호출되었는지 확인
        verify(apiHealthCheckService).checkApiHealth(any(ExternalApi.class));
    }

    @Test
    @DisplayName("ExternalApiRegisterRequest로 API 등록 실패 - AI 분류 실패")
    void registerApiWithParametersAndAuth_FromRequest_Failure_AIClassificationFailed() {
        // given
        ExternalApiRegisterRequest request = ExternalApiRegisterRequest.builder()
                .apiName("테스트 API")
                .apiUrl("https://api.test.com/test")
                .apiIssuer("테스트 기관")
                .apiOwner("테스트팀")
                .httpMethod("GET")
                .apiDescription("테스트용 API")
                .parameters(Arrays.asList(
                        ApiParameterRegisterRequest.builder()
                                .paramName("city")
                                .paramType("STRING")
                                .isRequired(true)
                                .defaultValue("Seoul")
                                .description("도시명")
                                .build()
                ))
                .apiToken("test_token_12345")
                .build();

        // AI 분류 실패 시뮬레이션
        when(aiClassificationService.classifyApi(anyString(), anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("AI 분류 실패"));
        
        // testApi에 기본값 설정
        testApi.setApiDomain(ApiDomain.OTHERS);
        testApi.setApiKeyword(ApiKeyword.API_DOCUMENT);
        
        // 기본값으로 API 등록 성공
        when(externalApiService.registerApiWithAuth(any(ExternalApi.class), isNull(), anyString()))
                .thenReturn(testApi);

        // when
        ExternalApi result = apiManagementService.registerApiWithParametersAndAuth(request);

        // then
        assertThat(result).isNotNull();
        // AI 분류 실패 시 기본값이 설정되었는지 확인
        assertThat(result.getApiDomain()).isEqualTo(ApiDomain.OTHERS);
        assertThat(result.getApiKeyword()).isEqualTo(ApiKeyword.API_DOCUMENT);
        
        verify(aiClassificationService).classifyApi(anyString(), anyString(), anyString(), anyString(), any());
        verify(externalApiService).registerApiWithAuth(any(ExternalApi.class), isNull(), anyString());
    }
}
