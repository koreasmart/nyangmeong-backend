package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.nyangmeong.community.domain.CommunityAiCommentary;
import com.nyangmeong.community.dto.CommunityAiCommentaryResponse;
import com.nyangmeong.community.mapper.CommunityAiCommentaryMapper;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;

/**
 * [검사 - 오늘의 이야기]
 */
class CommunityAiCommentaryServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 26);

    private final CommunityAiCommentaryMapper commentaryMapper = mock(CommunityAiCommentaryMapper.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private final CommunityAiCommentaryService service = new CommunityAiCommentaryService(commentaryMapper,
            systemSettingService);

    @Test
    void 해당_일자_코멘트가_있으면_그_내용을_돌려준다() {
        CommunityAiCommentary commentary = new CommunityAiCommentary();
        commentary.setCmntyAiCmtrySn(3L);
        commentary.setCmtryCn("오늘은 산책하기 좋은 날");
        when(commentaryMapper.selectByYmd(DAY)).thenReturn(commentary);

        CommunityAiCommentaryResponse res = service.getCommentary(DAY);

        assertThat(res.cmntyAiCmtrySn()).isEqualTo(3L);
        assertThat(res.cmtryCrtrYmd()).isEqualTo(DAY);
        assertThat(res.cmtryCn()).isEqualTo("오늘은 산책하기 좋은 날");
    }

    @Test
    void 코멘트가_없으면_번호_없이_시스템_설정의_기본_문구를_돌려준다() {
        when(systemSettingService.get()).thenReturn(new SystemSetting());

        CommunityAiCommentaryResponse res = service.getCommentary(DAY);

        assertThat(res.cmntyAiCmtrySn()).isNull();
        assertThat(res.cmtryCn()).isEqualTo(new SystemSetting().getAiCmtryBscCn());
    }

    @Test
    void 일자를_생략하면_오늘로_조회한다() {
        when(systemSettingService.get()).thenReturn(new SystemSetting());

        CommunityAiCommentaryResponse res = service.getCommentary(null);

        assertThat(res.cmtryCrtrYmd()).isEqualTo(LocalDate.now());
    }
}
