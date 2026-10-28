package com.nyangmeong.global.security.filter;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * [X-Requested-With 검사 필터]
 * - 상태 변경 요청에 헤더 없으면 403 (CSRF 토큰 대체)
 */
@RequiredArgsConstructor
public class RequestedWithFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Requested-With";
    public static final String EXPECTED_VALUE = "XMLHttpRequest";

    private static final Set<String> SAFE_METHODS = Set.of(HttpMethod.GET.name(), HttpMethod.HEAD.name(),
            HttpMethod.OPTIONS.name());

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!SAFE_METHODS.contains(request.getMethod()) && !EXPECTED_VALUE.equals(request.getHeader(HEADER))) {
            errorResponseWriter.write(request, response, ErrorCode.FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
