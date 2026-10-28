package com.nyangmeong.expense.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.expense.dto.ReceiptResponse;
import com.nyangmeong.expense.service.ReceiptAnalyzer;
import com.nyangmeong.expense.service.ReceiptService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
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
 * [검사 - 영수증 업로드·다시 분석·취소 API]
 */
@WebMvcTest(controllers = ReceiptController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class ReceiptControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ReceiptAnalyzer receiptAnalyzer;

    @MockitoBean
    ReceiptService receiptService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private final MockMultipartFile file = new MockMultipartFile("file", "r.png", "image/png", new byte[] {1});

    @Test
    @WithMockLoginMember
    void 업로드는_201과_분석_결과를_돌려준다() throws Exception {
        ReceiptResponse.Section tab = new ReceiptResponse.Section(null, 10L, 10_000L, null, 10_000L, "진찰료",
                List.of(new ReceiptResponse.Line("진찰료", 10_000L)), List.of());
        when(receiptAnalyzer.create(eq(1L), eq(10L), any())).thenReturn(new ReceiptResponse(3L, "AIMC0002", null,
                "냥멍동물병원", LocalDate.of(2026, 10, 21), 10_000L, null, List.of(tab)));

        mockMvc.perform(multipart("/api/receipts").file(file).param("petSn", "10")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201))
                .andExpect(jsonPath("$.data.recptSn").value(3))
                .andExpect(jsonPath("$.data.ocrPrcsSttsCd").value("AIMC0002"))
                .andExpect(jsonPath("$.data.ckupYmd").value("2026-10-21"))
                .andExpect(jsonPath("$.data.sections[0].petSn").value(10))
                .andExpect(jsonPath("$.data.sections[0].ckupCn").value("진찰료"));
    }

    @Test
    @WithMockLoginMember
    void 일시_오류도_201에_상태와_사유로_구분한다() throws Exception {
        when(receiptAnalyzer.create(eq(1L), eq(10L), any()))
                .thenReturn(ReceiptResponse.empty(3L, "AIMC0005", "TIMEOUT"));

        mockMvc.perform(multipart("/api/receipts").file(file).param("petSn", "10")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ocrPrcsSttsCd").value("AIMC0005"))
                .andExpect(jsonPath("$.data.failRsn").value("TIMEOUT"))
                .andExpect(jsonPath("$.data.sections").isEmpty());
    }

    @Test
    @WithMockLoginMember
    void 파일이나_반려동물이_없으면_400_INVALID_INPUT_VALUE() throws Exception {
        mockMvc.perform(multipart("/api/receipts").param("petSn", "10")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("file"));
        mockMvc.perform(multipart("/api/receipts").file(file)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("petSn"));
        verify(receiptAnalyzer, never()).create(any(), any(), any());
    }

    @Test
    @WithMockLoginMember
    void 형식이_다른_파일은_400_FILE_TYPE_NOT_ALLOWED() throws Exception {
        when(receiptAnalyzer.create(eq(1L), eq(10L), any()))
                .thenThrow(new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED));

        mockMvc.perform(multipart("/api/receipts").file(file).param("petSn", "10")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_TYPE_NOT_ALLOWED"));
    }

    @Test
    @WithMockLoginMember
    void 다시_분석은_200() throws Exception {
        when(receiptAnalyzer.analyze(1L, 3L, 10L)).thenReturn(ReceiptResponse.empty(3L, "AIMC0004", null));

        mockMvc.perform(post("/api/receipts/3/analyze").contentType(MediaType.APPLICATION_JSON)
                .content("{\"petSn\":10}")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ocrPrcsSttsCd").value("AIMC0004"));
    }

    @Test
    @WithMockLoginMember
    void 분석중에_다시_분석하면_409() throws Exception {
        when(receiptAnalyzer.analyze(1L, 3L, 10L)).thenThrow(new CustomException(ErrorCode.RECEIPT_ANALYZING));

        mockMvc.perform(post("/api/receipts/3/analyze").contentType(MediaType.APPLICATION_JSON)
                .content("{\"petSn\":10}")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RECEIPT_ANALYZING"));
    }

    @Test
    @WithMockLoginMember
    void 취소는_200에_data_없음() throws Exception {
        mockMvc.perform(post("/api/receipts/3/cancel").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
        verify(receiptService).cancel(1L, 3L);
    }

    @Test
    @WithMockLoginMember
    void 이미_취소한_영수증은_409() throws Exception {
        doThrow(new CustomException(ErrorCode.RECEIPT_ALREADY_CANCELED)).when(receiptService).cancel(1L, 3L);

        mockMvc.perform(post("/api/receipts/3/cancel").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RECEIPT_ALREADY_CANCELED"));
    }
}
