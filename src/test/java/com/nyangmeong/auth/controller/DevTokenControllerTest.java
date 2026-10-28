package com.nyangmeong.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nyangmeong.auth.service.AuthService;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.crypto.TokenHashUtil;
import com.nyangmeong.global.security.domain.AuthMember;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.security.domain.RefreshToken;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.global.security.session.LoginSessionManager;
import com.nyangmeong.global.security.session.RefreshTokenMapper;

import jakarta.servlet.http.Cookie;

/**
 * [검사 - 개발용 토큰 발급]
 * - prod 프로필에서는 경로 없음
 */
class DevTokenControllerTest {

    @Import({AuthService.class, FieldCryptoService.class, CryptoProperties.class, SecurityConfig.class,
            JwtTokenProvider.class, JwtProperties.class, TokenCookies.class, ErrorResponseWriter.class,
            RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, SensitiveDataMasker.class,
            GlobalExceptionHandler.class, LoginSessionManager.class, TokenHashUtil.class})
    abstract static class Base {

        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        AuthMemberMapper authMemberMapper;

        @MockitoBean
        RefreshTokenMapper refreshTokenMapper;

        MockHttpServletRequestBuilder issue(long mbrSn) {
            return post("/api/auth/dev-tokens")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mbrSn\":" + mbrSn + "}");
        }
    }

    @Nested
    @WebMvcTest(controllers = {DevTokenController.class, AuthController.class})
    class 기본_프로필 extends Base {

        @Autowired
        FieldCryptoService fieldCryptoService;

        /** 등록된 RFRSH_TKN 행 번호 */
        static final Long SID = 11L;

        private AuthMember normalMember() {
            AuthMember member = new AuthMember();
            member.setMbrSn(3L);
            member.setMbrSttsCd(MemberStatus.NORMAL.getCode());
            member.setMbrAuthrtCd(MemberAuthority.USER.getCode());
            return member;
        }

        private void givenInsertedRow() {
            doAnswer(invocation -> {
                ((RefreshToken) invocation.getArgument(0)).setRfrshTknSn(SID);
                return null;
            }).when(refreshTokenMapper).insert(any(RefreshToken.class), anyLong());
        }

        @Test
        void 있는_회원이면_access_refresh_쿠키_두_개를_내려준다() throws Exception {
            when(authMemberMapper.selectForAuth(3L)).thenReturn(normalMember());
            givenInsertedRow();

            mockMvc.perform(issue(3L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(header().stringValues("Set-Cookie", Matchers.hasItems(
                            Matchers.allOf(Matchers.startsWith("access_token="), Matchers.containsString("HttpOnly"),
                                    Matchers.containsString("Path=/")),
                            Matchers.allOf(Matchers.startsWith("refresh_token="),
                                    Matchers.containsString("Path=/api/auth")))));
        }

        @Test
        void 발급하면_RFRSH_TKN_행이_생기고_그_쿠키로_auth_me가_200이다() throws Exception {
            AuthMember member = normalMember();
            member.setAcntEmlEnc(fieldCryptoService.encrypt("dev@nyangmeong.com"));
            when(authMemberMapper.selectForAuth(3L)).thenReturn(member);
            when(authMemberMapper.selectForSession(3L, SID)).thenReturn(member);
            when(authMemberMapper.selectForMe(3L)).thenReturn(member);
            givenInsertedRow();

            MvcResult issued = mockMvc.perform(issue(3L)).andExpect(status().isOk()).andReturn();

            verify(refreshTokenMapper).insert(any(RefreshToken.class), anyLong());
            verify(refreshTokenMapper).updateHash(eq(SID), matches("[0-9a-f]{64}"));
            Cookie access = issued.getResponse().getCookie(TokenCookies.ACCESS_TOKEN);
            mockMvc.perform(get("/api/auth/me").cookie(access))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.mbrSn").value(3));
        }

        @Test
        void 없는_회원이면_404_MEMBER_NOT_FOUND다() throws Exception {
            when(authMemberMapper.selectForAuth(99L)).thenReturn(null);

            mockMvc.perform(issue(99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
        }

        @Test
        void 회원_번호가_없으면_400_INVALID_INPUT_VALUE다() throws Exception {
            mockMvc.perform(post("/api/auth/dev-tokens")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                    .andExpect(jsonPath("$.data[0].field").value("mbrSn"));
        }
    }

    @Nested
    @WebMvcTest(controllers = DevTokenController.class)
    @ActiveProfiles("prod")
    class prod_프로필 extends Base {

        @Test
        void 빈이_없어_404_NOT_FOUND다() throws Exception {
            mockMvc.perform(issue(3L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }
}
