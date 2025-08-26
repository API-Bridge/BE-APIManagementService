package org.example.APIManagementSvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.dto.ApiKeywordDto;
import org.example.APIManagementSvc.service.ApiKeywordService;
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
@RequestMapping("/api/keywords")
@RequiredArgsConstructor
@Validated
@Tag(name = "API Keyword Management", description = "API 키워드 관리 API")
public class ApiKeywordController {

    private final ApiKeywordService apiKeywordService;

    @Operation(summary = "키워드 생성", description = "새로운 API 키워드를 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiKeywordDto> createKeyword(@Valid @RequestBody ApiKeywordDto keywordDto) {
        log.info("키워드 생성 요청: {}", keywordDto.getKeywordName());
        
        ApiKeywordDto response = apiKeywordService.createKeyword(keywordDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "키워드 단건 조회", description = "키워드 ID로 특정 키워드를 조회합니다.")
    @GetMapping("/{keywordId}")
    public ResponseEntity<ApiKeywordDto> getKeyword(
            @Parameter(description = "키워드 ID", required = true) @PathVariable Integer keywordId) {
        log.info("키워드 단건 조회: {}", keywordId);
        
        ApiKeywordDto response = apiKeywordService.getKeyword(keywordId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "키워드 전체 조회 (페이징)", description = "등록된 모든 키워드를 페이징으로 조회합니다.")
    @GetMapping
    public ResponseEntity<Page<ApiKeywordDto>> getAllKeywords(
            @Parameter(description = "페이징 정보") @PageableDefault(size = 20) Pageable pageable) {
        log.info("키워드 전체 페이징 조회");
        
        Page<ApiKeywordDto> response = apiKeywordService.getAllKeywords(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "키워드 전체 목록 조회", description = "등록된 모든 키워드를 목록으로 조회합니다 (페이징 없음).")
    @GetMapping("/list")
    public ResponseEntity<List<ApiKeywordDto>> getAllKeywordsList() {
        log.info("키워드 전체 목록 조회");
        
        List<ApiKeywordDto> response = apiKeywordService.getAllKeywords();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "도메인별 키워드 조회", description = "특정 도메인에 속한 키워드들을 조회합니다.")
    @GetMapping("/by-domain/{domainId}")
    public ResponseEntity<List<ApiKeywordDto>> getKeywordsByDomain(
            @Parameter(description = "도메인 ID", required = true) @PathVariable Integer domainId) {
        log.info("도메인별 키워드 조회: {}", domainId);
        
        List<ApiKeywordDto> response = apiKeywordService.getKeywordsByDomain(domainId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "키워드 수정", description = "기존 키워드 정보를 수정합니다.")
    @PutMapping("/{keywordId}")
    public ResponseEntity<ApiKeywordDto> updateKeyword(
            @Parameter(description = "키워드 ID", required = true) @PathVariable Integer keywordId,
            @Valid @RequestBody ApiKeywordDto keywordDto) {
        log.info("키워드 수정: {}", keywordId);
        
        ApiKeywordDto response = apiKeywordService.updateKeyword(keywordId, keywordDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "키워드 삭제", description = "키워드를 삭제합니다. API 명세가 연결된 키워드는 삭제할 수 없습니다.")
    @DeleteMapping("/{keywordId}")
    public ResponseEntity<Void> deleteKeyword(
            @Parameter(description = "키워드 ID", required = true) @PathVariable Integer keywordId) {
        log.info("키워드 삭제: {}", keywordId);
        
        apiKeywordService.deleteKeyword(keywordId);
        return ResponseEntity.noContent().build();
    }
}