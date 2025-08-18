package org.example.APIManagementSvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.annotation.RateLimit;
import org.example.APIManagementSvc.service.RateLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * API 엔드포인트의 Rate Limiting을 처리하는 인터셉터입니다.
 * 
 * 이 인터셉터는 컨트롤러 메서드에 @RateLimit 어노테이션이 적용된
 * 요청에 대해 호출 횟수를 제한하고 모니터링합니다.
 * 
 * 주요 기능:
 * - IP 주소, API 키, 사용자 ID 등 다양한 키 타입 지원
 * - 시간 기반 호출 횟수 제한
 * - 제한 초과 시 적절한 HTTP 상태 코드 반환
 * - Rate Limiting 이력 로깅
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // HandlerMethod가 아닌 경우 (정적 리소스 등) 통과
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        
        // @RateLimit 어노테이션이 없는 경우 통과
        if (rateLimit == null) {
            return true;
        }
        
        // Rate Limiting 키 생성
        String key = generateRateLimitKey(request, rateLimit);
        
        // Rate Limiting 검사
        boolean allowed = rateLimitService.isAllowed(key, rateLimit.value(), rateLimit.timeUnit());
        
        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}, limit: {} per {}", 
                    key, rateLimit.value(), rateLimit.timeUnit());
            
            // Rate Limiting 제한 초과 시 429 (Too Many Requests) 반환
            response.setStatus(429); // SC_TOO_MANY_REQUESTS
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}"
            );
            
            return false;
        }
        
        log.debug("Rate limit check passed for key: {}, remaining: {} per {}", 
                key, rateLimit.value(), rateLimit.timeUnit());
        
        return true;
    }
    
    /**
     * Rate Limiting 키를 생성합니다.
     * 
     * @param request HTTP 요청 객체
     * @param rateLimit RateLimit 어노테이션
     * @return Rate Limiting 키
     */
    private String generateRateLimitKey(HttpServletRequest request, RateLimit rateLimit) {
        String baseKey = request.getRequestURI() + ":" + rateLimit.keyType().name();
        
        switch (rateLimit.keyType()) {
            case IP_ADDRESS:
                return baseKey + ":" + getClientIpAddress(request);
                
            case API_KEY:
                String apiKey = extractApiKey(request);
                return baseKey + ":" + (apiKey != null ? apiKey : "anonymous");
                
            case USER_ID:
                String userId = extractUserId(request);
                return baseKey + ":" + (userId != null ? userId : "anonymous");
                
            case SESSION:
                String sessionId = request.getSession().getId();
                return baseKey + ":" + sessionId;
                
            default:
                return baseKey + ":" + getClientIpAddress(request);
        }
    }
    
    /**
     * 클라이언트의 실제 IP 주소를 추출합니다.
     * 프록시나 로드밸런서 뒤에서 실행되는 경우를 고려합니다.
     * 
     * @param request HTTP 요청 객체
     * @return 클라이언트 IP 주소
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * HTTP 요청에서 API 키를 추출합니다.
     * 
     * @param request HTTP 요청 객체
     * @return API 키 또는 null
     */
    private String extractApiKey(HttpServletRequest request) {
        // Authorization 헤더에서 Bearer 토큰 추출
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        
        // X-API-Key 헤더에서 추출
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }
        
        // 쿼리 파라미터에서 추출
        String queryApiKey = request.getParameter("apiKey");
        if (queryApiKey != null && !queryApiKey.isEmpty()) {
            return queryApiKey;
        }
        
        return null;
    }
    
    /**
     * HTTP 요청에서 사용자 ID를 추출합니다.
     * 
     * @param request HTTP 요청 객체
     * @return 사용자 ID 또는 null
     */
    private String extractUserId(HttpServletRequest request) {
        // X-User-ID 헤더에서 추출
        String userId = request.getHeader("X-User-ID");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }
        
        // 쿼리 파라미터에서 추출
        String queryUserId = request.getParameter("userId");
        if (queryUserId != null && !queryUserId.isEmpty()) {
            return queryUserId;
        }
        
        return null;
    }
}
