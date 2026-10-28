package com.nyangmeong.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.ai.dto.ConversationMessageResponse;
import com.nyangmeong.ai.dto.ConversationResponse;
import com.nyangmeong.ai.dto.ConversationSummary;
import com.nyangmeong.ai.service.AiConversationService;
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
 * [검사 - /internal/ai/conversations API]
 * - 회원은 쿠키 인증 주체로만 정하고 요청 값으로 받지 않음
 */
@WebMvcTest(controllers = AiConversationController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AiConversationControllerTest {

    private static final String PATH = "/internal/ai/conversations";
    private static final String AJAX = "XMLHttpRequest";
    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 27, 10, 0);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AiConversationService aiConversationService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    void 쿠키가_없으면_401이다() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 대화를_만들면_201과_요약을_준다() throws Exception {
        when(aiConversationService.create(eq(1L), any()))
                .thenReturn(new ConversationSummary(7L, 3L, "제목", AT));

        mockMvc.perform(post(PATH).header("X-Requested-With", AJAX).contentType(MediaType.APPLICATION_JSON)
                .content("{\"petSn\":3,\"cnvrsTtl\":\"제목\",\"cnvrsCn\":\"첫 질문\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.aiCnvrsRoomSn").value(7))
                .andExpect(jsonPath("$.data.cnvrsTtl").value("제목"));
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 제목이_200자를_넘으면_400이다() throws Exception {
        String longTitle = "가".repeat(201);

        mockMvc.perform(post(PATH).header("X-Requested-With", AJAX).contentType(MediaType.APPLICATION_JSON)
                .content("{\"cnvrsTtl\":\"" + longTitle + "\",\"cnvrsCn\":\"첫 질문\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 내용이_비었으면_400이다() throws Exception {
        mockMvc.perform(post(PATH).header("X-Requested-With", AJAX).contentType(MediaType.APPLICATION_JSON)
                .content("{\"cnvrsTtl\":\"제목\",\"cnvrsCn\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 목록은_인증된_회원_번호와_태그로_조회한다() throws Exception {
        when(aiConversationService.search(1L, 3L)).thenReturn(List.of(new ConversationSummary(7L, 3L, "제목", AT)));

        mockMvc.perform(get(PATH).param("petSn", "3")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].aiCnvrsRoomSn").value(7));
        verify(aiConversationService).search(1L, 3L);
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 요청에_회원_번호를_넣어도_인증된_회원으로만_조회한다() throws Exception {
        when(aiConversationService.search(1L, null)).thenReturn(List.of());

        mockMvc.perform(get(PATH).param("mbrSn", "99")).andExpect(status().isOk());
        verify(aiConversationService).search(1L, null);
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 상세는_메시지를_함께_준다() throws Exception {
        when(aiConversationService.getConversation(1L, 7L)).thenReturn(new ConversationResponse(7L, null, "제목", AT,
                List.of(new ConversationMessageResponse(10L, "AIMC0024", "첫 질문", 1, true, AT))));

        mockMvc.perform(get(PATH + "/7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[0].cnvrsCn").value("첫 질문"))
                .andExpect(jsonPath("$.data.messages[0].msgSeq").value(1));
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 남의_대화_상세는_404다() throws Exception {
        when(aiConversationService.getConversation(1L, 7L)).thenThrow(new CustomException(ErrorCode.NOT_FOUND));

        mockMvc.perform(get(PATH + "/7")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 메시지를_추가하면_201이다() throws Exception {
        when(aiConversationService.createMessage(eq(1L), eq(7L), any()))
                .thenReturn(new ConversationMessageResponse(11L, "AIMC0025", "답변", 2, true, AT));

        mockMvc.perform(post(PATH + "/7/messages").header("X-Requested-With", AJAX)
                .contentType(MediaType.APPLICATION_JSON).content("{\"roleCd\":\"AIMC0025\",\"cnvrsCn\":\"답변\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.msgSeq").value(2));
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 삭제하면_200이다() throws Exception {
        mockMvc.perform(delete(PATH + "/7").header("X-Requested-With", AJAX)).andExpect(status().isOk());

        verify(aiConversationService).delete(1L, 7L);
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 남의_대화_삭제는_404다() throws Exception {
        org.mockito.Mockito.doThrow(new CustomException(ErrorCode.NOT_FOUND)).when(aiConversationService).delete(1L,
                7L);

        mockMvc.perform(delete(PATH + "/7").header("X-Requested-With", AJAX)).andExpect(status().isNotFound());
    }
}
