package com.nyangmeong.expense.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.expense.dto.ReceiptSaveCheckResponse;
import com.nyangmeong.expense.dto.ReceiptSaveResponse;
import com.nyangmeong.expense.service.ReceiptSaveService;
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
 * [검사 - 영수증 저장 전 확인·확인 후 저장 API]
 */
@WebMvcTest(controllers = ReceiptSaveController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class ReceiptSaveControllerTest {

    private static final String SAVE_BODY = """
            {"idmptDmndId":"req-1","hsptlNm":"냥멍동물병원","ckupYmd":"2026-10-21","expndPlcusNm":"행복동물병원",
             "pets":[{"petSn":10,"ckupTypeCd":"HLTC0002","ckupCn":"진찰료","amt":30000,"expenseYn":true,
                      "drugs":[{"mdcnNm":"항생제","mdcnBgngYmd":"2026-10-21","mdcnEndYmd":"2026-10-26",
                                "dayMdcnNmtm":2,"prscSttsCd":"HLTC0010"}]}]}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ReceiptSaveService receiptSaveService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private ReceiptSaveResponse response() {
        return new ReceiptSaveResponse(3L, List.of(new ReceiptSaveResponse.CheckupRecord(10L, 501L)));
    }

    @Test
    @WithMockLoginMember
    void 저장은_201과_반려동물별_검진기록_번호를_돌려준다() throws Exception {
        when(receiptSaveService.save(eq(1L), eq(3L), any()))
                .thenReturn(new ReceiptSaveService.Saved(response(), true));

        mockMvc.perform(post("/api/receipts/3/save").contentType(MediaType.APPLICATION_JSON).content(SAVE_BODY)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recptSn").value(3))
                .andExpect(jsonPath("$.data.ckupRcds[0].petSn").value(10))
                .andExpect(jsonPath("$.data.ckupRcds[0].ckupRcdSn").value(501));
    }

    @Test
    @WithMockLoginMember
    void 같은_요청_다시_보내면_200으로_같은_결과다() throws Exception {
        when(receiptSaveService.save(eq(1L), eq(3L), any()))
                .thenReturn(new ReceiptSaveService.Saved(response(), false));

        mockMvc.perform(post("/api/receipts/3/save").contentType(MediaType.APPLICATION_JSON).content(SAVE_BODY)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ckupRcds[0].ckupRcdSn").value(501));
    }

    @Test
    @WithMockLoginMember
    void 저장할_반려동물이_없으면_400이다() throws Exception {
        String body = """
                {"idmptDmndId":"req-1","hsptlNm":"냥멍동물병원","ckupYmd":"2026-10-21","pets":[]}
                """;

        mockMvc.perform(post("/api/receipts/3/save").contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest());
        verify(receiptSaveService, never()).save(any(), any(), any());
    }

    @Test
    @WithMockLoginMember
    void 병원명_50자_초과는_400이다() throws Exception {
        String body = SAVE_BODY.replace("냥멍동물병원", "가".repeat(51));

        mockMvc.perform(post("/api/receipts/3/save").contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockLoginMember
    void 사용처_80자_초과는_400이다() throws Exception {
        String body = SAVE_BODY.replace("행복동물병원", "가".repeat(81));

        mockMvc.perform(post("/api/receipts/3/save").contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest());
        verify(receiptSaveService, never()).save(any(), any(), any());
    }

    @Test
    @WithMockLoginMember
    void 약품명_40자_초과나_검진_종류_누락은_400이다() throws Exception {
        mockMvc.perform(post("/api/receipts/3/save").contentType(MediaType.APPLICATION_JSON)
                .content(SAVE_BODY.replace("항생제", "약".repeat(41)))
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/receipts/3/save").contentType(MediaType.APPLICATION_JSON)
                .content(SAVE_BODY.replace("\"ckupTypeCd\":\"HLTC0002\",", ""))
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest());
        verify(receiptSaveService, never()).save(any(), any(), any());
    }

    @Test
    @WithMockLoginMember
    void 검진_내용_500자_초과는_400이다() throws Exception {
        mockMvc.perform(post("/api/receipts/3/save").contentType(MediaType.APPLICATION_JSON)
                .content(SAVE_BODY.replace("진찰료", "가".repeat(501)))
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockLoginMember
    void 저장_충돌은_409로_나간다() throws Exception {
        when(receiptSaveService.save(eq(1L), eq(3L), any()))
                .thenThrow(new CustomException(ErrorCode.RECEIPT_NOT_SAVABLE));

        mockMvc.perform(post("/api/receipts/3/save").contentType(MediaType.APPLICATION_JSON).content(SAVE_BODY)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockLoginMember
    void 저장_전_확인은_중복_지출과_가까운_일정을_돌려준다() throws Exception {
        ReceiptSaveCheckResponse.DuplicateExpense duplicate = new ReceiptSaveCheckResponse.DuplicateExpense(10L, 7L,
                java.time.LocalDate.of(2026, 10, 21), 30_000L, null, "냥멍동물병원");
        when(receiptSaveService.check(eq(1L), eq(3L), any()))
                .thenReturn(new ReceiptSaveCheckResponse(List.of(duplicate), List.of()));
        String body = """
                {"ckupYmd":"2026-10-21","pets":[{"petSn":10,"amt":30000,"expenseYn":true}]}
                """;

        mockMvc.perform(post("/api/receipts/3/save-check").contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicateExpenses[0].expndSn").value(7))
                .andExpect(jsonPath("$.data.duplicateExpenses[0].expndPlcusNm").value("냥멍동물병원"))
                .andExpect(jsonPath("$.data.nearbySchedules").isEmpty());
    }

    @Test
    void 로그인하지_않으면_401이다() throws Exception {
        mockMvc.perform(post("/api/receipts/3/save").contentType(MediaType.APPLICATION_JSON).content(SAVE_BODY)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized());
    }
}
