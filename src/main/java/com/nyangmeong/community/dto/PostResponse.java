package com.nyangmeong.community.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.nyangmeong.community.domain.Post;

/**
 * [게시글 응답]
 * - 상세·작성·수정·인기 포토 공통
 */
public record PostResponse(
        Long pstSn,
        String bbsCd,
        String pstTtl,
        String pstCn,
        Long mbrSn,
        String mbrNcnm,
        LocalDateTime regDt,
        Integer inqCnt,
        Integer rcmdtnCnt,
        Integer cmntCnt,
        List<PostImageSummary> imgs,
        Boolean liked,
        Boolean owner) {

    public static PostResponse from(Post post, String mbrNcnm, int cmntCnt, List<PostImageSummary> imgs,
            boolean liked, boolean owner) {
        return new PostResponse(post.getPstSn(), post.getBbsCd(), post.getPstTtl(), post.getPstCn(), post.getMbrSn(),
                mbrNcnm, post.getRegDt(), post.getInqCnt(), post.getRcmdtnCnt(), cmntCnt, imgs, liked, owner);
    }
}
