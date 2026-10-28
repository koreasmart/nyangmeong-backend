package com.nyangmeong.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.notification.NotificationTab;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.notification.domain.NotificationLeadTime;
import com.nyangmeong.notification.dto.NotificationSettingResponse;
import com.nyangmeong.notification.dto.NotificationSettingUpdateRequest;
import com.nyangmeong.notification.dto.NotificationSummary;
import com.nyangmeong.notification.dto.UnreadCountResponse;
import com.nyangmeong.notification.service.NotificationService;

/**
 * [검사 - 알림 드롭다운 API]
 */
@WebMvcTest(controllers = {NotificationController.class, NotificationSettingController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class NotificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NotificationService notificationService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private NotificationSummary row() {
        NotificationSummary row = new NotificationSummary();
        row.setInappNotiSn(10L);
        row.setNotiTypeCd(NotificationType.CARE_MATCH.getCode());
        row.setTab(NotificationTab.COMMUNITY.name());
        row.setNotiTtl("이서연님이 매칭을 요청했어요");
        row.setMvmnPath("/chat/rooms/3");
        row.setRegDt(LocalDateTime.of(2026, 10, 21, 9, 30));
        return row;
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember
    void 미읽음_개수를_돌려준다() throws Exception {
        when(notificationService.getUnreadCount(1L)).thenReturn(new UnreadCountResponse(3));

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.count").value(3));
    }

    @Test
    @WithMockLoginMember
    void 탭별_목록에_유형_탭_이동경로가_있다() throws Exception {
        when(notificationService.search(1L, NotificationTab.COMMUNITY)).thenReturn(List.of(row()));

        mockMvc.perform(get("/api/notifications").param("tab", "COMMUNITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].inappNotiSn").value(10))
                .andExpect(jsonPath("$.data[0].notiTypeCd").value("AIMC0009"))
                .andExpect(jsonPath("$.data[0].tab").value("COMMUNITY"))
                .andExpect(jsonPath("$.data[0].mvmnPath").value("/chat/rooms/3"))
                .andExpect(jsonPath("$.data[0].regDt").value("2026-10-21T09:30:00"));
    }

    @Test
    @WithMockLoginMember
    void 탭을_생략하면_조건_없이_조회한다() throws Exception {
        when(notificationService.search(eq(1L), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
        verify(notificationService).search(eq(1L), isNull());
    }

    @Test
    @WithMockLoginMember
    void 잘못된_탭은_400_INVALID_INPUT_VALUE() throws Exception {
        mockMvc.perform(get("/api/notifications").param("tab", "CHAT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("tab"))
                .andExpect(jsonPath("$.data[0].message").value("형식이 올바르지 않습니다."));
    }

    @Test
    @WithMockLoginMember
    void 읽음_처리는_200() throws Exception {
        mockMvc.perform(post("/api/notifications/10/read").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
        verify(notificationService).markRead(1L, 10L);
    }

    @Test
    @WithMockLoginMember
    void 남의_알림_읽음_처리는_404_NOTIFICATION_NOT_FOUND() throws Exception {
        doThrow(new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND)).when(notificationService).markRead(1L, 99L);

        mockMvc.perform(post("/api/notifications/99/read").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }

    private NotificationSettingResponse setting() {
        return new NotificationSettingResponse("jin****@gmail.com", true, true, true,
                List.of(NotificationLeadTime.D1, NotificationLeadTime.D7), true, true, true, false, false);
    }

    private static final String SETTING_BODY = """
            {"emlNotiRcptnYn":true,"inappNotiRcptnYn":true,"careSchdlNotiRcptnYn":true,
             "leadTimes":["D1","D7"],"mtchDlngNotiRcptnYn":true,"cmntNotiRcptnYn":true,
             "lostExpryNotiRcptnYn":true,"expndLimNotiRcptnYn":false}
            """;

    @Test
    @WithMockLoginMember
    void 알림_설정을_true_false로_돌려준다() throws Exception {
        when(notificationService.getSetting(1L)).thenReturn(setting());

        mockMvc.perform(get("/api/members/me/notification-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maskedAcntEml").value("jin****@gmail.com"))
                .andExpect(jsonPath("$.data.emlNotiRcptnYn").value(true))
                .andExpect(jsonPath("$.data.leadTimes[0]").value("D1"))
                .andExpect(jsonPath("$.data.leadTimes[1]").value("D7"))
                .andExpect(jsonPath("$.data.expndLimNotiRcptnYn").value(false))
                .andExpect(jsonPath("$.data.expndLimSetYn").value(false));
    }

    @Test
    @WithMockLoginMember
    void 알림_설정_저장은_200과_저장된_설정() throws Exception {
        when(notificationService.updateSetting(eq(1L), any(NotificationSettingUpdateRequest.class)))
                .thenReturn(setting());

        mockMvc.perform(put("/api/members/me/notification-settings").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SETTING_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.leadTimes[1]").value("D7"));
        verify(notificationService).updateSetting(1L, new NotificationSettingUpdateRequest(true, true, true,
                List.of(NotificationLeadTime.D1, NotificationLeadTime.D7), true, true, true, false));
    }

    @Test
    @WithMockLoginMember
    void 설정_필드가_빠지면_400_INVALID_INPUT_VALUE() throws Exception {
        mockMvc.perform(put("/api/members/me/notification-settings").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emlNotiRcptnYn\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }
}
