package org.example.APIManagementSvc.controller;

import org.example.APIManagementSvc.dto.externalapi.ExternalApiRegisterRequest;
import org.example.APIManagementSvc.dto.externalapi.ExternalApiResponse;
import org.example.APIManagementSvc.dto.externalapi.ApiParameterRegisterRequest;
import org.example.APIManagementSvc.dto.externalapi.ApiParameterResponse;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.dto.common.PageResponse;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API CRUD 통합 테스트
 * 실제 HTTP 요청을 통해 전체 시스템을 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
class ApiCrudIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";
    }

    @Test
    @Order(1)
    void testRegisterApi() {
        // 테스트용 API 등록 데이터
        ExternalApiRegisterRequest request = ExternalApiRegisterRequest.builder()
                .apiName("테스트 날씨 API")
                .apiUrl("https://api.test-weather.com/current")
                .apiIssuer("Test Weather Service")
                .apiDescription("테스트용 날씨 정보 API")
                .httpMethod("GET")
                .apiDomain(ApiDomain.WEATHER)
                .apiKeyword(ApiKeyword.CURRENT_WEATHER)
                .parameters(Arrays.asList(
                        ApiParameterRegisterRequest.builder()
                                .paramName("city")
                                .paramType("String")
                                .isRequired(true)
                                .build(),
                        ApiParameterRegisterRequest.builder()
                                .paramName("units")
                                .paramType("String")
                                .isRequired(false)
                                .defaultValue("metric")
                                .build()
                ))
                .build();

        // API 등록 요청
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/external-apis",
                request,
                String.class
        );

        // 응답 검증
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("테스트 날씨 API"));
        
        System.out.println("API 등록 성공: " + response.getBody());
    }

    @Test
    @Order(2)
    void testGetApis() {
        try {
            // API 목록 조회
            ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + "/external-apis?page=0&size=10",
                    String.class
            );

            // 응답 검증
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().contains("테스트 날씨 API"));
            
            System.out.println("API 목록 조회 성공: " + response.getBody());
            
        } catch (Exception e) {
            System.err.println("API 목록 조회 실패: " + e.getMessage());
            e.printStackTrace();
            fail("API 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    void testGetApiStatistics() {
        // API 통계 조회
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/external-apis/statistics",
                String.class
        );

        // 응답 검증
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("totalApis"));
        
        System.out.println("API 통계 조회 성공: " + response.getBody());
    }
}
