package com.nyangmeong.pet.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.pet.dto.DailyMissionSummary;
import com.nyangmeong.pet.service.CareMissionService;

/**
 * [검사 - 그날의 케어미션 API]
 */
@WebMvcTest(controllers = CareMissionController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class CareMissionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CareMissionService careMissionService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember
    void 그날의_미션을_배점_진행_상태와_함께_돌려준다() throws Exception {
        DailyMissionSummary row = new DailyMissionSummary();
        row.setDayMsnSn(77L);
        row.setCareMsnSn(2L);
        row.setMsnNm("양치하기");
        row.setScorPnt(5);
        row.setPrgrsNmtm(1);
        row.setAchvCrtrNmtm(1);
        row.setGivePnt(5);
        row.setCmptnYn(true);
        when(careMissionService.search(1L, 10L, LocalDate.of(2026, 10, 27))).thenReturn(List.of(row));

        mockMvc.perform(get("/api/pets/10/daily-missions").param("msnCrtrYmd", "2026-10-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].msnNm").value("양치하기"))
                .andExpect(jsonPath("$.data[0].scorPnt").value(5))
                .andExpect(jsonPath("$.data[0].givePnt").value(5))
                .andExpect(jsonPath("$.data[0].cmptnYn").value(true));
    }

    @Test
    @WithMockLoginMember
    void 공동양육자는_PET_PRIMARY_ONLY_403() throws Exception {
        when(careMissionService.search(1L, 10L, null)).thenThrow(new CustomException(ErrorCode.PET_PRIMARY_ONLY));

        mockMvc.perform(get("/api/pets/10/daily-missions"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.httpCode").value(403));
    }
}
