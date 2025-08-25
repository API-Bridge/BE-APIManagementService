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
     * 도메인별 외부 API 명세 조회
     * -
     * 특정 도메인에 속한 모든 외부 API를 조회
     * 도메인별로 API를 분류하여 관리할 때 사용
     * -
     * 사용 예시:
     * - 날씨 도메인의 모든 API (현재날씨, 일기예보, 기상특보)
     * - 교통 도메인의 모든 API (실시간 교통정보, 대중교통 정보)
     * 
     * @param domainId 도메인 ID (Path Variable)
     * @return 해당 도메인의 API 명세 목록 (200 OK)
     */
    @Operation(summary = "도메인별 외부 API 명세 조회", description = "특정 도메인에 속한 외부 API 명세들을 조회합니다.")
    @GetMapping("/by-domain/{domainId}")
    public ResponseEntity<List<ExternalApiSpecResponseDto>> getExternalApiSpecsByDomain(
            @Parameter(description = "도메인 ID", required = true) @PathVariable Integer domainId) {
        log.info("Getting external API specs by domain: {}", domainId);
        
        List<ExternalApiSpecResponseDto> response = externalApiSpecService.getExternalApiSpecsByDomain(domainId);
        return ResponseEntity.ok(response);
    }

    /**
     * 키워드별 외부 API 명세 조회
     * -
     * 특정 키워드에 속한 모든 외부 API를 조회
     * 세부 키워드별로 더 정확한 API 분류가 필요할 때 사용
     * -
     * 사용 예시:
     * - current_weather 키워드의 모든 API
     * - stock_price 키워드의 모든 API
     * 
     * @param keywordId 키워드 ID (Path Variable)
     * @return 해당 키워드의 API 명세 목록 (200 OK)
     */
    @Operation(summary = "키워드별 외부 API 명세 조회", description = "특정 키워드에 속한 외부 API 명세들을 조회합니다.")
    @GetMapping("/by-keyword/{keywordId}")
    public ResponseEntity<List<ExternalApiSpecResponseDto>> getExternalApiSpecsByKeyword(
            @Parameter(description = "키워드 ID", required = true) @PathVariable Integer keywordId) {
        log.info("Getting external API specs by keyword: {}", keywordId);
        
        List<ExternalApiSpecResponseDto> response = externalApiSpecService.getExternalApiSpecsByKeyword(keywordId);
        return ResponseEntity.ok(response);
    }
}