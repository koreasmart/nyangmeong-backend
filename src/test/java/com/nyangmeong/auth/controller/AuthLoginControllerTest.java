package com.nyangmeong.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nyangmeong.auth.dto.CaptchaCreateRequest;
import com.nyangmeong.auth.dto.CaptchaResponse;
import com.nyangmeong.auth.dto.LoginAttemptLimitResponse;
import com.nyangmeong.auth.dto.LoginRequest;
import com.nyangmeong.auth.dto.LoginResponse;
import com.nyangmeong.auth.dto.LoginResult;
import com.nyangmeong.auth.service.LoginService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.crypto.TokenHashUtil;
import com.nyangmeong.global.security.domain.RefreshToken;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.global.security.session.LoginSessionManager;
import com.nyangmeong.global.security.session.RefreshTokenMapper;

/**
 * [검사 - 이메일 로그인 API]
 * - 로그인 없이 열려 있다 / 실패는 4xx 이고 data 에 상태가 실린다 / 성공만 쿠키를 발급한다
 * - 쿠키 발급은 실제 LoginSessionManager 로 확인하고 RFRSH_TKN Mapper 만 가짜로 둔다
 */
@WebMvcTest(controllers = AuthLoginController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, LoginSessionManager.class, TokenHashUtil.class})
class AuthLoginControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LoginService loginService;

    @MockitoBean
    RefreshTokenMapper refreshTokenMapper;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private MockHttpServletRequestBuilder login(String body) {
        return post("/api/auth/login")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    @Test
    void 로그인_성공은_access_refresh_쿠키를_발급하고_닉네임_재안내_여부를_준다() throws Exception {
        doAnswer(invocation -> {
            ((RefreshToken) invocation.getArgument(0)).setRfrshTknSn(11L);
            return null;
        }).when(refreshTokenMapper).insert(any(RefreshToken.class), anyLong());
        when(loginService.login(any(LoginRequest.class)))
                .thenReturn(LoginResult.success(7L, new LoginResponse(true, "초코맘")));

        mockMvc.perform(login("{\"acntEml\":\"choco@example.com\",\"pswd\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.ncnmReguide").value(true))
                .andExpect(jsonPath("$.data.mbrNcnm").value("초코맘"))
                .andExpect(header().stringValues("Set-Cookie", Matchers.hasItems(
                        Matchers.allOf(Matchers.startsWith("access_token="), Matchers.containsString("HttpOnly"),
                                Matchers.containsString("Path=/")),
                        Matchers.allOf(Matchers.startsWith("refresh_token="), Matchers.containsString("HttpOnly"),
                                Matchers.containsString("Path=/api/auth")))));
    }

    @Test
    void 로그인_실패는_400이고_data에_실패_횟수와_보안코드_요구가_실린다() throws Exception {
        when(loginService.login(any(LoginRequest.class))).thenReturn(
                LoginResult.fail(ErrorCode.LOGIN_FAILED, new LoginAttemptLimitResponse(false, 2)));

        mockMvc.perform(login("{\"acntEml\":\"choco@example.com\",\"pswd\":\"wrong1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
                .andExpect(jsonPath("$.message").value("일치하는 회원정보를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.data.scrtyCdRqtYn").value(false))
                .andExpect(jsonPath("$.data.cntnuCertFailNmtm").value(2));

        verify(refreshTokenMapper, never()).insert(any(), anyLong());
    }

    @Test
    void 다섯_번째_실패는_429_LOGIN_CAPTCHA_REQUIRED다() throws Exception {
        when(loginService.login(any(LoginRequest.class))).thenReturn(
                LoginResult.fail(ErrorCode.LOGIN_CAPTCHA_REQUIRED, new LoginAttemptLimitResponse(true, 5)));

        mockMvc.perform(login("{\"acntEml\":\"choco@example.com\",\"pswd\":\"wrong1234\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_CAPTCHA_REQUIRED"))
                .andExpect(jsonPath("$.data.scrtyCdRqtYn").value(true));
    }

    @Test
    void 탈퇴_회원은_403_MEMBER_WITHDRAWN이다() throws Exception {
        when(loginService.login(any(LoginRequest.class))).thenReturn(
                LoginResult.fail(ErrorCode.MEMBER_WITHDRAWN, new LoginAttemptLimitResponse(false, 0)));

        mockMvc.perform(login("{\"acntEml\":\"choco@example.com\",\"pswd\":\"pass1234\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_WITHDRAWN"));
    }

    @Test
    void 비밀번호가_비어_있으면_400_INVALID_INPUT_VALUE다() throws Exception {
        mockMvc.perform(login("{\"acntEml\":\"choco@example.com\",\"pswd\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("pswd"));
    }

    @Test
    void 이메일_형식이_틀리면_400이고_서비스를_부르지_않는다() throws Exception {
        mockMvc.perform(login("{\"acntEml\":\"not-an-email\",\"pswd\":\"pass1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));

        verify(loginService, never()).login(any());
    }

    @Test
    void 보안코드가_6글자_영문_숫자가_아니면_400이다() throws Exception {
        mockMvc.perform(login("{\"acntEml\":\"choco@example.com\",\"pswd\":\"pass1234\",\"scrtyCd\":\"AB1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("scrtyCd"));
    }

    @Test
    void X_Requested_With_헤더가_없으면_403이다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"acntEml\":\"choco@example.com\",\"pswd\":\"pass1234\"}"))
                .andExpect(status().isForbidden());

        verify(loginService, never()).login(any());
    }

    @Test
    void 실패_횟수_조회는_로그인_없이_열려_있다() throws Exception {
        when(loginService.getAttemptLimit("choco@example.com")).thenReturn(new LoginAttemptLimitResponse(true, 5));

        mockMvc.perform(get("/api/auth/login-attempt-limits").param("email", "choco@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scrtyCdRqtYn").value(true))
                .andExpect(jsonPath("$.data.cntnuCertFailNmtm").value(5));
    }

    @Test
    void 실패_횟수_조회의_이메일_형식이_틀리면_400이다() throws Exception {
        mockMvc.perform(get("/api/auth/login-attempt-limits").param("email", "nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 보안코드_발급은_로그인_없이_이미지를_준다() throws Exception {
        when(loginService.issueCaptcha(any(CaptchaCreateRequest.class)))
                .thenReturn(new CaptchaResponse("data:image/png;base64,AAAA"));

        mockMvc.perform(post("/api/auth/captchas")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"acntEml\":\"choco@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.captchaImg").value("data:image/png;base64,AAAA"));
    }

    @Test
    void 보안코드가_필요_없는_아이디의_발급은_400_CAPTCHA_NOT_REQUIRED다() throws Exception {
        doThrow(new CustomException(ErrorCode.CAPTCHA_NOT_REQUIRED)).when(loginService)
                .issueCaptcha(any(CaptchaCreateRequest.class));

        mockMvc.perform(post("/api/auth/captchas")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"acntEml\":\"choco@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAPTCHA_NOT_REQUIRED"));
    }
}
