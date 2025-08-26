package org.example.APIManagementSvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.dto.ApiDomainDto;
import org.example.APIManagementSvc.service.ApiDomainService;
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
@RequestMapping("/api/domains")
@RequiredArgsConstructor
@Validated
@Tag(name = "API Domain Management", description = "API 도메인 관리 API")
public class ApiDomainController {

    private final ApiDomainService apiDomainService;

    @Operation(summary = "도메인 생성 (키워드 포함)", description = "새로운 API 도메인을 생성하며, 동시에 키워드들도 함께 등록할 수 있습니다.")
    @PostMapping
    public ResponseEntity<ApiDomainDto> createDomain(@Valid @RequestBody ApiDomainDto domainDto) {
        log.info("도메인 생성 요청: {} (키워드 개수: {})", 
                domainDto.getDomainName(), 
                domainDto.getKeywords() != null ? domainDto.getKeywords().size() : 0);
        
        ApiDomainDto response = apiDomainService.createDomain(domainDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "도메인 단건 조회 (키워드 포함)", description = "도메인 ID로 특정 도메인과 소속 키워드들을 조회합니다.")
    @GetMapping("/{domainId}")
    public ResponseEntity<ApiDomainDto> getDomain(
            @Parameter(description = "도메인 ID", required = true) @PathVariable Integer domainId) {
        log.info("도메인 단건 조회: {}", domainId);
        
        ApiDomainDto response = apiDomainService.getDomain(domainId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "도메인 전체 조회 (페이징)", description = "등록된 모든 도메인을 페이징으로 조회합니다.")
    @GetMapping
    public ResponseEntity<Page<ApiDomainDto>> getAllDomains(
            @Parameter(description = "페이징 정보") @PageableDefault(size = 20) Pageable pageable) {
        log.info("도메인 전체 페이징 조회");
        
        Page<ApiDomainDto> response = apiDomainService.getAllDomains(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "도메인 전체 목록 조회", description = "등록된 모든 도메인을 목록으로 조회합니다 (페이징 없음).")
    @GetMapping("/list")
    public ResponseEntity<List<ApiDomainDto>> getAllDomainsList() {
        log.info("도메인 전체 목록 조회");
        
        List<ApiDomainDto> response = apiDomainService.getAllDomains();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "도메인 수정", description = "기존 도메인 정보를 수정합니다.")
    @PutMapping("/{domainId}")
    public ResponseEntity<ApiDomainDto> updateDomain(
            @Parameter(description = "도메인 ID", required = true) @PathVariable Integer domainId,
            @Valid @RequestBody ApiDomainDto domainDto) {
        log.info("도메인 수정: {}", domainId);
        
        ApiDomainDto response = apiDomainService.updateDomain(domainId, domainDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "도메인 삭제 (키워드 포함)", description = "도메인과 소속 키워드들을 함께 삭제합니다. API 명세가 연결된 도메인은 삭제할 수 없습니다.")
    @DeleteMapping("/{domainId}")
    public ResponseEntity<Void> deleteDomain(
            @Parameter(description = "도메인 ID", required = true) @PathVariable Integer domainId) {
        log.info("도메인 삭제: {}", domainId);
        
        apiDomainService.deleteDomain(domainId);
        return ResponseEntity.noContent().build();
    }
}