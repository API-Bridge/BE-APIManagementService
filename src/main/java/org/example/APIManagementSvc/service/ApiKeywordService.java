package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ApiDomain;
import org.example.APIManagementSvc.domain.Entity.ApiKeyword;
import org.example.APIManagementSvc.dto.ApiKeywordDto;
import org.example.APIManagementSvc.repository.ApiDomainRepository;
import org.example.APIManagementSvc.repository.ApiKeywordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiKeywordService {

    private final ApiKeywordRepository apiKeywordRepository;
    private final ApiDomainRepository apiDomainRepository;

    @Transactional
    public ApiKeywordDto createKeyword(ApiKeywordDto keywordDto) {
        log.info("키워드 생성: {}", keywordDto.getKeywordName());
        
        if (apiKeywordRepository.existsByKeywordName(keywordDto.getKeywordName())) {
            throw new IllegalArgumentException("이미 존재하는 키워드명입니다: " + keywordDto.getKeywordName());
        }
        
        ApiDomain domain = apiDomainRepository.findById(keywordDto.getDomainId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도메인 ID: " + keywordDto.getDomainId()));
        
        ApiKeyword keyword = ApiKeyword.builder()
                .keywordName(keywordDto.getKeywordName())
                .description(keywordDto.getDescription())
                .domain(domain)
                .build();
        
        ApiKeyword savedKeyword = apiKeywordRepository.save(keyword);
        return convertToDto(savedKeyword);
    }

    public ApiKeywordDto getKeyword(Integer keywordId) {
        log.info("키워드 단건 조회: {}", keywordId);
        
        ApiKeyword keyword = apiKeywordRepository.findById(keywordId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 키워드 ID: " + keywordId));
        
        return convertToDto(keyword);
    }

    public Page<ApiKeywordDto> getAllKeywords(Pageable pageable) {
        log.info("모든 키워드 페이징 조회");
        
        return apiKeywordRepository.findAll(pageable)
                .map(this::convertToDto);
    }

    public List<ApiKeywordDto> getAllKeywords() {
        log.info("모든 키워드 목록 조회");
        
        return apiKeywordRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ApiKeywordDto> getKeywordsByDomain(Integer domainId) {
        log.info("도메인별 키워드 조회: {}", domainId);
        
        return apiKeywordRepository.findByDomain_DomainId(domainId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiKeywordDto updateKeyword(Integer keywordId, ApiKeywordDto keywordDto) {
        log.info("키워드 수정: {}", keywordId);
        
        ApiKeyword keyword = apiKeywordRepository.findById(keywordId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 키워드 ID: " + keywordId));
        
        if (keywordDto.getKeywordName() != null && !keywordDto.getKeywordName().equals(keyword.getKeywordName())) {
            if (apiKeywordRepository.existsByKeywordName(keywordDto.getKeywordName())) {
                throw new IllegalArgumentException("이미 존재하는 키워드명입니다: " + keywordDto.getKeywordName());
            }
            keyword.setKeywordName(keywordDto.getKeywordName());
        }
        
        if (keywordDto.getDescription() != null) {
            keyword.setDescription(keywordDto.getDescription());
        }
        
        if (keywordDto.getDomainId() != null && !keywordDto.getDomainId().equals(keyword.getDomain().getDomainId())) {
            ApiDomain newDomain = apiDomainRepository.findById(keywordDto.getDomainId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도메인 ID: " + keywordDto.getDomainId()));
            keyword.setDomain(newDomain);
        }
        
        ApiKeyword updatedKeyword = apiKeywordRepository.save(keyword);
        return convertToDto(updatedKeyword);
    }

    @Transactional
    public void deleteKeyword(Integer keywordId) {
        log.info("키워드 삭제: {}", keywordId);
        
        ApiKeyword keyword = apiKeywordRepository.findById(keywordId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 키워드 ID: " + keywordId));
        
        if (!keyword.getApiSpecs().isEmpty()) {
            throw new IllegalStateException("API 명세가 존재하는 키워드는 삭제할 수 없습니다");
        }
        
        apiKeywordRepository.delete(keyword);
    }

    private ApiKeywordDto convertToDto(ApiKeyword keyword) {
        return ApiKeywordDto.builder()
                .keywordId(keyword.getKeywordId())
                .keywordName(keyword.getKeywordName())
                .description(keyword.getDescription())
                .domainId(keyword.getDomain().getDomainId())
                .domainName(keyword.getDomain().getDomainName())
                .createdAt(keyword.getCreatedAt())
                .apiSpecCount(keyword.getApiSpecs() != null ? keyword.getApiSpecs().size() : 0)
                .build();
    }
}