package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ApiDomain;
import org.example.APIManagementSvc.domain.Entity.ApiKeyword;
import org.example.APIManagementSvc.dto.ApiDomainDto;
import org.example.APIManagementSvc.dto.ApiKeywordDto;
import org.example.APIManagementSvc.dto.ApiKeywordRequestDto;
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
public class ApiDomainService {

    private final ApiDomainRepository apiDomainRepository;
    private final ApiKeywordRepository apiKeywordRepository;

    @Transactional
    public ApiDomainDto createDomain(ApiDomainDto domainDto) {
        log.info("도메인 생성: {}", domainDto.getDomainName());
        
        if (apiDomainRepository.existsByDomainName(domainDto.getDomainName())) {
            throw new IllegalArgumentException("이미 존재하는 도메인명입니다: " + domainDto.getDomainName());
        }
        
        ApiDomain domain = ApiDomain.builder()
                .domainName(domainDto.getDomainName())
                .description(domainDto.getDescription())
                .build();
        
        ApiDomain savedDomain = apiDomainRepository.save(domain);
        
        // 키워드들 일괄 생성
        if (domainDto.getKeywords() != null && !domainDto.getKeywords().isEmpty()) {
            List<ApiKeyword> keywords = domainDto.getKeywords().stream()
                    .map(keywordRequestDto -> {
                        // 키워드명 중복 검사
                        if (apiKeywordRepository.existsByKeywordName(keywordRequestDto.getKeywordName())) {
                            throw new IllegalArgumentException("이미 존재하는 키워드명입니다: " + keywordRequestDto.getKeywordName());
                        }
                        
                        return ApiKeyword.builder()
                                .keywordName(keywordRequestDto.getKeywordName())
                                .description(keywordRequestDto.getDescription())
                                .domain(savedDomain)
                                .build();
                    })
                    .collect(Collectors.toList());
                    
            apiKeywordRepository.saveAll(keywords);
        }
        
        return convertToDto(savedDomain);
    }

    public ApiDomainDto getDomain(Integer domainId) {
        log.info("도메인 단건 조회: {}", domainId);
        
        ApiDomain domain = apiDomainRepository.findById(domainId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도메인 ID: " + domainId));
        
        return convertToDto(domain);
    }

    public Page<ApiDomainDto> getAllDomains(Pageable pageable) {
        log.info("모든 도메인 페이징 조회");
        
        return apiDomainRepository.findAll(pageable)
                .map(this::convertToDto);
    }

    public List<ApiDomainDto> getAllDomains() {
        log.info("모든 도메인 목록 조회");
        
        return apiDomainRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiDomainDto updateDomain(Integer domainId, ApiDomainDto domainDto) {
        log.info("도메인 수정: {}", domainId);
        
        ApiDomain domain = apiDomainRepository.findById(domainId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도메인 ID: " + domainId));
        
        if (domainDto.getDomainName() != null && !domainDto.getDomainName().equals(domain.getDomainName())) {
            if (apiDomainRepository.existsByDomainName(domainDto.getDomainName())) {
                throw new IllegalArgumentException("이미 존재하는 도메인명입니다: " + domainDto.getDomainName());
            }
            domain.setDomainName(domainDto.getDomainName());
        }
        
        if (domainDto.getDescription() != null) {
            domain.setDescription(domainDto.getDescription());
        }
        
        ApiDomain updatedDomain = apiDomainRepository.save(domain);
        return convertToDto(updatedDomain);
    }

    @Transactional
    public void deleteDomain(Integer domainId) {
        log.info("도메인 삭제: {}", domainId);
        
        ApiDomain domain = apiDomainRepository.findById(domainId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도메인 ID: " + domainId));
        
        if (!domain.getApiSpecs().isEmpty()) {
            throw new IllegalStateException("API 명세가 존재하는 도메인은 삭제할 수 없습니다");
        }
        
        // 키워드들 먼저 삭제 (CASCADE 설정이 되어있지만 명시적으로 처리)
        if (!domain.getKeywords().isEmpty()) {
            apiKeywordRepository.deleteAll(domain.getKeywords());
        }
        
        apiDomainRepository.delete(domain);
    }

    private ApiDomainDto convertToDto(ApiDomain domain) {
        List<ApiKeywordDto> keywordDetails = domain.getKeywords() != null ? 
            domain.getKeywords().stream()
                .map(keyword -> ApiKeywordDto.builder()
                    .keywordId(keyword.getKeywordId())
                    .keywordName(keyword.getKeywordName())
                    .description(keyword.getDescription())
                    .domainId(keyword.getDomain().getDomainId())
                    .domainName(keyword.getDomain().getDomainName())
                    .createdAt(keyword.getCreatedAt())
                    .apiSpecCount(keyword.getApiSpecs() != null ? keyword.getApiSpecs().size() : 0)
                    .build())
                .collect(Collectors.toList()) : List.of();
                
        return ApiDomainDto.builder()
                .domainId(domain.getDomainId())
                .domainName(domain.getDomainName())
                .description(domain.getDescription())
                .createdAt(domain.getCreatedAt())
                .keywordCount(domain.getKeywords() != null ? domain.getKeywords().size() : 0)
                .apiSpecCount(domain.getApiSpecs() != null ? domain.getApiSpecs().size() : 0)
                .keywordDetails(keywordDetails)
                .build();
    }
}