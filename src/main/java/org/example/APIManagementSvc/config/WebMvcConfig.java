package org.example.APIManagementSvc.config;

import org.example.APIManagementSvc.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 설정을 담당하는 설정 클래스입니다.
 * 
 * 주요 설정:
 * - Rate Limiting 인터셉터 등록
 * - CORS 설정
 * - 인터셉터 순서 및 경로 패턴 설정
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Rate Limiting 인터셉터 등록
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")  // API 경로에만 적용
                .excludePathPatterns(
                    "/api/v1/actuator/**",  // 헬스체크 등은 제외
                    "/api/v1/swagger-ui/**", // Swagger UI는 제외
                    "/api/v1/v3/api-docs/**" // API 문서는 제외
                )
                .order(1); // 높은 우선순위로 실행
    }
}
