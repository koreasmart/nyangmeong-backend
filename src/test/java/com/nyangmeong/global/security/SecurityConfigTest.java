package com.nyangmeong.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.domain.AuthMember;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;

import jakarta.servlet.http.Cookie;

/**
 * [검사 - Spring Security 설정]
 * - 실제 필터 체인, 회원 조회만 Mock
 */
@WebMvcTest(controllers = SecurityConfigTest.PingController.class)
@Import({SecurityConfigTest.PingController.class, SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class, ErrorResponseWriter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, SensitiveDataMasker.class,
        GlobalExceptionHandler.class})
class SecurityConfigTest {

    @RestController
    static class PingController {

        @GetMapping("/api/ping")
        String get(@AuthenticationPrincipal LoginMember me) {
            return "id=" + me.id() + ",authrt=" + me.mbrAuthrtCd();
        }

        @PostMapping("/api/ping")
        String post() {
            return "ok";
        }

        @GetMapping("/api/admin/ping")
        String admin() {
            return "admin";
        }
    }

    /** 로그인 세션 번호 */
    private static final Long SID = 5L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    JwtProperties jwtProperties;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private static AuthMember member(String status, String authority) {
        AuthMember member = new AuthMember();
        member.setMbrSn(1L);
        member.setMbrSttsCd(status);
        member.setMbrAuthrtCd(authority);
        return member;
    }

    private Cookie accessCookie(Long mbrSn) {
        return new Cookie(TokenCookies.ACCESS_TOKEN, jwtTokenProvider.createAccessToken(mbrSn, SID));
    }

    private Cookie expiredCookie() {
        JwtProperties expired = new JwtProperties();
        expired.setSecretBase64(jwtProperties.getSecretBase64());
        expired.setAccessTokenExpiryMs(-1_000);
        JwtTokenProvider provider = new JwtTokenProvider(expired);
        provider.initializeKey();
        return new Cookie(TokenCookies.ACCESS_TOKEN, provider.createAccessToken(1L, SID));
    }

    @Test
    void 쿠키_없이_보호_경로를_부르면_401_UNAUTHORIZED_JSON이다() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.httpCode").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/ping"));
    }

    @Test
    void 만료된_토큰은_401_TOKEN_EXPIRED다() throws Exception {
        mockMvc.perform(get("/api/ping").cookie(expiredCookie()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void 유효한_토큰과_정상_회원이면_LoginMember를_받는다() throws Exception {
        when(authMemberMapper.selectForSession(1L, SID)).thenReturn(member(MemberStatus.NORMAL.getCode(), MemberAuthority.USER.getCode()));

        mockMvc.perform(get("/api/ping").cookie(accessCookie(1L)))
                .andExpect(status().isOk())
                .andExpect(content().string("id=1,authrt=USRC0004"));
    }

    @Test
    void 폐기된_세션의_토큰은_401_UNAUTHORIZED다() throws Exception {
        when(authMemberMapper.selectForSession(1L, SID)).thenReturn(null);

        mockMvc.perform(get("/api/ping").cookie(accessCookie(1L)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 정지_탈퇴_회원은_403_MEMBER_INACTIVE와_쿠키_삭제_헤더다() throws Exception {
        when(authMemberMapper.selectForSession(1L, SID)).thenReturn(member(MemberStatus.WITHDRAWN.getCode(), MemberAuthority.USER.getCode()));

        mockMvc.perform(get("/api/ping").cookie(accessCookie(1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_INACTIVE"))
                .andExpect(header().stringValues("Set-Cookie", org.hamcrest.Matchers.hasItems(
                        org.hamcrest.Matchers.containsString("access_token=;"),
                        org.hamcrest.Matchers.containsString("refresh_token=;"))));
    }

    @Test
    void 관리자_경로는_일반회원이면_403_ADMIN_ONLY_관리자면_200이다() throws Exception {
        when(authMemberMapper.selectForSession(1L, SID)).thenReturn(member(MemberStatus.NORMAL.getCode(), MemberAuthority.USER.getCode()));
        mockMvc.perform(get("/api/admin/ping").cookie(accessCookie(1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));

        when(authMemberMapper.selectForSession(1L, SID)).thenReturn(member(MemberStatus.NORMAL.getCode(), MemberAuthority.ADMIN.getCode()));
        mockMvc.perform(get("/api/admin/ping").cookie(accessCookie(1L)))
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));
    }

    @Test
    void 변경_요청에_X_Requested_With가_없으면_403_FORBIDDEN이다() throws Exception {
        when(authMemberMapper.selectForSession(1L, SID)).thenReturn(member(MemberStatus.NORMAL.getCode(), MemberAuthority.USER.getCode()));

        mockMvc.perform(post("/api/ping").cookie(accessCookie(1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/ping").cookie(accessCookie(1L)).header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk());
    }

    @Test
    void 열린_경로는_쿠키_없이_인증에_막히지_않는다() throws Exception {
        for (String path : new String[] {"/v3/api-docs", "/swagger-ui/index.html", "/api/auth/dev-tokens"}) {
            int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertThat(status).as(path).isNotIn(401, 403);
        }
    }

    @Test
    void 화이트리스트_밖은_auth_아래나_api_밖이어도_401이다() throws Exception {
        for (String path : new String[] {"/api/auth/unknown", "/files/1", "/"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    @Test
    void 화이트리스트의_인증_경로는_쿠키_없이_인증에_막히지_않는다() throws Exception {
        for (String path : new String[] {"/api/auth/login", "/api/auth/refresh", "/api/auth/logout"}) {
            int status = mockMvc.perform(post(path).header("X-Requested-With", "XMLHttpRequest"))
                    .andReturn().getResponse().getStatus();
            assertThat(status).as(path).isNotIn(401, 403);
        }
    }

    @Test
    void auth_me는_열린_auth_경로_중_예외라_로그인이_필요하다() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
