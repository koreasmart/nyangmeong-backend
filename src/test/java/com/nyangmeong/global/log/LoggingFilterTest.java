package com.nyangmeong.global.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * [검사 - 요청 로그 필터]
 */
class LoggingFilterTest {

    private final LoggingFilter filter = new LoggingFilter(new SensitiveDataMasker());

    @Test
    void 요청_동안_traceId와_clientIp가_MDC에_있고_끝나면_지워진다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/pets");
        request.setRemoteAddr("10.0.0.7");
        AtomicReference<String> traceIdInside = new AtomicReference<>();
        AtomicReference<String> clientIpInside = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
        }, (req, res, c) -> {
            traceIdInside.set(MDC.get(MdcUtils.TRACE_ID));
            clientIpInside.set(MDC.get(MdcUtils.CLIENT_IP));
            c.doFilter(req, res);
        });

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(traceIdInside.get()).hasSize(8);
        assertThat(clientIpInside.get()).isEqualTo("10.0.0.7");
        assertThat(MDC.get(MdcUtils.TRACE_ID)).isNull();
        assertThat(MDC.get(MdcUtils.CLIENT_IP)).isNull();
    }

    @Test
    void 프록시_뒤에서는_X_Forwarded_For의_첫_값을_쓴다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/pets");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        AtomicReference<String> clientIpInside = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
        }, (req, res, c) -> {
            clientIpInside.set(MDC.get(MdcUtils.CLIENT_IP));
            c.doFilter(req, res);
        });

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(clientIpInside.get()).isEqualTo("203.0.113.5");
    }
}
