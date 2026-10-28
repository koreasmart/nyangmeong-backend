package com.nyangmeong.community.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.community.domain.CommunityAiCommentary;
import com.nyangmeong.community.dto.CommunityAiCommentaryResponse;
import com.nyangmeong.community.mapper.CommunityAiCommentaryMapper;
import com.nyangmeong.global.setting.SystemSettingService;

import lombok.RequiredArgsConstructor;

/**
 * [오늘의 이야기 서비스]
 * - SCR-CMU-001 우측 레일, 해당 일자 코멘트가 없으면 SYS_STNG 기본 문구
 */
@Service
@RequiredArgsConstructor
public class CommunityAiCommentaryService {

    private final CommunityAiCommentaryMapper commentaryMapper;
    private final SystemSettingService systemSettingService;

    /**
     * 일자별 오늘의 이야기 조회
     */
    @Transactional(readOnly = true)
    public CommunityAiCommentaryResponse getCommentary(LocalDate cmtryCrtrYmd) {
        LocalDate ymd = cmtryCrtrYmd != null ? cmtryCrtrYmd : LocalDate.now();
        CommunityAiCommentary commentary = commentaryMapper.selectByYmd(ymd);
        if (commentary == null) {
            return new CommunityAiCommentaryResponse(null, ymd, systemSettingService.get().getAiCmtryBscCn());
        }
        return new CommunityAiCommentaryResponse(commentary.getCmntyAiCmtrySn(), ymd, commentary.getCmtryCn());
    }
}
