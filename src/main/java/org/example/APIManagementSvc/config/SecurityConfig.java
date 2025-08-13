package org.example.APIManagementSvc.config;

/**
 * Spring Security 설정 클래스
 * 현재 테스트를 위해 Spring Security를 완전히 비활성화함
 * Controller 스캔 문제 해결 후 복구 예정
 */
// @Configuration // 테스트를 위해 비활성화
// @EnableWebSecurity // 테스트를 위해 비활성화
public class SecurityConfig {

    /**
     * Spring Security 필터 체인 설정
     * 현재는 Spring Security를 완전히 비활성화 (테스트용)
     */
    // @Bean // 테스트를 위해 비활성화
    // public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    //     http
    //         .csrf(csrf -> csrf.disable())
    //         .authorizeHttpRequests(authz -> authz
    //             .anyRequest().permitAll() // 모든 요청 허용 (테스트용)
    //         );
    //
    //     return http.build();
    // }
}