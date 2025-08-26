package org.example.APIManagementSvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.dto.*;
import org.example.APIManagementSvc.dto.response.ExternalApiSpecRequestDto;
import org.example.APIManagementSvc.dto.response.ExternalApiSpecResponseDto;
import org.example.APIManagementSvc.service.ExternalApiSpecService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/external-api-specs")
@RequiredArgsConstructor
@Validated
@Tag(name = "External API Spec Management", description = "외부 API 명세 관리 API")
public class ExternalApiSpecController {

    private final ExternalApiSpecService externalApiSpecService;

    @Operation(summary = "외부 API 명세 등록", description = "새로운 외부 API 명세를 등록합니다. 파라미터는 유동적으로 설정 가능합니다.")
    @PostMapping
    public ResponseEntity<ExternalApiSpecResponseDto> createExternalApiSpec(
            @Valid @RequestBody ExternalApiSpecRequestDto requestDto) {
        log.info("Creating external API spec: {}", requestDto.getApiName());
        
        ExternalApiSpecResponseDto response = externalApiSpecService.createExternalApiSpec(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "외부 API 명세 단건 조회", description = "API ID로 외부 API 명세 정보를 조회합니다.")
    @GetMapping("/{apiId}")
    public ResponseEntity<ExternalApiSpecResponseDto> getExternalApiSpec(
            @Parameter(description = "API ID", required = true) @PathVariable String apiId) {
        log.info("Getting external API spec: {}", apiId);
        
        ExternalApiSpecResponseDto response = externalApiSpecService.getExternalApiSpec(apiId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "외부 API 명세 전체 조회 (페이징)", description = "등록된 모든 외부 API 명세를 페이징으로 조회합니다.")
    @GetMapping
    public ResponseEntity<Page<ExternalApiSpecResponseDto>> getAllExternalApiSpecs(
            @Parameter(description = "페이징 정보") @PageableDefault(size = 20) Pageable pageable) {
        log.info("Getting all external API specs with pagination");
        
        Page<ExternalApiSpecResponseDto> response = externalApiSpecService.getAllExternalApiSpecs(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "활성화된 외부 API 명세 조회", description = "현재 활성화된 모든 외부 API 명세를 조회합니다.")
    @GetMapping("/active")
    public ResponseEntity<List<ExternalApiSpecResponseDto>> getAllActiveExternalApiSpecs() {
        log.info("Getting all active external API specs");
        
        List<ExternalApiSpecResponseDto> response = externalApiSpecService.getAllActiveExternalApiSpecs();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "외부 API 명세 수정", description = "기존 외부 API 명세 정보를 수정합니다. 파라미터도 함께 수정 가능합니다.")
    @PutMapping("/{apiId}")
    public ResponseEntity<ExternalApiSpecResponseDto> updateExternalApiSpec(
            @Parameter(description = "API ID", required = true) @PathVariable String apiId,
            @Valid @RequestBody ExternalApiSpecUpdateDto updateDto) {
        log.info("Updating external API spec: {}", apiId);
        
        ExternalApiSpecResponseDto response = externalApiSpecService.updateExternalApiSpec(apiId, updateDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "외부 API 명세 삭제", description = "외부 API 명세를 삭제합니다. 관련된 파라미터도 함께 삭제됩니다.")
    @DeleteMapping("/{apiId}")
    public ResponseEntity<Void> deleteExternalApiSpec(
            @Parameter(description = "API ID", required = true) @PathVariable String apiId) {
        log.info("Deleting external API spec: {}", apiId);
        
        externalApiSpecService.deleteExternalApiSpec(apiId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "제공 기관별 외부 API 명세 조회", description = "특정 제공기관 으로 등록된 외부 API 명세들을 조회합니다.")
    @GetMapping("/by-credential/{credentialId}")
    public ResponseEntity<List<ExternalApiSpecResponseDto>> getExternalApiSpecsByCredential(
            @Parameter(description = "제공 기관 ID", required = true) @PathVariable String credentialId) {
        log.info("Getting external API specs by credential: {}", credentialId);
        
        List<ExternalApiSpecResponseDto> response = externalApiSpecService.getExternalApiSpecsByCredential(credentialId);
        return ResponseEntity.ok(response);
    }




    /**
     * 도메인 이름별 외부 API 명세 조회
     * -
     * 특정 도메인 이름에 속한 모든 외부 API를 조회
     * 사용자 친화적인 도메인 이름으로 API를 분류하여 관리할 때 사용
     * -
     * 사용 예시:
     * - finance 도메인의 모든 API (주식, 환율, 암호화폐 등)
     * - weather 도메인의 모든 API (현재날씨, 일기예보, 대기질 등)
     * 
     * @param domainName 도메인 이름 (Path Variable)
     * @return 해당 도메인의 API 명세 목록 (200 OK)
     */
    @Operation(summary = "도메인 이름별 외부 API 명세 조회", 
               description = "특정 도메인 이름에 속한 외부 API 명세들을 조회합니다.")
    @GetMapping("/by-domain-name/{domainName}")
    public ResponseEntity<List<ExternalApiSpecResponseDto>> getExternalApiSpecsByDomainName(
            @Parameter(description = "도메인 이름", example = "finance", required = true) @PathVariable String domainName) {
        log.info("Getting external API specs by domain name: {}", domainName);
        
        List<ExternalApiSpecResponseDto> response = externalApiSpecService.getExternalApiSpecsByDomainName(domainName);
        return ResponseEntity.ok(response);
    }

    /**
     * 키워드 이름별 외부 API 명세 조회
     * -
     * 특정 키워드 이름에 속한 모든 외부 API를 조회
     * 사용자 친화적인 키워드 이름으로 더 정확한 API 분류가 필요할 때 사용
     * -
     * 사용 예시:
     * - stock_price 키워드의 모든 API
     * - current_weather 키워드의 모든 API
     * 
     * @param keywordName 키워드 이름 (Path Variable)
     * @return 해당 키워드의 API 명세 목록 (200 OK)
     */
    @Operation(summary = "키워드 이름별 외부 API 명세 조회", 
               description = "특정 키워드 이름에 속한 외부 API 명세들을 조회합니다.")
    @GetMapping("/by-keyword-name/{keywordName}")
    public ResponseEntity<List<ExternalApiSpecResponseDto>> getExternalApiSpecsByKeywordName(
            @Parameter(description = "키워드 이름", example = "stock_price", required = true) @PathVariable String keywordName) {
        log.info("Getting external API specs by keyword name: {}", keywordName);
        
        List<ExternalApiSpecResponseDto> response = externalApiSpecService.getExternalApiSpecsByKeywordName(keywordName);
        return ResponseEntity.ok(response);
    }

    /**
     * 도메인 이름과 키워드 이름을 함께 사용한 외부 API 명세 조회
     * -
     * 특정 도메인 이름 내에서 특정 키워드 이름에 해당하는 API들을 정확하게 필터링
     * 사용자 친화적인 이름을 사용하여 더 직관적인 API 검색 제공
     * -
     * 사용 예시:
     * - finance + stock_price = 주식 관련 금융 API만 조회
     * - weather + current_weather = 현재 날씨 API만 조회
     * - transport + subway = 지하철 관련 교통 API만 조회
     * 
     * @param domainName 도메인 이름 (Path Variable)
     * @param keywordName 키워드 이름 (Path Variable)
     * @return 해당 도메인과 키워드 조건에 매칭되는 API 명세 목록 (200 OK)
     */
    @Operation(summary = "도메인+키워드 이름별 외부 API 명세 조회", 
               description = "특정 도메인 이름 내에서 특정 키워드 이름에 해당하는 외부 API 명세들을 정확하게 조회합니다.")
    @GetMapping("/by-domain-name/{domainName}/keyword-name/{keywordName}")
    public ResponseEntity<List<ExternalApiSpecResponseDto>> getExternalApiSpecsByDomainAndKeywordName(
            @Parameter(description = "도메인 이름", example = "finance", required = true) @PathVariable String domainName,
            @Parameter(description = "키워드 이름", example = "stock_price", required = true) @PathVariable String keywordName) {
        log.info("Getting external API specs by domain name: {} and keyword name: {}", domainName, keywordName);
        
        List<ExternalApiSpecResponseDto> response = externalApiSpecService.getExternalApiSpecsByDomainAndKeywordName(domainName, keywordName);
        return ResponseEntity.ok(response);
    }

    /**
     * 쿼리 파라미터를 사용한 외부 API 명세 검색 (이름 기반)
     * -
     * 도메인 이름과 키워드 이름을 쿼리 파라미터로 전달하여 API 검색
     * 사용자 친화적인 이름을 사용한 RESTful한 복합 조건 검색 지원
     * 단일 및 다중 값 모두 지원
     * -
     * 사용 예시:
     * - GET /api/external-api-specs/search-by-name?domains=finance&keywords=stock_price
     * - GET /api/external-api-specs/search-by-name?domains=weather,finance&keywords=current_weather,stock_price
     * - GET /api/external-api-specs/search-by-name?domains=weather (키워드 없이 도메인만)
     * - GET /api/external-api-specs/search-by-name?keywords=current_weather (도메인 없이 키워드만)
     * 
     * @param domains 도메인 이름 리스트 (Query Parameter, 선택사항)
     * @param keywords 키워드 이름 리스트 (Query Parameter, 선택사항)
     * @return 검색 조건에 매칭되는 API 명세 목록 (200 OK)
     */
    @Operation(summary = "외부 API 명세 검색 (이름 기반)", 
               description = "도메인 이름과 키워드 이름을 사용하여 외부 API 명세들을 검색합니다. 단일 또는 다중 조건 모두 지원합니다.")
    @GetMapping("/search-by-name")
    public ResponseEntity<List<ExternalApiSpecResponseDto>> searchExternalApiSpecsByName(
            @Parameter(description = "도메인 이름 (선택사항, 다중 값 지원)", example = "finance,weather") @RequestParam(required = false) List<String> domains,
            @Parameter(description = "키워드 이름 (선택사항, 다중 값 지원)", example = "stock_price,current_weather") @RequestParam(required = false) List<String> keywords) {
        
        log.info("다음 기준으로 외부API를 검색합니다. domains: {} and keywords: {}", domains, keywords);
        
        List<ExternalApiSpecResponseDto> response = externalApiSpecService.searchExternalApiSpecsByNames(domains, keywords);
        
        return ResponseEntity.ok(response);
    }
}