package com.nyangmeong.expense.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.expense.service.ReceiptAnalyzer;
import com.nyangmeong.expense.service.ReceiptService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;

/**
 * [검사 - 영수증 원본 이미지 API]
 */
@WebMvcTest(controllers = ReceiptController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class ReceiptImageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ReceiptAnalyzer receiptAnalyzer;

    @MockitoBean
    ReceiptService receiptService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember
    void 원본_이미지를_파일_형식으로_돌려준다() throws Exception {
        when(receiptService.getImage(1L, 3L)).thenReturn(new StoredFile(new byte[] {1, 2}, "image/png", "r.png"));

        mockMvc.perform(get("/api/receipts/3/image").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[] {1, 2}));
    }

    @Test
    @WithMockLoginMember
    void 남의_영수증_이미지는_404다() throws Exception {
        when(receiptService.getImage(1L, 3L)).thenThrow(new CustomException(ErrorCode.RECEIPT_NOT_FOUND));

        mockMvc.perform(get("/api/receipts/3/image").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isNotFound());
    }
}
