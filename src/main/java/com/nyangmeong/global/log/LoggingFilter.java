package com.nyangmeong.global.log;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [요청 로그 필터]
 * - 최전선(@Order 최상위)에서 traceId/clientIp 를 MDC 에 주입
 * - 요청 종료 시 한 줄 로그 후 MDC 정리
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {

    private final SensitiveDataMasker masker;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        MdcUtils.put(MdcUtils.TRACE_ID, MdcUtils.generateTraceId());
        MdcUtils.put(MdcUtils.CLIENT_IP, resolveClientIp(request));
        try {
            chain.doFilter(request, response);
        } finally {
            /** URI 마스킹 */
            log.info("{} {} -> {} ({}ms)", request.getMethod(), masker.maskUri(request.getRequestURI()),
                    response.getStatus(), System.currentTimeMillis() - start);
            MdcUtils.clear();
        }
    }

    /** 프록시 뒤 실제 클라이언트 IP */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
