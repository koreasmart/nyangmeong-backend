package com.nyangmeong.auth.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.auth.service.AuthService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.AuthMember;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.global.security.session.LoginSessionManager;

/**
 * [검사 - GET /api/auth/me, POST /api/auth/refresh·logout]
 */
@WebMvcTest(controllers = AuthController.class)
@Import({AuthService.class, FieldCryptoService.class, CryptoProperties.class, SecurityConfig.class,
        JwtTokenProvider.class, JwtProperties.class, TokenCookies.class, ErrorResponseWriter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, SensitiveDataMasker.class,
        GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FieldCryptoService fieldCryptoService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @MockitoBean
    LoginSessionManager loginSessionManager;

    private AuthMember member(String email, Long atchFileSn) {
        AuthMember member = new AuthMember();
        member.setMbrSn(1L);
        member.setMbrSttsCd(MemberStatus.NORMAL.getCode());
        member.setMbrAuthrtCd(MemberAuthority.USER.getCode());
        member.setMbrNcnm("초코보호자");
        member.setAtchFileSn(atchFileSn);
        member.setAcntEmlEnc(fieldCryptoService.encrypt(email));
        return member;
    }

    @Test
    @WithMockLoginMember
    void 닉네임_사진_번호_마스킹_이메일을_돌려준다() throws Exception {
        when(authMemberMapper.selectForMe(1L)).thenReturn(member("jinseong@gmail.com", 55L));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.mbrSn").value(1))
                .andExpect(jsonPath("$.data.mbrAuthrtCd").value("USRC0004"))
                .andExpect(jsonPath("$.data.mbrNcnm").value("초코보호자"))
                .andExpect(jsonPath("$.data.atchFileSn").value(55))
                .andExpect(jsonPath("$.data.maskedAcntEml").value("jin****@gmail.com"));
    }

    @Test
    @WithMockLoginMember
    void 사진이_없으면_atchFileSn이_빠지고_짧은_이메일은_첫_글자만_남는다() throws Exception {
        when(authMemberMapper.selectForMe(1L)).thenReturn(member("ab@naver.com", null));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.atchFileSn").doesNotExist())
                .andExpect(jsonPath("$.data.maskedAcntEml").value("a****@naver.com"));
    }

    @Test
    @WithMockLoginMember
    void 회원_행이_없으면_404_MEMBER_NOT_FOUND다() throws Exception {
        when(authMemberMapper.selectForMe(1L)).thenReturn(null);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void 로그인하지_않으면_401이다() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 재발급은_로그인_없이_세션_회전을_부르고_data가_없다() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(loginSessionManager).rotate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 재발급에_실패하면_401_UNAUTHORIZED다() throws Exception {
        doThrow(new CustomException(ErrorCode.UNAUTHORIZED)).when(loginSessionManager)
                .rotate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/api/auth/refresh").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 로그아웃은_로그인_없이도_세션_폐기를_부른다() throws Exception {
        mockMvc.perform(post("/api/auth/logout").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(loginSessionManager).revoke(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
