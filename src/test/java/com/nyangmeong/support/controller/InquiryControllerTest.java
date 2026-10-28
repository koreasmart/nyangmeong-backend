package com.nyangmeong.support.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.support.dto.InquiryImageSummary;
import com.nyangmeong.support.dto.InquiryResponse;
import com.nyangmeong.support.dto.InquirySearchCond;
import com.nyangmeong.support.dto.InquirySummary;
import com.nyangmeong.support.service.InquiryService;

/**
 * [검사 - 1:1 문의 API]
 */
@WebMvcTest(controllers = InquiryController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class InquiryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    InquiryService inquiryService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private InquiryResponse response() {
        return new InquiryResponse(7L, "CSCC0008", "커뮤니티", "CSCC0012", "접수", "제목", "내용",
                List.of(new InquiryImageSummary(101L, 0)), LocalDateTime.of(2026, 10, 26, 9, 0), null, null);
    }

    @Test
    @WithMockLoginMember
    void 목록은_로그인_회원_번호와_필터로_조회한다() throws Exception {
        InquirySummary row = new InquirySummary();
        row.setOtoInqrySn(7L);
        row.setInqryTtl("제목");
        when(inquiryService.search(eq(1L), any(InquirySearchCond.class), any()))
                .thenReturn(new PageResponse<>(List.of(row), 1, 10, 1, 1, false));

        mockMvc.perform(get("/api/inquiries").param("inqryPrcsSttsCd", "CSCC0012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].otoInqrySn").value(7))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockLoginMember
    void 작성은_201과_문의를_돌려준다() throws Exception {
        when(inquiryService.create(eq(1L), any())).thenReturn(response());

        mockMvc.perform(multipart("/api/inquiries")
                .file(new MockMultipartFile("images", "a.png", "image/png", new byte[] {1}))
                .param("inqryTypeCd", "CSCC0008").param("inqryTtl", "제목").param("inqryCn", "내용")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201))
                .andExpect(jsonPath("$.data.otoInqrySn").value(7))
                .andExpect(jsonPath("$.data.inqryPrcsSttsCdNm").value("접수"))
                .andExpect(jsonPath("$.data.imgs[0].atchFileSn").value(101));
    }

    @Test
    @WithMockLoginMember
    void 제목이_50자를_넘거나_비어_있으면_400() throws Exception {
        mockMvc.perform(multipart("/api/inquiries")
                .param("inqryTypeCd", "CSCC0008").param("inqryTtl", "가".repeat(51)).param("inqryCn", "내용")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("inqryTtl"));
        mockMvc.perform(multipart("/api/inquiries")
                .param("inqryTypeCd", "CSCC0008").param("inqryTtl", " ").param("inqryCn", "내용")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("inqryTtl"));
    }

    @Test
    @WithMockLoginMember
    void 내용이_2000자를_넘으면_400() throws Exception {
        mockMvc.perform(multipart("/api/inquiries")
                .param("inqryTypeCd", "CSCC0008").param("inqryTtl", "제목").param("inqryCn", "가".repeat(2001))
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("inqryCn"));
    }

    @Test
    @WithMockLoginMember
    void 남의_문의_상세는_403() throws Exception {
        when(inquiryService.getInquiry(1L, 7L)).thenThrow(new CustomException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/inquiries/7"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockLoginMember
    void 수정은_multipart_PATCH로_받아_200을_돌려준다() throws Exception {
        when(inquiryService.update(eq(1L), eq(7L), any())).thenReturn(response());

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/inquiries/7")
                .param("inqryTtl", "새 제목").param("keepAtchFileSns", "101")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.otoInqrySn").value(7));
        verify(inquiryService).update(eq(1L), eq(7L), any());
    }

    @Test
    @WithMockLoginMember
    void 수정_제목이_공백이면_400() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/inquiries/7")
                .param("inqryTtl", "   ")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("inqryTtl"));
    }
}
