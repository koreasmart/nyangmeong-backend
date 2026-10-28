package com.nyangmeong.global.error;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [전역 예외 처리]
 * - 모든 예외를 ApiResponse 표준 포맷으로 변환
 * - 필터 단계 예외(401/403)는 global/security 의 ErrorResponseWriter 담당
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /** 타입 변환 실패(없는 enum 값 등)에 Java 기본 문구 대신 내보내는 문구 */
    private static final String INVALID_FORMAT_MESSAGE = "형식이 올바르지 않습니다.";

    private final SensitiveDataMasker masker;

    /** 검증 오류 항목 (field, message) */
    public record FieldErrorResponse(String field, String message) {
    }

    /**
     * 업무 예외 처리 핸들러 메소드
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBase(BaseException e, HttpServletRequest request) {
        logException(e);
        return respond(e.getErrorCode(), request);
    }

    /**
     * 검증 실패 처리 핸들러 메소드
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorResponse>>> handleValidation(MethodArgumentNotValidException e,
            HttpServletRequest request) {
        /** 로그에는 필드명/규칙 코드만 */
        String violations = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ":" + fieldError.getCode())
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
        log.warn("[Validation] path={} violations={}", path(request), violations);

        List<FieldErrorResponse> fields = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldErrorResponse(fieldError.getField(),
                        fieldError.isBindingFailure() ? INVALID_FORMAT_MESSAGE
                                : String.valueOf(fieldError.getDefaultMessage())))
                .toList();
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(errorCode.status())
                .body(ApiResponse.errorWithData(errorCode, path(request), fields));
    }

    /**
     * 요청 형식 오류 처리 핸들러 메소드
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e, HttpServletRequest request) {
        logException(e);
        return respond(ErrorCode.BAD_REQUEST, request);
    }

    /**
     * 경로 없음 처리 핸들러 메소드
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e,
            HttpServletRequest request) {
        logException(e);
        return respond(ErrorCode.NOT_FOUND, request);
    }

    /**
     * 미지원 HTTP 메서드 처리 핸들러 메소드
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e,
            HttpServletRequest request) {
        logException(e);
        return respond(ErrorCode.METHOD_NOT_ALLOWED, request);
    }

    /**
     * 미지원 Content-Type 처리 핸들러 메소드
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e,
            HttpServletRequest request) {
        logException(e);
        return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, request);
    }

    /**
     * 업로드 크기 초과 처리 핸들러 메소드
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e,
            HttpServletRequest request) {
        logException(e);
        return respond(ErrorCode.FILE_TOO_LARGE, request);
    }

    /**
     * 컨트롤러 내 인증 예외 처리 핸들러 메소드
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException e,
            HttpServletRequest request) {
        logException(e);
        return respond(ErrorCode.UNAUTHORIZED, request);
    }

    /**
     * 컨트롤러 내 권한 예외 처리 핸들러 메소드
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e,
            HttpServletRequest request) {
        logException(e);
        return respond(ErrorCode.FORBIDDEN, request);
    }

    /**
     * 전역 예외 처리 핸들러 메소드
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e, HttpServletRequest request) {
        logException(e);
        log.error("[Unexpected] path={}", path(request), e);
        return respond(ErrorCode.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * 예외 발생 지점(클래스/메서드/라인)을 포함한 에러 로그
     */
    private void logException(Exception e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        String message = masker.maskText(e.getMessage());
        if (stackTrace.length > 0) {
            StackTraceElement origin = stackTrace[0];
            log.error("[Exception] {} at {}.{} ({}:{}) - message={}", e.getClass().getSimpleName(),
                    origin.getClassName(), origin.getMethodName(), origin.getFileName(), origin.getLineNumber(),
                    message);
        } else {
            log.error("[Exception] {} - message={}", e.getClass().getSimpleName(), message);
        }
    }

    private ResponseEntity<ApiResponse<Void>> respond(ErrorCode errorCode, HttpServletRequest request) {
        return ResponseEntity.status(errorCode.status()).body(ApiResponse.error(errorCode, path(request)));
    }

    private String path(HttpServletRequest request) {
        return masker.maskUri(request.getRequestURI());
    }
}
