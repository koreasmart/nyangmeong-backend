package com.nyangmeong.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.ai.service.AiConversationService;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.member.service.MemberService;

/**
 * [검사 - 탈퇴 회원 AI 대화 파기 배치]
 */
class AiConversationPurgeJobTest {

    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private final MemberService memberService = mock(MemberService.class);
    private final AiConversationService aiConversationService = mock(AiConversationService.class);
    private final AiConversationPurgeJob job = new AiConversationPurgeJob(systemSettingService, memberService,
            aiConversationService);

    @Test
    void 설정의_보관일수가_지난_회원의_대화를_파기한다() {
        SystemSetting setting = new SystemSetting();
        setting.setWhdwlMbrKpngDayCnt(45L);
        when(systemSettingService.get()).thenReturn(setting);
        when(memberService.searchExpiredWithdrawn(45)).thenReturn(List.of(5L, 6L));
        when(aiConversationService.deleteByMbrSns(List.of(5L, 6L))).thenReturn(3);

        assertThat(job.purge()).isEqualTo(3);

        verify(memberService).searchExpiredWithdrawn(45);
        verify(aiConversationService).deleteByMbrSns(List.of(5L, 6L));
    }
}
