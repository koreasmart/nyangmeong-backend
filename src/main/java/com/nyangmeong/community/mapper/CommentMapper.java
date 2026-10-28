package com.nyangmeong.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.dto.CommentSummary;

/**
 * [댓글 Mapper]
 */
@Mapper
public interface CommentMapper {

    /**
     * 댓글·대댓글 목록 조회 (삭제된 댓글 포함)
     */
    List<CommentSummary> selectList(@Param("pstSn") Long pstSn, @Param("mbrSn") Long mbrSn);

    /**
     * 삭제되지 않은 댓글 단건 조회
     */
    Comment selectComment(Long cmntSn);

    /**
     * 댓글 등록
     */
    int insertComment(Comment comment);

    /**
     * 댓글 삭제 처리
     */
    int updateDeleted(Long cmntSn);
}
