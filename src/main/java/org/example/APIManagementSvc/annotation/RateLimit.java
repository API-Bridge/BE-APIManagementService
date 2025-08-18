package org.example.APIManagementSvc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * API 엔드포인트에 Rate Limiting을 적용하기 위한 커스텀 어노테이션입니다.
 * 
 * 이 어노테이션을 컨트롤러 메서드에 적용하면 지정된 시간 동안
 * 최대 호출 횟수를 제한할 수 있습니다.
 * 
 * ⚠️  중요: 구체적인 한도(횟수)를 정확하게 설정할 수 있습니다!
 * - value: 허용되는 구체적인 횟수 (예: 5, 100, 1000)
 * - timeUnit: 구체적인 시간 단위 (예: 초, 분, 시간, 일)
 * - keyType: 구체적인 제한 기준 (예: IP, API키, 사용자ID, 세션)
 * 
 * 사용 예시:
 * <pre>
 * // 1시간에 최대 100회 (구체적인 한도 설정)
 * @RateLimit(value = 100, timeUnit = TimeUnit.HOURS)
 * public ApiResponse getApiData() { ... }
 * 
 * // 1분에 최대 60회 (구체적인 한도 설정)
 * @RateLimit(value = 60, timeUnit = TimeUnit.MINUTES)
 * public ApiResponse searchApi() { ... }
 * 
 * // 1일에 최대 1000회 (구체적인 한도 설정)
 * @RateLimit(value = 1000, timeUnit = TimeUnit.DAYS)
 * public ApiResponse getStatistics() { ... }
 * </pre>
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 지정된 시간 동안 허용되는 최대 API 호출 횟수
     * 
     * @return 최대 호출 횟수
     */
    int value();
    
    /**
     * Rate Limiting을 적용할 시간 단위
     * 
     * @return 시간 단위 (기본값: HOURS)
     */
    TimeUnit timeUnit() default TimeUnit.HOURS;
    
    /**
     * Rate Limiting을 적용할 키 타입
     * 
     * @return 키 타입 (기본값: IP_ADDRESS)
     */
    KeyType keyType() default KeyType.IP_ADDRESS;
    
    /**
     * Rate Limiting 키 타입을 정의하는 열거형
     */
    enum KeyType {
        /**
         * 클라이언트 IP 주소 기반 제한
         */
        IP_ADDRESS,
        
        /**
         * API 키 기반 제한 (SGIS API 키 사용)
         */
        API_KEY,
        
        /**
         * 사용자 ID 기반 제한
         */
        USER_ID,
        
        /**
         * 세션 기반 제한
         */
        SESSION
    }
}
