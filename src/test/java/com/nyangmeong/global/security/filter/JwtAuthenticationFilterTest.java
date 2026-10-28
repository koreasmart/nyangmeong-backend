package com.nyangmeong.global.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.domain.AuthMember;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;

/**
 * [검사 - JWT 인증 필터]
 */
class JwtAuthenticationFilterTest {

    private JwtTokenProvider provider;
    private JwtTokenProvider expiredProvider;
    private AuthMemberMapper mapper;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecretBase64(Base64.getEncoder().encodeToString("jwt-secret-for-tests-32-bytes-long!!".getBytes()));
        provider = new JwtTokenProvider(properties);
        provider.initializeKey();

        JwtProperties expired = new JwtProperties();
        expired.setSecretBase64(properties.getSecretBase64());
        expired.setAccessTokenExpiryMs(-1_000);
        expiredProvider = new JwtTokenProvider(expired);
        expiredProvider.initializeKey();

        mapper = mock(AuthMemberMapper.class);
        TokenCookies cookies = new TokenCookies(properties);
        filter = new JwtAuthenticationFilter(provider, cookies, mapper,
                new ErrorResponseWriter(JsonMapper.builder().build(), new SensitiveDataMasker()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static AuthMember member(String status, String authority) {
        AuthMember member = new AuthMember();
        member.setMbrSn(7L);
        member.setMbrSttsCd(status);
        member.setMbrAuthrtCd(authority);
        return member;
    }

    private static MockHttpServletRequest requestWith(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/pets");
        if (token != null) {
            request.setCookies(new Cookie(TokenCookies.ACCESS_TOKEN, token));
        }
        return request;
    }

    @Test
    void 쿠키가_없으면_조회_없이_미인증으로_통과한다() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWith(null), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(mapper, never()).selectForSession(anyLong(), anyLong());
    }

    @Test
    void 유효한_토큰과_정상_회원이면_LoginMember가_principal이_된다() throws Exception {
        when(mapper.selectForSession(7L, 3L)).thenReturn(member(MemberStatus.NORMAL.getCode(), MemberAuthority.ADMIN.getCode()));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWith(provider.createAccessToken(7L, 3L)), new MockHttpServletResponse(), chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(LoginMember.class);
        LoginMember me = (LoginMember) authentication.getPrincipal();
        assertThat(me.id()).isEqualTo(7L);
        assertThat(me.mbrAuthrtCd()).isEqualTo(MemberAuthority.ADMIN.getCode());
        assertThat(me.isAdmin()).isTrue();
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void 만료된_토큰은_TOKEN_EXPIRED를_요청_속성에_남기고_미인증으로_통과한다() throws Exception {
        MockHttpServletRequest request = requestWith(expiredProvider.createAccessToken(7L, 3L));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_CODE)).isEqualTo(ErrorCode.TOKEN_EXPIRED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verify(mapper, never()).selectForSession(anyLong(), anyLong());
    }

    @Test
    void 회원_행이_없으면_UNAUTHORIZED를_남기고_미인증으로_통과한다() throws Exception {
        when(mapper.selectForSession(7L, 3L)).thenReturn(null);
        MockHttpServletRequest request = requestWith(provider.createAccessToken(7L, 3L));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_CODE)).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void 폐기되거나_만료된_세션이면_UNAUTHORIZED를_남기고_미인증으로_통과한다() throws Exception {
        when(mapper.selectForSession(7L, 3L)).thenReturn(null);
        MockHttpServletRequest request = requestWith(provider.createAccessToken(7L, 3L));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(mapper).selectForSession(7L, 3L);
        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_CODE)).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void Refresh_토큰을_Access_쿠키에_넣으면_조회_없이_UNAUTHORIZED다() throws Exception {
        MockHttpServletRequest request = requestWith(provider.createRefreshToken(7L, 3L));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_CODE)).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(mapper, never()).selectForSession(anyLong(), anyLong());
    }

    @Test
    void 정지_탈퇴_회원은_403_MEMBER_INACTIVE와_쿠키_삭제로_끝낸다() throws Exception {
        when(mapper.selectForSession(7L, 3L)).thenReturn(member(MemberStatus.SUSPENDED.getCode(), MemberAuthority.USER.getCode()));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWith(provider.createAccessToken(7L, 3L)), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":\"MEMBER_INACTIVE\"");
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(2)
                .allMatch(h -> h.contains("Max-Age=0"));
        assertThat(chain.getRequest()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
