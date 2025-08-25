package org.example.APIManagementSvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * REST 통신 설정 클래스
 * 외부 API 호출을 위한 RestTemplate 구성
 * 
 * 주요 기능:
 * - HTTP 클라이언트 타임아웃 설정
 * - 헬스체크 및 외부 API 호출용 RestTemplate 제공
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 헬스체크용 RestTemplate Bean 생성
     * 
     * 외부 API 헬스체크를 위한 HTTP 클라이언트 구성
     * 빠른 응답과 안정적인 연결을 위해 타임아웃을 설정
     * 
     * 타임아웃 설정:
     * - 연결 타임아웃: 5초 (서버 연결 대기 시간)
     * - 읽기 타임아웃: 10초 (응답 데이터 읽기 대기 시간)
     * 
     * @return RestTemplate HTTP 클라이언트 빈
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // 연결 타임아웃: 5초 - 외부 API 서버 연결 시도 제한 시간
        factory.setConnectTimeout(5000);
        
        // 읽기 타임아웃: 10초 - API 응답 대기 제한 시간  
        factory.setReadTimeout(10000);
        
        return new RestTemplate(factory);
    }
}