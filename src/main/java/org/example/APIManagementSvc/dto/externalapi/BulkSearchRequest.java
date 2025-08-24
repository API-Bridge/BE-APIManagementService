package org.example.APIManagementSvc.dto.externalapi;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 다중 도메인과 키워드를 이용한 벌크 검색 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkSearchRequest {
    
    /** 검색할 도메인 목록 (소문자 코드로) */
    @NotEmpty(message = "최소 하나의 도메인이 필요합니다")
    private List<String> domains;
    
    /** 검색할 키워드 목록 (소문자 코드로) */
    @NotEmpty(message = "최소 하나의 키워드가 필요합니다")
    private List<String> keywords;
}