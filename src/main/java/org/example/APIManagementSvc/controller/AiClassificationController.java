package org.example.APIManagementSvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.AiClassification;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import org.example.APIManagementSvc.dto.ai.AiClassificationRequest;
import org.example.APIManagementSvc.dto.ai.AiClassificationResponse;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.service.AiClassificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 분류 컨트롤러
 * API 자동 분류 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api-management/ai/classifications")
@RequiredArgsConstructor
public class AiClassificationController {

    private final AiClassificationService aiClassificationService;

    /**
     * API 자동 분류 실행
     */
    @PostMapping("/classify")
    public ResponseEntity<ApiResponse<AiClassificationResponse>> classifyApi(
            @Valid @RequestBody AiClassificationRequest request) {
        
        log.info("AI 분류 요청: API ID={}, Name={}", request.getApiId(), request.getApiName());
        
        try {
            AiClassification classification = aiClassificationService.classifyApi(
                request.getApiId(),
                request.getApiName(),
                request.getApiDescription(),
                request.getApiUrl()
            );
            
            AiClassificationResponse response = convertToResponse(classification);
            
            return ResponseEntity.ok(ApiResponse.success(response, "AI 분류가 완료되었습니다"));
            
        } catch (Exception e) {
            log.error("AI 분류 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("AI 분류에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 분류 결과 조회
     */
    @GetMapping("/{classificationId}")
    public ResponseEntity<ApiResponse<AiClassificationResponse>> getClassification(
            @PathVariable String classificationId) {
        
        return aiClassificationService.getClassificationById(classificationId)
            .map(classification -> {
                AiClassificationResponse response = convertToResponse(classification);
                return ResponseEntity.ok(ApiResponse.success(response, "분류 결과를 조회했습니다"));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * API의 분류 결과 조회
     */
    @GetMapping("/api/{apiId}")
    public ResponseEntity<ApiResponse<List<AiClassificationResponse>>> getClassificationsByApiId(
            @PathVariable String apiId) {
        
        List<AiClassification> classifications = aiClassificationService.getClassificationsByApiId(apiId);
        List<AiClassificationResponse> responses = classifications.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(responses, "API의 분류 결과를 조회했습니다"));
    }

    /**
     * 도메인별 분류 결과 조회
     */
    @GetMapping("/domain/{domain}")
    public ResponseEntity<ApiResponse<List<AiClassificationResponse>>> getClassificationsByDomain(
            @PathVariable ApiDomain domain) {
        
        List<AiClassification> classifications = aiClassificationService.getClassificationsByDomain(domain);
        List<AiClassificationResponse> responses = classifications.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(responses, "도메인별 분류 결과를 조회했습니다"));
    }

    /**
     * 키워드별 분류 결과 조회
     */
    @GetMapping("/keyword/{keyword}")
    public ResponseEntity<ApiResponse<List<AiClassificationResponse>>> getClassificationsByKeyword(
            @PathVariable ApiKeyword keyword) {
        
        List<AiClassification> classifications = aiClassificationService.getClassificationsByKeyword(keyword);
        List<AiClassificationResponse> responses = classifications.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(responses, "키워드별 분류 결과를 조회했습니다"));
    }

    /**
     * 최근 분류 결과 조회
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<AiClassificationResponse>>> getRecentClassifications(
            @RequestParam(defaultValue = "7") int days) {
        
        List<AiClassification> classifications = aiClassificationService.getRecentClassifications(days);
        List<AiClassificationResponse> responses = classifications.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(responses, "최근 분류 결과를 조회했습니다"));
    }

    /**
     * 분류 결과 통계 조회
     */
    @GetMapping("/stats/domain")
    public ResponseEntity<ApiResponse<List<Object[]>>> getClassificationStatsByDomain() {
        List<Object[]> stats = aiClassificationService.getClassificationStatsByDomain();
        return ResponseEntity.ok(ApiResponse.success(stats, "도메인별 분류 통계를 조회했습니다"));
    }

    @GetMapping("/stats/keyword")
    public ResponseEntity<ApiResponse<List<Object[]>>> getClassificationStatsByKeyword() {
        List<Object[]> stats = aiClassificationService.getClassificationStatsByKeyword();
        return ResponseEntity.ok(ApiResponse.success(stats, "키워드별 분류 통계를 조회했습니다"));
    }

    /**
     * 분류 결과 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<AiClassificationResponse>>> searchClassifications(
            @RequestParam String q) {
        
        List<AiClassification> classifications = aiClassificationService.searchClassifications(q);
        List<AiClassificationResponse> responses = classifications.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(responses, "분류 결과 검색이 완료되었습니다"));
    }

    /**
     * 분류 결과 삭제
     */
    @DeleteMapping("/{classificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteClassification(
            @PathVariable String classificationId) {
        
        aiClassificationService.deleteClassification(classificationId);
        return ResponseEntity.ok(ApiResponse.success(null, "분류 결과가 삭제되었습니다"));
    }

    /**
     * API의 모든 분류 결과 삭제
     */
    @DeleteMapping("/api/{apiId}")
    public ResponseEntity<ApiResponse<Void>> deleteAllClassificationsByApiId(
            @PathVariable String apiId) {
        
        aiClassificationService.deleteAllClassificationsByApiId(apiId);
        return ResponseEntity.ok(ApiResponse.success(null, "API의 모든 분류 결과가 삭제되었습니다"));
    }

    /**
     * Entity를 Response DTO로 변환
     */
    private AiClassificationResponse convertToResponse(AiClassification classification) {
        AiClassificationResponse response = new AiClassificationResponse();
        response.setClassificationId(classification.getClassificationId());
        response.setApiId(classification.getApiId());
        response.setClassifiedDomain(classification.getClassifiedDomain());
        response.setClassifiedKeyword(classification.getClassifiedKeyword());
        response.setClassifiedAt(classification.getClassifiedAt());
        response.setClassificationLog(classification.getClassificationLog());
        response.setAnalyzedText(classification.getAnalyzedText());
        response.setModelVersion(classification.getModelVersion());
        response.setMetadata(classification.getMetadata());
        return response;
    }
}
