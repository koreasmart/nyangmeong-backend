package com.nyangmeong.health.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
import com.nyangmeong.health.dto.CareScheduleResponse;
import com.nyangmeong.health.dto.CareScheduleSummary;
import com.nyangmeong.health.service.CareScheduleService;

/**
 * [검사 - 케어 캘린더 일정 API]
 */
@WebMvcTest(controllers = CareScheduleController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class CareScheduleControllerTest {

    private static final String XHR = "X-Requested-With";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CareScheduleService careScheduleService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private CareScheduleResponse response() {
        return new CareScheduleResponse(100L, 10L, "HLTC0057", LocalDateTime.of(2026, 11, 3, 10, 30), true,
                "HLTC0001", "튼튼동물병원", null, null, null, "HLTC0065");
    }

    @Test
    @WithMockLoginMember
    void 목록은_기간과_건수_조건을_받아_코드명과_검진기록_수를_돌려준다() throws Exception {
        CareScheduleSummary row = new CareScheduleSummary();
        row.setSchdlSn(100L);
        row.setSchdlKndCdNm("병원");
        row.setCkupTypeCdNm("정기검진");
        row.setCkupRcdCnt(0);
        when(careScheduleService.search(eq(1L), eq(10L), any())).thenReturn(List.of(row));

        mockMvc.perform(get("/api/pets/10/care-schedules")
                .param("fromYmd", "2026-11-01").param("toYmd", "2026-11-30").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].schdlKndCdNm").value("병원"))
                .andExpect(jsonPath("$.data[0].ckupTypeCdNm").value("정기검진"))
                .andExpect(jsonPath("$.data[0].ckupRcdCnt").value(0));
        verify(careScheduleService).search(eq(1L), eq(10L), argThat(cond -> cond.getSize() == 3
                && LocalDate.of(2026, 11, 1).equals(cond.getFromYmd())
                && LocalDate.of(2026, 11, 30).equals(cond.getToYmd())));
    }

    @Test
    @WithMockLoginMember
    void 등록은_201과_저장한_일정을_돌려준다() throws Exception {
        when(careScheduleService.create(eq(1L), eq(10L), any())).thenReturn(response());

        mockMvc.perform(post("/api/pets/10/care-schedules").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"schdlKndCd":"HLTC0057","schdlDt":"2026-11-03T10:30:00","hrInptYn":true,
                         "ckupTypeCd":"HLTC0001","hsptlNm":"튼튼동물병원"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201))
                .andExpect(jsonPath("$.data.schdlSn").value(100))
                .andExpect(jsonPath("$.data.hrInptYn").value(true));
    }

    @Test
    @WithMockLoginMember
    void 일정_종류나_날짜가_없으면_400() throws Exception {
        mockMvc.perform(post("/api/pets/10/care-schedules").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hrInptYn\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        verify(careScheduleService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 메모가_500자를_넘으면_400() throws Exception {
        String memo = "가".repeat(501);
        mockMvc.perform(patch("/api/pets/10/care-schedules/100").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memoCn\":\"" + memo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @WithMockLoginMember
    void 수정은_200과_바뀐_일정을_돌려준다() throws Exception {
        when(careScheduleService.update(eq(1L), eq(10L), eq(100L), any())).thenReturn(response());

        mockMvc.perform(patch("/api/pets/10/care-schedules/100").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"petSn\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schdlSn").value(100));
    }

    @Test
    @WithMockLoginMember
    void 삭제는_200() throws Exception {
        mockMvc.perform(delete("/api/pets/10/care-schedules/100").header(XHR, "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
        verify(careScheduleService).delete(1L, 10L, 100L);
    }

    @Test
    @WithMockLoginMember
    void 없는_일정은_404_CARE_SCHEDULE_NOT_FOUND() throws Exception {
        doThrow(new CustomException(ErrorCode.CARE_SCHEDULE_NOT_FOUND)).when(careScheduleService)
                .delete(1L, 10L, 999L);

        mockMvc.perform(delete("/api/pets/10/care-schedules/999").header(XHR, "XMLHttpRequest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CARE_SCHEDULE_NOT_FOUND"));
    }
}
