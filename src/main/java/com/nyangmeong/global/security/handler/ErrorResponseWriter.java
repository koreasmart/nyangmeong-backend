package com.nyangmeong.global.security.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * [필터 단계 오류 응답]
 * - 401/403 을 ApiResponse JSON 으로 응답
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;
    private final SensitiveDataMasker masker;

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode)
            throws IOException {
        response.setStatus(errorCode.code());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponse.error(errorCode, masker.maskUri(request.getRequestURI()));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
