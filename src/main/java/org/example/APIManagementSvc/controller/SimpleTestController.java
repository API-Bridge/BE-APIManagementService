package org.example.APIManagementSvc.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

/**
 * 간단한 테스트를 위한 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/test")
public class SimpleTestController {
    
    @GetMapping("/hello")
    public ApiResponse<String> hello() {
        log.info("Hello 테스트 엔드포인트 호출");
        return ApiResponse.success("Hello! Redis 캐싱 테스트 컨트롤러가 정상적으로 등록되었습니다!");
    }
    
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        log.info("Ping 테스트 엔드포인트 호출");
        return ApiResponse.success("Pong! 서버가 정상 동작 중입니다.");
    }
}
