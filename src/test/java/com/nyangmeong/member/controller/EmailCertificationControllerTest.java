package com.nyangmeong.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.member.dto.EmailCertificationCreateRequest;
import com.nyangmeong.member.dto.EmailCertificationResponse;
import com.nyangmeong.member.dto.EmailCertificationResult;
import com.nyangmeong.member.dto.EmailCertificationVerifyRequest;
import com.nyangmeong.member.service.EmailCertificationService;

/**
 * [검사 - 이메일 인증 API]
 * - 로그인 쿠키 없이 열려 있다 / 재전송 제한은 4xx 에 재시도 시각 / 입력 형식 오류는 서비스까지 가지 않는다
 */
@WebMvcTest(controllers = EmailCertificationController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class EmailCertificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EmailCertificationService emailCertificationService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private MockHttpServletRequestBuilder send(String body) {
        return post("/api/email-certifications")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder verifyRequest(String body) {
        return post("/api/email-certifications/3/verify")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    @Test
    void 발송은_로그인_없이_200과_만료시각을_준다() throws Exception {
        LocalDateTime expiry = LocalDateTime.of(2026, 10, 27, 12, 30, 0);
        when(emailCertificationService.send(any(EmailCertificationCreateRequest.class))).thenReturn(
                EmailCertificationResult.success(new EmailCertificationResponse(3L, "USRC0010", expiry, null, null, null)));

        mockMvc.perform(send("{\"emlCertPrpsCd\":\"USRC0010\",\"certTrgtEml\":\"newbie@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emlCertSn").value(3))
                .andExpect(jsonPath("$.data.certExpryDt").value("2026-10-27T12:30:00"))
                .andExpect(jsonPath("$.data.resendAvailableDt").doesNotExist());
    }

    @Test
    void 재전송_제한은_400이고_data에_재시도_가능_시각이_실린다() throws Exception {
        LocalDateTime available = LocalDateTime.of(2026, 10, 27, 12, 1, 0);
        when(emailCertificationService.send(any(EmailCertificationCreateRequest.class))).thenReturn(
                EmailCertificationResult.fail(ErrorCode.EMAIL_CERT_RESEND_NOT_YET,
                        EmailCertificationResponse.resendLimited(available)));

        mockMvc.perform(send("{\"emlCertPrpsCd\":\"USRC0010\",\"certTrgtEml\":\"newbie@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_CERT_RESEND_NOT_YET"))
                .andExpect(jsonPath("$.data.resendAvailableDt").value("2026-10-27T12:01:00"));
    }

    @Test
    void 가입된_이메일은_409이다() throws Exception {
        when(emailCertificationService.send(any(EmailCertificationCreateRequest.class)))
                .thenThrow(new CustomException(ErrorCode.EMAIL_ALREADY_REGISTERED));

        mockMvc.perform(send("{\"emlCertPrpsCd\":\"USRC0010\",\"certTrgtEml\":\"newbie@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다. 기존에 가입한 방법으로 로그인해 주세요."));
    }

    @Test
    void 이메일_형식이_틀리면_400이고_서비스를_부르지_않는다() throws Exception {
        mockMvc.perform(send("{\"emlCertPrpsCd\":\"USRC0010\",\"certTrgtEml\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("certTrgtEml"))
                .andExpect(jsonPath("$.data[0].message").value("이메일 주소 형식을 확인해주세요."));

        verify(emailCertificationService, never()).send(any());
    }

    @Test
    void 요청_헤더가_없으면_403이다() throws Exception {
        mockMvc.perform(post("/api/email-certifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emlCertPrpsCd\":\"USRC0010\",\"certTrgtEml\":\"newbie@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 인증_확인은_로그인_없이_가입_진행_결과를_준다() throws Exception {
        when(emailCertificationService.verify(eq(3L), any(EmailCertificationVerifyRequest.class))).thenReturn(
                new EmailCertificationResponse(3L, "USRC0010", null, null, LocalDateTime.of(2026, 10, 27, 12, 2, 0),
                        EmailCertificationResponse.SIGNUP_READY));

        mockMvc.perform(verifyRequest("{\"certNo\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.certRslt").value("SIGNUP_READY"))
                .andExpect(jsonPath("$.data.certCmptnDt").value("2026-10-27T12:02:00"));
    }

    @Test
    void 인증번호_불일치는_400이다() throws Exception {
        when(emailCertificationService.verify(eq(3L), any(EmailCertificationVerifyRequest.class)))
                .thenThrow(new CustomException(ErrorCode.EMAIL_CERT_MISMATCH));

        mockMvc.perform(verifyRequest("{\"certNo\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_CERT_MISMATCH"))
                .andExpect(jsonPath("$.message").value("인증번호가 일치하지 않습니다."));
    }

    @Test
    void 인증번호가_숫자_6자리가_아니면_400이고_서비스를_부르지_않는다() throws Exception {
        mockMvc.perform(verifyRequest("{\"certNo\":\"AB12CD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("certNo"));

        verify(emailCertificationService, never()).verify(any(), any());
    }
}
