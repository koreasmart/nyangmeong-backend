package com.nyangmeong.global.response;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [표준 API 응답]
 * - 모든 응답을 동일한 구조로 통일
 *   : { timestamp, status, httpCode, message, path, code, data }
 * - @JsonInclude(NON_NULL) : null 필드는 JSON 에서 제외
 * - code : ErrorCode 상수 이름 (실패 응답에만)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        LocalDateTime timestamp,
        String status,
        int httpCode,
        String message,
        String path,
        String code,
        T data) {

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_ERROR = "error";

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(LocalDateTime.now(), STATUS_SUCCESS, HttpStatus.OK.value(), "ok", null, null, data);
    }

    public static <T> ApiResponse<T> created() {
        return created(null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(LocalDateTime.now(), STATUS_SUCCESS, HttpStatus.CREATED.value(), "created", null,
                null, data);
    }

    /**
     * ErrorCode 기준 실패 응답
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String path) {
        return error(errorCode, errorCode.message(), path);
    }

    /**
     * 메시지만 다른 실패 응답
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, String path) {
        return new ApiResponse<>(LocalDateTime.now(), STATUS_ERROR, errorCode.code(), message, path,
                errorCode.name(), null);
    }

    /**
     * 상세 목록(data)이 있는 실패 응답
     */
    public static <T> ApiResponse<T> errorWithData(ErrorCode errorCode, String path, T data) {
        return new ApiResponse<>(LocalDateTime.now(), STATUS_ERROR, errorCode.code(), errorCode.message(), path,
                errorCode.name(), data);
    }
}
