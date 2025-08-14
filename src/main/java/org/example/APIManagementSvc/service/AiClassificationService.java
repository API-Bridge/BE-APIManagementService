package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.AiClassification;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import org.example.APIManagementSvc.repository.AiClassificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * API 자동 분류 실행
     */
    @Transactional
    public AiClassification classifyApi(String apiId, String apiName, String apiDescription, String apiUrl) {
        log.info("AI 분류 시작: API ID={}, Name={}", apiId, apiName);

        try {
            // Gemini AI를 사용한 분류 로직 (실제 구현은 Gemini API 연동 필요)
            ApiDomain classifiedDomain = classifyDomain(apiName, apiDescription, apiUrl);
            ApiKeyword classifiedKeyword = classifyKeyword(apiName, apiDescription, apiUrl, classifiedDomain);
            
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
     * 도메인 분류 (Gemini AI 연동 필요)
     */
    private ApiDomain classifyDomain(String apiName, String apiDescription, String apiUrl) {
        // TODO: Gemini AI API 연동하여 실제 분류 로직 구현
        // 현재는 간단한 키워드 기반 분류
        
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
     * 키워드 분류 (Gemini AI 연동 필요)
     */
    private ApiKeyword classifyKeyword(String apiName, String apiDescription, String apiUrl, ApiDomain domain) {
        // TODO: Gemini AI API 연동하여 실제 분류 로직 구현
        // 현재는 도메인별 기본 키워드 반환
        
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

