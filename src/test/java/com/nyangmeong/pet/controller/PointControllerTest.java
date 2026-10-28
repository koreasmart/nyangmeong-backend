package com.nyangmeong.pet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
import com.nyangmeong.pet.dto.PointLedgerSummary;
import com.nyangmeong.pet.dto.PointResponse;
import com.nyangmeong.pet.service.PointService;

/**
 * [검사 - 포인트 요약·내역 API]
 */
@WebMvcTest(controllers = {PointController.class, PointLedgerController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class PointControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PointService pointService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    void 쿠키가_없으면_401() throws Exception {
        mockMvc.perform(get("/api/members/me/points"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember
    void 요약은_보유_포인트와_이번_달_합계_오늘_미션을_돌려준다() throws Exception {
        when(pointService.getPoint(1L)).thenReturn(new PointResponse(8_450, 295, 4, 1_300, 2, 11, 24, 3));

        mockMvc.perform(get("/api/members/me/points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hldPnt").value(8_450))
                .andExpect(jsonPath("$.data.monthEarnPnt").value(295))
                .andExpect(jsonPath("$.data.monthUsePnt").value(1_300))
                .andExpect(jsonPath("$.data.todayCmptnMsnCnt").value(11))
                .andExpect(jsonPath("$.data.todayMsnCnt").value(24))
                .andExpect(jsonPath("$.data.todayMsnPetCnt").value(3));
    }

    @Test
    @WithMockLoginMember
    void 내역은_고른_달로_조회하고_날짜_내용_부호_있는_포인트를_돌려준다() throws Exception {
        PointLedgerSummary row = new PointLedgerSummary();
        row.setPntGnlgrSn(5L);
        row.setPntDlngTypeCd("MYPC0008");
        row.setPntDlngTypeCdNm("아이템 구매");
        row.setDlngPnt(-600);
        row.setMemoCn("밀짚모자");
        row.setRegDt(LocalDateTime.of(2026, 9, 12, 14, 0));
        when(pointService.searchLedgers(1L, YearMonth.of(2026, 9))).thenReturn(List.of(row));

        mockMvc.perform(get("/api/point-ledgers").param("yearMonth", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].pntDlngTypeCdNm").value("아이템 구매"))
                .andExpect(jsonPath("$.data[0].dlngPnt").value(-600))
                .andExpect(jsonPath("$.data[0].memoCn").value("밀짚모자"))
                .andExpect(jsonPath("$.data[0].regDt").value("2026-09-12T14:00:00"));
    }

    @Test
    @WithMockLoginMember
    void 형식이_틀린_yearMonth는_400() throws Exception {
        mockMvc.perform(get("/api/point-ledgers").param("yearMonth", "2026-13"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/point-ledgers").param("yearMonth", "202609"))
                .andExpect(status().isBadRequest());

        verify(pointService, never()).searchLedgers(anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void yearMonth가_없으면_null로_넘겨_이번_달을_조회한다() throws Exception {
        mockMvc.perform(get("/api/point-ledgers"))
                .andExpect(status().isOk());

        verify(pointService).searchLedgers(1L, null);
    }
}
