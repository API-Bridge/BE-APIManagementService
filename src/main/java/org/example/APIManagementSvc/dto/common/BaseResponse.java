package org.example.APIManagementSvc.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공통 API 응답 래퍼 클래스
 * 
 * 모든 REST API 응답을 일관된 형태로 제공하기 위한 래퍼 클래스
 * 성공/실패 여부, 메시지, 데이터를 표준화된 형식으로 반환
 * 
 * 응답 구조:
 * {
 *   "success": true,
 *   "message": "요청이 성공적으로 처리되었습니다",
 *   "data": { ... },
 *   "timestamp": "2023-12-01T10:30:00"
 * }
 * 
 * @param <T> 응답 데이터 타입
 * @author API Bridge Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseResponse<T> {
    
    /** 요청 처리 성공 여부 */
    @Builder.Default
    private boolean success = true;
    
    /** 응답 메시지 */
    private String message;
    
    /** 실제 응답 데이터 */
    private T data;
    
    /** 응답 생성 시각 */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 성공 응답 생성 (데이터 포함)
     */
    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.<T>builder()
                .success(true)
                .message("요청이 성공적으로 처리되었습니다")
                .data(data)
                .build();
    }
    
    /**
     * 성공 응답 생성 (커스텀 메시지, 데이터 포함)
     */
    public static <T> BaseResponse<T> success(String message, T data) {
        return BaseResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * 실패 응답 생성 (메시지만)
     */
    public static <T> BaseResponse<T> failure(String message) {
        return BaseResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
    
    /**
     * 실패 응답 생성 (메시지와 데이터)
     */
    public static <T> BaseResponse<T> failure(String message, T data) {
        return BaseResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .build();
    }
}