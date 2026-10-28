package com.nyangmeong.global.security.handler;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.nyangmeong.global.error.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * [권한 없음 응답]
 * - 403, 관리자 경로는 ADMIN_ONLY
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    public static final String ADMIN_PATH_PREFIX = "/api/admin/";

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        ErrorCode errorCode = request.getRequestURI().startsWith(ADMIN_PATH_PREFIX) ? ErrorCode.ADMIN_ONLY
                : ErrorCode.FORBIDDEN;
        errorResponseWriter.write(request, response, errorCode);
    }
}
