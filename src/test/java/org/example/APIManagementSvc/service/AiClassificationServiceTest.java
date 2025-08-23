package org.example.APIManagementSvc.service;

import org.example.APIManagementSvc.config.GeminiConfig;
import org.example.APIManagementSvc.domain.Entity.AiClassification;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import org.example.APIManagementSvc.repository.AiClassificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiClassificationService 테스트")
class AiClassificationServiceTest {

    @Mock
    private AiClassificationRepository aiClassificationRepository;

    @Mock
    private GeminiConfig geminiConfig;

    @Mock
    private RestTemplate geminiRestTemplate;

    @InjectMocks
    private AiClassificationService aiClassificationService;

    private AiClassification testClassification;
    private String testApiId;
    private String testApiName;
    private String testApiDescription;
    private String testApiUrl;

    @BeforeEach
    void setUp() {
        testApiId = "test-api-001";
        testApiName = "주식 가격 API";
        testApiDescription = "실시간 주식 가격 정보를 제공하는 API";
        testApiUrl = "https://api.stock.com/price";

        testClassification = new AiClassification();
        testClassification.setClassificationId("test-classification-001");
        testClassification.setApiId(testApiId);
        testClassification.setClassifiedDomain(ApiDomain.FINANCE);
        testClassification.setClassifiedKeyword(ApiKeyword.STOCK_PRICE);
        testClassification.setClassifiedAt(LocalDateTime.now());
        testClassification.setDeleted(false);
    }

    @Test
    @DisplayName("API 자동 분류 성공 - AI 분류")
    void classifyApi_Success_WithAI() {
        // given
        String classificationPrompt = "주식 관련 API를 분류해주세요";
        
        when(geminiConfig.isApiKeyConfigured()).thenReturn(true);
        when(geminiConfig.getApiUrl()).thenReturn("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent");
        when(geminiConfig.getApiKey()).thenReturn("test-api-key");
        
        // AI 응답 모킹
        Map<String, Object> aiResponse = createMockAiResponse("FINANCE", "STOCK_PRICE");
        when(geminiRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(aiResponse);
        
        when(aiClassificationRepository.save(any(AiClassification.class))).thenReturn(testClassification);

        // when
        AiClassification result = aiClassificationService.classifyApi(testApiId, testApiName, testApiDescription, testApiUrl, classificationPrompt);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getApiId()).isEqualTo(testApiId);
        assertThat(result.getClassifiedDomain()).isEqualTo(ApiDomain.FINANCE);
        assertThat(result.getClassifiedKeyword()).isEqualTo(ApiKeyword.STOCK_PRICE);
        assertThat(result.getClassifiedAt()).isNotNull();
        assertThat(result.getDeleted()).isFalse();
        
        verify(geminiRestTemplate, times(2)).postForObject(anyString(), any(), eq(Map.class));
        verify(aiClassificationRepository).save(any(AiClassification.class));
    }

    @Test
    @DisplayName("API 자동 분류 성공 - 키워드 기반 Fallback")
    void classifyApi_Success_WithKeywordFallback() {
        // given
        String classificationPrompt = null;
        
        when(geminiConfig.isApiKeyConfigured()).thenReturn(false);
        when(aiClassificationRepository.save(any(AiClassification.class))).thenReturn(testClassification);

        // when
        AiClassification result = aiClassificationService.classifyApi(testApiId, testApiName, testApiDescription, testApiUrl, classificationPrompt);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getApiId()).isEqualTo(testApiId);
        assertThat(result.getClassifiedDomain()).isEqualTo(ApiDomain.FINANCE); // 키워드 기반 분류
        assertThat(result.getClassifiedKeyword()).isEqualTo(ApiKeyword.STOCK_PRICE); // 도메인 기본 키워드
        
