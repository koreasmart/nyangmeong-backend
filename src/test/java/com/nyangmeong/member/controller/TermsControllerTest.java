package com.nyangmeong.member.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
import com.nyangmeong.member.dto.TermsResponse;
import com.nyangmeong.member.dto.TermsSummary;
import com.nyangmeong.member.service.TermsService;

/**
 * [검사 - 약관 API]
 * - 로그인 쿠키 없이 목록·전문 조회 / 없는 약관은 404
 */
@WebMvcTest(controllers = TermsController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class TermsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TermsService termsService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    void 약관_목록은_로그인_없이_조회한다() throws Exception {
        TermsSummary service = new TermsSummary();
        service.setTrmsSn(1L);
        service.setTrmsTtl("서비스 이용약관");
        service.setEsntlAgreYn(true);
        TermsSummary marketing = new TermsSummary();
        marketing.setTrmsSn(4L);
        marketing.setEsntlAgreYn(false);
        when(termsService.search()).thenReturn(List.of(service, marketing));

        mockMvc.perform(get("/api/terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].trmsTtl").value("서비스 이용약관"))
                .andExpect(jsonPath("$.data[0].esntlAgreYn").value(true))
                .andExpect(jsonPath("$.data[1].esntlAgreYn").value(false));
    }

    @Test
    void 약관_전문은_로그인_없이_조회한다() throws Exception {
        when(termsService.getTerms(1L))
                .thenReturn(new TermsResponse(1L, "SERVICE", "v1.0", "서비스 이용약관", "전문 내용", true));

        mockMvc.perform(get("/api/terms/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trmsCn").value("전문 내용"));
    }

    @Test
    void 없는_약관은_404이다() throws Exception {
        when(termsService.getTerms(9L)).thenThrow(new CustomException(ErrorCode.TERMS_NOT_FOUND));

        mockMvc.perform(get("/api/terms/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TERMS_NOT_FOUND"));
    }
}
