package com.nyangmeong.community.mapper;

import java.time.LocalDate;

import org.apache.ibatis.annotations.Mapper;

import com.nyangmeong.community.domain.CommunityAiCommentary;

/**
 * [커뮤니티 AI 코멘트 Mapper]
 */
@Mapper
public interface CommunityAiCommentaryMapper {

    /**
     * 일자별 사용 중인 코멘트 조회
     */
    CommunityAiCommentary selectByYmd(LocalDate cmtryCrtrYmd);
}
