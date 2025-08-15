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
import java.util.Optional;
import java.util.UUID;

/**
 * AI 분류 서비스
 * Gemini AI를 사용하여 API를 자동으로 분류하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiClassificationService {

    private final AiClassificationRepository aiClassificationRepository;
    private final GeminiConfig geminiConfig;
    private final RestTemplate geminiRestTemplate;

    /**
     * API 자동 분류 실행
     */
    @Transactional
    public AiClassification classifyApi(String apiId, String apiName, String apiDescription, String apiUrl) {
        log.info("AI 분류 시작: API ID={}, Name={}", apiId, apiName);

        try {
            // Gemini AI를 사용한 분류 로직
            ApiDomain classifiedDomain = classifyDomainWithAI(apiName, apiDescription, apiUrl);
            ApiKeyword classifiedKeyword = classifyKeywordWithAI(apiName, apiDescription, apiUrl, classifiedDomain);
            
            // 분류 결과 저장
            AiClassification classification = new AiClassification();
            classification.setClassificationId(UUID.randomUUID().toString());
            classification.setApiId(apiId);
            classification.setClassifiedDomain(classifiedDomain);
            classification.setClassifiedKeyword(classifiedKeyword);
            classification.setClassifiedAt(LocalDateTime.now());
            classification.setAnalyzedText(String.format("API Name: %s, Description: %s, URL: %s", apiName, apiDescription, apiUrl));
            classification.setModelVersion("gemini-1.5-flash");
            classification.setClassificationLog("AI 자동 분류 완료");
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
    private ApiDomain classifyDomainWithAI(String apiName, String apiDescription, String apiUrl) {
        if (!geminiConfig.isApiKeyConfigured()) {
            log.warn("Gemini API 키가 설정되지 않았습니다. 키워드 기반 분류를 사용합니다.");
            return classifyDomainWithKeywords(apiName, apiDescription, apiUrl);
        }

        try {
            String prompt = String.format("""
                다음 API 정보를 분석하여 가장 적절한 도메인을 분류해주세요.
                
                API 이름: %s
                API 설명: %s
                API URL: %s
                
                다음 도메인 중에서 하나를 선택해주세요:
                - COMMERCE (상거래, 쇼핑, 결제)
                - EDUCATION (교육, 학습, 강의)
                - ENTERTAINMENT (엔터테인먼트, 게임, 미디어)
                - FINANCE (금융, 주식, 환율, 은행)
                - GOVERNMENT (정부, 공공, 행정)
                - HEALTHCARE (의료, 건강, 병원)
                - LIFESTYLE (라이프스타일, 패션, 뷰티)
                - NEWS (뉴스, 소셜, 트렌드)
                - OTHERS (기타)
                - REALESTATE (부동산, 매매, 임대)
                - SPORTS (스포츠, 운동, 경기)
                - TECHNOLOGY (기술, 개발, IT)
                - TRANSPORTATION (교통, 이동, 지도)
                - TRAVEL (여행, 항공, 숙박)
                - WEATHER (날씨, 기상, 환경)
                
                답변은 도메인 이름만 출력해주세요. (예: FINANCE)
                """, apiName, apiDescription, apiUrl);

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
    private ApiKeyword classifyKeywordWithAI(String apiName, String apiDescription, String apiUrl, ApiDomain domain) {
        if (!geminiConfig.isApiKeyConfigured()) {
            log.warn("Gemini API 키가 설정되지 않았습니다. 기본 키워드를 사용합니다.");
            return getDefaultKeywordForDomain(domain);
        }

        try {
            String prompt = String.format("""
                다음 API 정보와 도메인을 분석하여 가장 적절한 키워드를 분류해주세요.
                
                API 이름: %s
                API 설명: %s
                API URL: %s
                도메인: %s
                
                다음 키워드 중에서 하나를 선택해주세요:
                - AIR_QUALITY (대기질, 미세먼지)
                - API_DOCUMENT (API 문서, 개발자 도구)
                - BASEBALL_RESULT (야구 결과, 경기)
                - BASKETBALL_RESULT (농구 결과, 경기)
                - BEAUTY_TIP (뷰티 팁, 화장품)
                - BREAKING_NEWS (속보, 긴급 뉴스)
                - BUS_INFO (버스 정보, 노선)
                - CELEBRITY_NEWS (연예인 뉴스, 소식)
                - COUPON_DISCOUNT (쿠폰, 할인)
                - COURSE_INFO (강의 정보, 과정)
                - CRYPTOCURRENCY (암호화폐, 가상화폐)
                - CURRENT_WEATHER (현재 날씨, 기온)
                - DEVELOPER_TOOL (개발자 도구, 소프트웨어)
                - ECONOMIC_INDICATOR (경제 지표, 통계)
                - ECONOMY_NEWS (경제 뉴스, 시장)
                - EXAM_SCHEDULE (시험 일정, 일정)
                - EXCHANGE_RATE (환율, 외환)
                - FASHION_TREND (패션 트렌드, 유행)
                - FITNESS_DATA (피트니스 데이터, 운동)
                - FLIGHT_INFO (항공 정보, 비행)
                - GAME_INFO (게임 정보, 게임)
                - HEALTH_TIP (건강 팁, 건강)
                - HOSPITAL_INFO (병원 정보, 의료)
                - HOTEL_INFO (호텔 정보, 숙박)
                - HOUSE_PRICE (집값, 부동산 가격)
                - INTEREST_RATE (금리, 이자율)
                - INTERIOR_TIP (인테리어 팁, 인테리어)
                - LEGAL_INFO (법률 정보, 법적 조언)
                - MEDICINE_INFO (의약품 정보, 약물)
                - MOVIE_INFO (영화 정보, 영화)
                - MUSIC_CHART (음악 차트, 음악)
                - PARKING_INFO (주차 정보, 주차)
                - PLAYER_STATS (선수 통계, 선수)
                - POLICY_INFO (정책 정보, 정책)
                - POLITICS (정치, 정치 뉴스)
                - PRECIPITATION (강수량, 비, 눈)
                - PRICE_COMPARISON (가격 비교, 가격)
                - PRODUCT_INFO (상품 정보, 제품)
                - PRODUCT_REVIEW (상품 리뷰, 제품 리뷰)
                - PUBLIC_DATA (공공 데이터, 공개 데이터)
                - PUBLIC_SERVICE (공공 서비스, 공무)
                - REAL_ESTATE_TREND (부동산 트렌드, 시장)
                - RECIPE (레시피, 요리법)
                - RENT_INFO (임대 정보, 임대)
                - RESTAURANT_INFO (식당 정보, 음식점)
                - SCHOLARSHIP (장학금, 장학)
                - SCHOOL_INFO (학교 정보, 교육기관)
                - SHOPPING_RANK (쇼핑 순위, 인기)
                - SOCCER_RESULT (축구 결과, 경기)
                - SOCIAL_TREND (소셜 트렌드, 트렌드)
                - SPORTS_NEWS (스포츠 뉴스, 스포츠)
                - SPORTS_SCHEDULE (스포츠 일정, 경기 일정)
                - STATISTICS (통계, 데이터)
                - STOCK_INDEX (주가 지수, 지수)
                - STOCK_PRICE (주가, 주식 가격)
                - SUBWAY_INFO (지하철 정보, 지하철)
                - TAXI_FARE (택시 요금, 택시)
                - TEAM_RANKING (팀 순위, 순위)
                - TECHNOLOGY_NEWS (기술 뉴스, IT 뉴스)
                - TECH_TREND (기술 트렌드, IT 트렌드)
                - TEMPERATURE (온도, 기온)
                - TOURIST_SPOT (관광지, 여행지)
                - TRAFFIC_CONDITION (교통 상황, 교통)
                - TV_SCHEDULE (TV 일정, 방송)
                - UV_INDEX (자외선 지수, UV)
                - WEATHER_FORECAST (날씨 예보, 예보)
                
                답변은 키워드 이름만 출력해주세요. (예: STOCK_PRICE)
                """, apiName, apiDescription, apiUrl, domain);

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

    /**
     * 분류 결과 조회
     */
    @Transactional(readOnly = true)
    public Optional<AiClassification> getClassificationById(String classificationId) {
        return aiClassificationRepository.findByClassificationId(classificationId);
    }

    /**
     * API의 분류 결과 조회
     */
    @Transactional(readOnly = true)
    public List<AiClassification> getClassificationsByApiId(String apiId) {
        return aiClassificationRepository.findByApiId(apiId);
    }

    /**
     * 도메인별 분류 결과 조회
     */
    @Transactional(readOnly = true)
    public List<AiClassification> getClassificationsByDomain(ApiDomain domain) {
        return aiClassificationRepository.findByClassifiedDomain(domain);
    }

    /**
     * 키워드별 분류 결과 조회
     */
    @Transactional(readOnly = true)
    public List<AiClassification> getClassificationsByKeyword(ApiKeyword keyword) {
        return aiClassificationRepository.findByClassifiedKeyword(keyword);
    }

    /**
     * 최근 분류 결과 조회
     */
    @Transactional(readOnly = true)
    public List<AiClassification> getRecentClassifications(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return aiClassificationRepository.findRecentlyClassified(since);
    }

    /**
     * 분류 결과 통계 조회
     */
    @Transactional(readOnly = true)
    public List<Object[]> getClassificationStatsByDomain() {
        return aiClassificationRepository.getClassificationStatsByDomain();
    }

    /**
     * 분류 결과 통계 조회
     */
    @Transactional(readOnly = true)
    public List<Object[]> getClassificationStatsByKeyword() {
        return aiClassificationRepository.getClassificationStatsByKeyword();
    }

    /**
     * 분류 결과 검색
     */
    @Transactional(readOnly = true)
    public List<AiClassification> searchClassifications(String searchTerm) {
        List<AiClassification> results = aiClassificationRepository.findByAnalyzedTextContaining(searchTerm);
        results.addAll(aiClassificationRepository.findByClassificationLogContaining(searchTerm));
        return results;
    }

    /**
     * 분류 결과 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteClassification(String classificationId) {
        Optional<AiClassification> classification = aiClassificationRepository.findByClassificationId(classificationId);
        if (classification.isPresent()) {
            AiClassification entity = classification.get();
            entity.setDeleted(true);
            aiClassificationRepository.save(entity);
            log.info("분류 결과 삭제 완료: {}", classificationId);
        }
    }

    /**
     * API의 모든 분류 결과 삭제
     */
    @Transactional
    public void deleteAllClassificationsByApiId(String apiId) {
        List<AiClassification> classifications = aiClassificationRepository.findByApiId(apiId);
        for (AiClassification classification : classifications) {
            classification.setDeleted(true);
        }
        aiClassificationRepository.saveAll(classifications);
        log.info("API의 모든 분류 결과 삭제 완료: {}", apiId);
    }
}


