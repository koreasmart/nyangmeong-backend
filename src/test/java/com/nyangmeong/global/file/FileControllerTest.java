package com.nyangmeong.global.file;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/**
 * [검사 - 공개 파일 API]
 * - 쿠키 없이 열린다 / 공개가 아니거나 없는 파일은 404
 */
@WebMvcTest(controllers = FileController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class FileControllerTest {

    private static final String PATH = "/api/files/public/{atchFileSn}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FileStore fileStore;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    void 쿠키_없이_공개_이미지를_연다() throws Exception {
        when(fileStore.loadPublic(7L)).thenReturn(new StoredFile(new byte[] {1, 2, 3}, "image/png", "post.png"));

        mockMvc.perform(get(PATH, 7L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("public")))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    void 공개가_아니거나_없는_파일은_404이다() throws Exception {
        when(fileStore.loadPublic(8L)).thenThrow(new CustomException(ErrorCode.FILE_NOT_FOUND));

        mockMvc.perform(get(PATH, 8L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
    }
}
