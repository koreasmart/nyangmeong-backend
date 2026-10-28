package com.nyangmeong.global.code;

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
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;

/**
 * [검사 - 공통코드 하위 목록 API]
 */
@WebMvcTest(controllers = CommonCodeController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class CommonCodeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private CommonCodeSummary code(Long sn, String comCd, String comCdNm, int sortSeq) {
        CommonCodeSummary code = new CommonCodeSummary();
        code.setComCdSn(sn);
        code.setComCd(comCd);
        code.setComCdNm(comCdNm);
        code.setSortSeq(sortSeq);
        return code;
    }

    @Test
    @WithMockLoginMember
    void 부위_탭_코드를_정렬_순서대로_돌려준다() throws Exception {
        when(commonCodeService.search("MYPG01"))
                .thenReturn(List.of(code(1L, "MYPC0001", "머리", 10), code(2L, "MYPC0002", "얼굴", 20)));

        mockMvc.perform(get("/api/common-codes").param("prntComCd", "MYPG01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].comCd").value("MYPC0001"))
                .andExpect(jsonPath("$.data[0].comCdNm").value("머리"))
                .andExpect(jsonPath("$.data[1].sortSeq").value(20));
    }

    @Test
    @WithMockLoginMember
    void 그룹_코드가_없으면_400_INVALID_INPUT_VALUE() throws Exception {
        mockMvc.perform(get("/api/common-codes"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("prntComCd"));
    }

    @Test
    @WithMockLoginMember
    void 없는_그룹은_404_COMMON_CODE_NOT_FOUND() throws Exception {
        when(commonCodeService.search("XXXX01")).thenThrow(new CustomException(ErrorCode.COMMON_CODE_NOT_FOUND));

        mockMvc.perform(get("/api/common-codes").param("prntComCd", "XXXX01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_CODE_NOT_FOUND"));
    }
}
