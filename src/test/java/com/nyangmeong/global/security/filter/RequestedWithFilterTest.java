package com.nyangmeong.global.security.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;

import tools.jackson.databind.json.JsonMapper;

/**
 * [검사 - X-Requested-With 필터]
 */
class RequestedWithFilterTest {

    private final RequestedWithFilter filter = new RequestedWithFilter(
            new ErrorResponseWriter(JsonMapper.builder().build(), new SensitiveDataMasker()));

    @Test
    void 헤더_없는_POST는_403_FORBIDDEN_JSON이고_체인이_끊긴다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/posts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("\"code\":\"FORBIDDEN\"").contains("\"status\":\"error\"");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void 헤더가_있는_POST와_GET은_통과한다() throws Exception {
        MockHttpServletRequest post = new MockHttpServletRequest("POST", "/api/posts");
        post.addHeader("X-Requested-With", "XMLHttpRequest");
        MockFilterChain postChain = new MockFilterChain();
        filter.doFilter(post, new MockHttpServletResponse(), postChain);
        assertThat(postChain.getRequest()).isNotNull();

        MockFilterChain getChain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/posts"), new MockHttpServletResponse(), getChain);
        assertThat(getChain.getRequest()).isNotNull();

        MockFilterChain optionsChain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("OPTIONS", "/api/posts"), new MockHttpServletResponse(),
                optionsChain);
        assertThat(optionsChain.getRequest()).isNotNull();
    }
}
