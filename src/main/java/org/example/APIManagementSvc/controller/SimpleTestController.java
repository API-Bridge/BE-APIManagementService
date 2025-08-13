package org.example.APIManagementSvc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller 스캔 테스트용 매우 간단한 Controller
 */
@RestController
public class SimpleTestController {

    @GetMapping("/simple-test")
    public String simpleTest() {
        return "Simple Controller 스캔 테스트 성공!";
    }
}
