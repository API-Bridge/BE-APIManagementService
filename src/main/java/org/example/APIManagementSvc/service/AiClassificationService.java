package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.config.GeminiConfig;
import org.example.APIManagementSvc.domain.Entity.AiClassification;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import org.example.APIManagementSvc.repository.AiClassificationRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 분류 서비스
 * API 등록 시 자동으로 AI 분류를 수행하는 서비스
 * Gemini AI를 사용하여 API를 도메인과 키워드로 자동 분류
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiClassificationService {

    private final AiClassificationRepository aiClassificationRepository;
    private final GeminiConfig geminiConfig;
    private final RestTemplate geminiRestTemplate;

    /**
     * 🔥 API 자동 분류 실행
     * API 등록 시 자동으로 호출되어 도메인과 키워드를 분류
     */
    @Transactional
    public AiClassification classifyApi(String apiId, String apiName, String apiDescription, String apiUrl, String classificationPrompt) {
        log.info("AI 분류 시작: API ID={}, Name={}", apiId, apiName);

        try {
            // Gemini AI를 사용한 분류 로직
            ApiDomain classifiedDomain = classifyDomainWithAI(apiName, apiDescription, apiUrl, classificationPrompt);
            ApiKeyword classifiedKeyword = classifyKeywordWithAI(apiName, apiDescription, apiUrl, classifiedDomain, classificationPrompt);
            
            // 분류 결과 저장
            AiClassification classification = new AiClassification();
            classification.setClassificationId(UUID.randomUUID().toString());
            classification.setApiId(apiId);
            classification.setClassifiedDomain(classifiedDomain);
            classification.setClassifiedKeyword(classifiedKeyword);
            classification.setClassifiedAt(LocalDateTime.now());
            classification.setDeleted(false);

            AiClassification saved = aiClassificationRepository.save(classification);
            log.info("AI 분류 완료: Domain={}, Keyword={}", classifiedDomain, classifiedKeyword);
            
            return saved;
            
        } catch (Exception e) {
            log.error("AI 분류 실패: API ID={}, Error={}", apiId, e.getMessage(), e);
            throw new RuntimeException("AI 분류 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * Gemini AI를 사용한 도메인 분류
     */
    private ApiDomain classifyDomainWithAI(String apiName, String apiDescription, String apiUrl, String classificationPrompt) {
        if (!geminiConfig.isApiKeyConfigured()) {
            log.warn("Gemini API 키가 설정되지 않았습니다. 키워드 기반 분류를 사용합니다.");
            return classifyDomainWithKeywords(apiName, apiDescription, apiUrl);
        }

        try {
            String prompt = classificationPrompt; // Use the provided prompt

            String result = callGeminiAI(prompt);
            log.info("AI 도메인 분류 결과: {}", result);
            
            try {
                return ApiDomain.valueOf(result.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("AI가 반환한 도메인 '{}'이 유효하지 않습니다. 키워드 기반 분류를 사용합니다.", result);
                return classifyDomainWithKeywords(apiName, apiDescription, apiUrl);
            }
            
        } catch (Exception e) {
            log.error("AI 도메인 분류 실패: {}", e.getMessage(), e);
            return classifyDomainWithKeywords(apiName, apiDescription, apiUrl);
        }
    }

    /**
     * Gemini AI를 사용한 키워드 분류
     */
    private ApiKeyword classifyKeywordWithAI(String apiName, String apiDescription, String apiUrl, ApiDomain domain, String classificationPrompt) {
        if (!geminiConfig.isApiKeyConfigured()) {
            log.warn("Gemini API 키가 설정되지 않았습니다. 기본 키워드를 사용합니다.");
            return getDefaultKeywordForDomain(domain);
        }

        try {
            String prompt = classificationPrompt; // Use the provided prompt

            String result = callGeminiAI(prompt);
            log.info("AI 키워드 분류 결과: {}", result);
            
            try {
                return ApiKeyword.valueOf(result.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("AI가 반환한 키워드 '{}'이 유효하지 않습니다. 기본 키워드를 사용합니다.", result);
                return getDefaultKeywordForDomain(domain);
            }
            
        } catch (Exception e) {
            log.error("AI 키워드 분류 실패: {}", e.getMessage(), e);
            return getDefaultKeywordForDomain(domain);
        }
    }

    /**
     * Gemini AI API 호출
     */
    private String callGeminiAI(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
            )));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            String url = geminiConfig.getApiUrl() + "?key=" + geminiConfig.getApiKey();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = geminiRestTemplate.postForObject(url, request, Map.class);
            
            if (response != null && response.containsKey("candidates")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    if (candidate.containsKey("content")) {
                        Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                        if (content.containsKey("parts")) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                            if (!parts.isEmpty()) {
                                return (String) parts.get(0).get("text");
                            }
                        }
                    }
                }
            }
            
            throw new RuntimeException("AI 응답에서 텍스트를 추출할 수 없습니다.");
            
        } catch (Exception e) {
            log.error("Gemini AI API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("AI API 호출에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 키워드 기반 도메인 분류 (Fallback)
     */
    private ApiDomain classifyDomainWithKeywords(String apiName, String apiDescription, String apiUrl) {
        String text = (apiName + " " + apiDescription + " " + apiUrl).toLowerCase();
        
        if (text.contains("주식") || text.contains("환율") || text.contains("금리") || text.contains("금융")) {
            return ApiDomain.FINANCE;
        } else if (text.contains("날씨") || text.contains("기상") || text.contains("미세먼지")) {
            return ApiDomain.WEATHER;
        } else if (text.contains("뉴스") || text.contains("소셜") || text.contains("트렌드")) {
            return ApiDomain.NEWS;
        } else if (text.contains("교통") || text.contains("지도") || text.contains("위치")) {
            return ApiDomain.TRANSPORTATION;
        } else if (text.contains("상품") || text.contains("가격") || text.contains("쇼핑")) {
            return ApiDomain.COMMERCE;
        } else if (text.contains("정부") || text.contains("공공") || text.contains("통계")) {
            return ApiDomain.GOVERNMENT;
        } else if (text.contains("영화") || text.contains("음악") || text.contains("게임")) {
            return ApiDomain.ENTERTAINMENT;
        } else if (text.contains("스포츠") || text.contains("경기") || text.contains("선수")) {
            return ApiDomain.SPORTS;
        } else if (text.contains("건강") || text.contains("병원") || text.contains("의료")) {
            return ApiDomain.HEALTHCARE;
        } else if (text.contains("교육") || text.contains("학습") || text.contains("강의")) {
            return ApiDomain.EDUCATION;
        } else if (text.contains("부동산") || text.contains("매매") || text.contains("전세")) {
            return ApiDomain.REALESTATE;
        } else if (text.contains("여행") || text.contains("항공") || text.contains("숙박")) {
            return ApiDomain.TRAVEL;
        } else if (text.contains("개발") || text.contains("기술") || text.contains("API")) {
            return ApiDomain.TECHNOLOGY;
        } else if (text.contains("패션") || text.contains("뷰티") || text.contains("인테리어")) {
            return ApiDomain.LIFESTYLE;
        } else {
            return ApiDomain.OTHERS;
        }
    }

    /**
     * 도메인별 기본 키워드 반환
     */
    private ApiKeyword getDefaultKeywordForDomain(ApiDomain domain) {
        switch (domain) {
            case FINANCE:
                return ApiKeyword.STOCK_PRICE;
            case WEATHER:
                return ApiKeyword.CURRENT_WEATHER;
            case NEWS:
                return ApiKeyword.BREAKING_NEWS;
            case TRANSPORTATION:
                return ApiKeyword.BUS_INFO;
            case COMMERCE:
                return ApiKeyword.PRODUCT_INFO;
            case GOVERNMENT:
                return ApiKeyword.PUBLIC_DATA;
            case ENTERTAINMENT:
                return ApiKeyword.MOVIE_INFO;
            case SPORTS:
                return ApiKeyword.SOCCER_RESULT;
            case HEALTHCARE:
                return ApiKeyword.HEALTH_TIP;
            case EDUCATION:
                return ApiKeyword.COURSE_INFO;
            case REALESTATE:
                return ApiKeyword.HOUSE_PRICE;
            case TRAVEL:
                return ApiKeyword.HOTEL_INFO;
            case TECHNOLOGY:
                return ApiKeyword.API_DOCUMENT;
            case LIFESTYLE:
                return ApiKeyword.FASHION_TREND;
            default:
                return ApiKeyword.API_DOCUMENT;
        }
    }
}