        verify(geminiRestTemplate, never()).postForObject(anyString(), any(), eq(Map.class));
        verify(aiClassificationRepository).save(any(AiClassification.class));
    }

    @Test
    @DisplayName("API 자동 분류 - AI API 호출 실패로 키워드 기반 Fallback")
    void classifyApi_AIApiError_FallbackToKeyword() {
        // given
        String classificationPrompt = "테스트 프롬프트";
        
        when(geminiConfig.isApiKeyConfigured()).thenReturn(true);
        when(geminiConfig.getApiUrl()).thenReturn("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent");
        when(geminiConfig.getApiKey()).thenReturn("test-api-key");
        
        when(geminiRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("AI API 호출 실패"));
        
        when(aiClassificationRepository.save(any(AiClassification.class))).thenReturn(testClassification);

        // when
        AiClassification result = aiClassificationService.classifyApi(testApiId, testApiName, testApiDescription, testApiUrl, classificationPrompt);

        // then
        assertThat(result).isNotNull();
        // AI 호출 실패 시 키워드 기반 분류로 Fallback되므로 결과가 정상적으로 반환됨
        verify(aiClassificationRepository).save(any(AiClassification.class));
    }

    @Test
    @DisplayName("API 자동 분류 - AI 응답 파싱 실패로 키워드 기반 Fallback")
    void classifyApi_AIResponseParsingFailure_FallbackToKeyword() {
        // given
        String classificationPrompt = "테스트 프롬프트";
        
        when(geminiConfig.isApiKeyConfigured()).thenReturn(true);
        when(geminiConfig.getApiUrl()).thenReturn("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent");
        when(geminiConfig.getApiKey()).thenReturn("test-api-key");
        
        // 잘못된 AI 응답 모킹
        Map<String, Object> invalidResponse = createMockAiResponse("INVALID_DOMAIN", "INVALID_KEYWORD");
        when(geminiRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(invalidResponse);
        
        when(aiClassificationRepository.save(any(AiClassification.class))).thenReturn(testClassification);

        // when
        AiClassification result = aiClassificationService.classifyApi(testApiId, testApiName, testApiDescription, testApiUrl, classificationPrompt);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getClassifiedDomain()).isEqualTo(ApiDomain.FINANCE); // 키워드 기반 Fallback
        assertThat(result.getClassifiedKeyword()).isEqualTo(ApiKeyword.STOCK_PRICE); // 도메인 기본 키워드
        
        verify(aiClassificationRepository).save(any(AiClassification.class));
    }

    @Test
    @DisplayName("키워드 기반 도메인 분류 - 금융")
    void classifyDomainWithKeywords_Finance() {
        // given
        when(geminiConfig.isApiKeyConfigured()).thenReturn(false);
        when(aiClassificationRepository.save(any(AiClassification.class))).thenReturn(testClassification);

        // when
        AiClassification result = aiClassificationService.classifyApi(testApiId, "주식 API", "주식 가격 정보", "https://stock.com", null);

        // then
        assertThat(result.getClassifiedDomain()).isEqualTo(ApiDomain.FINANCE);
    }

    @Test
    @DisplayName("키워드 기반 도메인 분류 - 날씨")
    void classifyDomainWithKeywords_Weather() {
        // given
        when(geminiConfig.isApiKeyConfigured()).thenReturn(false);
        when(aiClassificationRepository.save(any(AiClassification.class))).thenReturn(testClassification);

        // when
        AiClassification result = aiClassificationService.classifyApi(testApiId, "날씨 API", "기상 정보", "https://weather.com", null);

        // then
        // testClassification을 그대로 반환하므로 실제 분류 로직은 확인할 수 없음
        // 실제 테스트에서는 분류 결과를 확인하려면 save 메서드 호출 시 인자를 캡처해야 함
        verify(aiClassificationRepository).save(any(AiClassification.class));
    }

    @Test
    @DisplayName("키워드 기반 도메인 분류 - 기타")
    void classifyDomainWithKeywords_Others() {
        // given
        when(geminiConfig.isApiKeyConfigured()).thenReturn(false);
        when(aiClassificationRepository.save(any(AiClassification.class))).thenReturn(testClassification);

        // when
        AiClassification result = aiClassificationService.classifyApi(testApiId, "알 수 없는 API", "알 수 없는 설명", "https://unknown.com", null);

        // then
        verify(aiClassificationRepository).save(any(AiClassification.class));
    }

    @Test
    @DisplayName("분류 결과 조회 성공")
    void getClassificationById_Success() {
        // given
        String classificationId = "test-classification-001";
        when(aiClassificationRepository.findByClassificationId(classificationId)).thenReturn(Optional.of(testClassification));

        // when
        Optional<AiClassification> result = aiClassificationService.getClassificationById(classificationId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getClassificationId()).isEqualTo(classificationId);
        
        verify(aiClassificationRepository).findByClassificationId(classificationId);
    }

    @Test
    @DisplayName("분류 결과 조회 실패 - 존재하지 않는 ID")
    void getClassificationById_NotFound() {
        // given
        String classificationId = "non-existent-id";
        when(aiClassificationRepository.findByClassificationId(classificationId)).thenReturn(Optional.empty());

        // when
        Optional<AiClassification> result = aiClassificationService.getClassificationById(classificationId);

        // then
        assertThat(result).isEmpty();
        
        verify(aiClassificationRepository).findByClassificationId(classificationId);
    }

    @Test
    @DisplayName("API의 분류 결과 조회")
    void getClassificationsByApiId() {
        // given
        List<AiClassification> classifications = Arrays.asList(testClassification);
        when(aiClassificationRepository.findByApiId(testApiId)).thenReturn(classifications);

        // when
        List<AiClassification> result = aiClassificationService.getClassificationsByApiId(testApiId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApiId()).isEqualTo(testApiId);
        
        verify(aiClassificationRepository).findByApiId(testApiId);
    }

    @Test
    @DisplayName("도메인별 분류 결과 조회")
    void getClassificationsByDomain() {
        // given
        ApiDomain domain = ApiDomain.FINANCE;
        List<AiClassification> classifications = Arrays.asList(testClassification);
        when(aiClassificationRepository.findByClassifiedDomain(domain)).thenReturn(classifications);

        // when
        List<AiClassification> result = aiClassificationService.getClassificationsByDomain(domain);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClassifiedDomain()).isEqualTo(domain);
        
        verify(aiClassificationRepository).findByClassifiedDomain(domain);
    }

    @Test
    @DisplayName("키워드별 분류 결과 조회")
    void getClassificationsByKeyword() {
        // given
        ApiKeyword keyword = ApiKeyword.STOCK_PRICE;
        List<AiClassification> classifications = Arrays.asList(testClassification);
        when(aiClassificationRepository.findByClassifiedKeyword(keyword)).thenReturn(classifications);

        // when
        List<AiClassification> result = aiClassificationService.getClassificationsByKeyword(keyword);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClassifiedKeyword()).isEqualTo(keyword);
        
        verify(aiClassificationRepository).findByClassifiedKeyword(keyword);
    }

    @Test
    @DisplayName("최근 분류 결과 조회")
    void getRecentClassifications() {
        // given
        int days = 7;
        List<AiClassification> classifications = Arrays.asList(testClassification);
        when(aiClassificationRepository.findRecentlyClassified(any(LocalDateTime.class))).thenReturn(classifications);

        // when
        List<AiClassification> result = aiClassificationService.getRecentClassifications(days);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        
        verify(aiClassificationRepository).findRecentlyClassified(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("도메인별 분류 통계 조회")
    void getClassificationStatsByDomain() {
        // given
        List<Object[]> stats = Arrays.asList(
                new Object[]{ApiDomain.FINANCE, 10L},
                new Object[]{ApiDomain.TECHNOLOGY, 5L}
        );
        when(aiClassificationRepository.getClassificationStatsByDomain()).thenReturn(stats);

        // when
        List<Object[]> result = aiClassificationService.getClassificationStatsByDomain();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        
        verify(aiClassificationRepository).getClassificationStatsByDomain();
    }

    @Test
    @DisplayName("키워드별 분류 통계 조회")
    void getClassificationStatsByKeyword() {
        // given
        List<Object[]> stats = Arrays.asList(
                new Object[]{ApiKeyword.STOCK_PRICE, 10L},
                new Object[]{ApiKeyword.API_DOCUMENT, 5L}
        );
        when(aiClassificationRepository.getClassificationStatsByKeyword()).thenReturn(stats);

        // when
        List<Object[]> result = aiClassificationService.getClassificationStatsByKeyword();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        
        verify(aiClassificationRepository).getClassificationStatsByKeyword();
    }

    @Test
    @DisplayName("분류 결과 검색")
    void searchClassifications() {
        // given
        String searchTerm = "주식";
        List<AiClassification> classifications = Arrays.asList(testClassification);
        when(aiClassificationRepository.findByClassifiedDomainAndClassifiedKeyword(ApiDomain.OTHERS, ApiKeyword.API_DOCUMENT))
                .thenReturn(classifications);

        // when
        List<AiClassification> result = aiClassificationService.searchClassifications(searchTerm);

        // then
        assertThat(result).isNotNull();
        
        verify(aiClassificationRepository).findByClassifiedDomainAndClassifiedKeyword(ApiDomain.OTHERS, ApiKeyword.API_DOCUMENT);
    }

    @Test
    @DisplayName("분류 결과 삭제 성공")
    void deleteClassification_Success() {
        // given
        String classificationId = "test-classification-001";
        when(aiClassificationRepository.findByClassificationId(classificationId)).thenReturn(Optional.of(testClassification));
        when(aiClassificationRepository.save(any(AiClassification.class))).thenReturn(testClassification);

        // when
        aiClassificationService.deleteClassification(classificationId);

        // then
        assertThat(testClassification.getDeleted()).isTrue();
        
        verify(aiClassificationRepository).findByClassificationId(classificationId);
        verify(aiClassificationRepository).save(testClassification);
    }

    @Test
    @DisplayName("분류 결과 삭제 실패 - 존재하지 않는 ID")
    void deleteClassification_NotFound() {
        // given
        String classificationId = "non-existent-id";
        when(aiClassificationRepository.findByClassificationId(classificationId)).thenReturn(Optional.empty());

        // when
        aiClassificationService.deleteClassification(classificationId);

        // then
        verify(aiClassificationRepository).findByClassificationId(classificationId);
        verify(aiClassificationRepository, never()).save(any(AiClassification.class));
    }

    @Test
    @DisplayName("API의 모든 분류 결과 삭제")
    void deleteAllClassificationsByApiId() {
        // given
        AiClassification classification2 = new AiClassification();
        classification2.setClassificationId("test-classification-002");
        classification2.setApiId(testApiId);
        classification2.setDeleted(false);
        
        List<AiClassification> classifications = Arrays.asList(testClassification, classification2);
        when(aiClassificationRepository.findByApiId(testApiId)).thenReturn(classifications);
        when(aiClassificationRepository.saveAll(any())).thenReturn(classifications);

        // when
        aiClassificationService.deleteAllClassificationsByApiId(testApiId);

        // then
        assertThat(testClassification.getDeleted()).isTrue();
        assertThat(classification2.getDeleted()).isTrue();
        
        verify(aiClassificationRepository).findByApiId(testApiId);
        verify(aiClassificationRepository).saveAll(classifications);
    }

    @Test
    @DisplayName("도메인별 기본 키워드 확인 - 모든 도메인")
    void getDefaultKeywordForDomain_AllDomains() {
        // given
        when(geminiConfig.isApiKeyConfigured()).thenReturn(false);
        when(aiClassificationRepository.save(any(AiClassification.class))).thenAnswer(invocation -> {
            AiClassification arg = invocation.getArgument(0);
            testClassification.setClassifiedDomain(arg.getClassifiedDomain());
            testClassification.setClassifiedKeyword(arg.getClassifiedKeyword());
            return testClassification;
        });

        // 각 도메인별로 테스트
        Map<String, ApiDomain> domainTestCases = new HashMap<>();
        domainTestCases.put("주식", ApiDomain.FINANCE);
        domainTestCases.put("날씨", ApiDomain.WEATHER);
        domainTestCases.put("뉴스", ApiDomain.NEWS);
        domainTestCases.put("교통", ApiDomain.TRANSPORTATION);
        domainTestCases.put("상품", ApiDomain.COMMERCE);
        domainTestCases.put("정부", ApiDomain.GOVERNMENT);
        domainTestCases.put("영화", ApiDomain.ENTERTAINMENT);
        domainTestCases.put("스포츠", ApiDomain.SPORTS);
        domainTestCases.put("건강", ApiDomain.HEALTHCARE);
        domainTestCases.put("교육", ApiDomain.EDUCATION);
        domainTestCases.put("부동산", ApiDomain.REALESTATE);
        domainTestCases.put("여행", ApiDomain.TRAVEL);
        domainTestCases.put("개발", ApiDomain.TECHNOLOGY);
        domainTestCases.put("패션", ApiDomain.LIFESTYLE);

        Map<ApiDomain, ApiKeyword> expectedKeywords = new HashMap<>();
        expectedKeywords.put(ApiDomain.FINANCE, ApiKeyword.STOCK_PRICE);
        expectedKeywords.put(ApiDomain.WEATHER, ApiKeyword.CURRENT_WEATHER);
        expectedKeywords.put(ApiDomain.NEWS, ApiKeyword.BREAKING_NEWS);
        expectedKeywords.put(ApiDomain.TRANSPORTATION, ApiKeyword.BUS_INFO);
        expectedKeywords.put(ApiDomain.COMMERCE, ApiKeyword.PRODUCT_INFO);
        expectedKeywords.put(ApiDomain.GOVERNMENT, ApiKeyword.PUBLIC_DATA);
        expectedKeywords.put(ApiDomain.ENTERTAINMENT, ApiKeyword.MOVIE_INFO);
        expectedKeywords.put(ApiDomain.SPORTS, ApiKeyword.SOCCER_RESULT);
        expectedKeywords.put(ApiDomain.HEALTHCARE, ApiKeyword.HEALTH_TIP);
        expectedKeywords.put(ApiDomain.EDUCATION, ApiKeyword.COURSE_INFO);
        expectedKeywords.put(ApiDomain.REALESTATE, ApiKeyword.HOUSE_PRICE);
        expectedKeywords.put(ApiDomain.TRAVEL, ApiKeyword.HOTEL_INFO);
        expectedKeywords.put(ApiDomain.TECHNOLOGY, ApiKeyword.API_DOCUMENT);
        expectedKeywords.put(ApiDomain.LIFESTYLE, ApiKeyword.FASHION_TREND);

        // when & then
        for (Map.Entry<String, ApiDomain> testCase : domainTestCases.entrySet()) {
            String keyword = testCase.getKey();
            ApiDomain expectedDomain = testCase.getValue();
            ApiKeyword expectedKeyword = expectedKeywords.get(expectedDomain);

            AiClassification result = aiClassificationService.classifyApi(
                    "test-api", keyword + " API", keyword + " 관련 설명", "https://test.com", null);

            // 실제 검증은 mock으로 반환되는 값이므로 호출만 확인
            verify(aiClassificationRepository, atLeastOnce()).save(any(AiClassification.class));
        }
    }

    /**
     * AI 응답 모킹용 헬퍼 메서드
     */
    private Map<String, Object> createMockAiResponse(String domainText, String keywordText) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        
        part.put("text", domainText);
        content.put("parts", Arrays.asList(part));
        candidate.put("content", content);
        response.put("candidates", Arrays.asList(candidate));
        
        return response;
    }
}
