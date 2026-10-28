package com.nyangmeong.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.PostImage;
import com.nyangmeong.community.domain.PostStatusHistory;
import com.nyangmeong.community.dto.PostSummary;

/**
 * [게시글 Mapper]
 */
@Mapper
public interface PostMapper {

    /**
     * 게시글 목록 조회 (게시판·키워드 없으면 전체)
     */
    List<PostSummary> selectList(@Param("bbsCd") String bbsCd, @Param("keyword") String keyword,
            @Param("popularSort") boolean popularSort, @Param("pstSttsCd") String pstSttsCd);

    /**
     * 오늘의 건강팁 조회
     */
    List<PostSummary> selectHealthTips(@Param("bbsCd") String bbsCd, @Param("pstSttsCd") String pstSttsCd);

    /**
     * 인기 포토 조회 (days 없으면 전체 기간)
     */
    Post selectPopularPhoto(@Param("bbsCd") String bbsCd, @Param("pstSttsCd") String pstSttsCd,
            @Param("days") Integer days);

    /**
     * 정상 상태 게시글 단건 조회
     */
    Post selectPost(@Param("pstSn") Long pstSn, @Param("pstSttsCd") String pstSttsCd);

    /**
     * 작성자 닉네임 조회
     */
    String selectMbrNcnm(Long mbrSn);

    /**
     * 삭제 댓글을 뺀 댓글 수 조회
     */
    int countComments(Long pstSn);

    /**
     * 좋아요 여부 조회
     */
    boolean existsLike(@Param("pstSn") Long pstSn, @Param("mbrSn") Long mbrSn);

    /**
     * 좋아요 수 조회
     */
    int selectRcmdtnCnt(Long pstSn);

    /**
     * 사용 중인 이미지 조회
     */
    List<PostImage> selectImages(Long pstSn);

    /**
     * 게시글 등록
     */
    int insertPost(Post post);

    /**
     * 게시글 상태 이력 등록
     */
    int insertStatusHistory(PostStatusHistory history);

    /**
     * 이미지 등록
     */
    int insertImage(PostImage image);

    /**
     * 정상 상태 게시글 수정
     */
    int updatePost(@Param("pstSn") Long pstSn, @Param("bbsCd") String bbsCd, @Param("pstTtl") String pstTtl,
            @Param("pstCn") String pstCn, @Param("actorId") String actorId, @Param("pstSttsCd") String pstSttsCd);

    /**
     * 게시글 상태 변경 (이전 상태 조건)
     */
    int updateStatus(@Param("pstSn") Long pstSn, @Param("beforeCd") String beforeCd,
            @Param("afterCd") String afterCd, @Param("actorId") String actorId);

    /**
     * 이미지 삭제 처리
     */
    int updateImagesDeleted(@Param("pstSn") Long pstSn, @Param("atchFileSns") List<Long> atchFileSns,
            @Param("actorId") String actorId);

    /**
     * 조회 이력 등록 (같은 회원·같은 날이면 0)
     */
    int insertViewIgnore(@Param("pstSn") Long pstSn, @Param("mbrSn") Long mbrSn);

    /**
     * 조회수 증가
     */
    int updateInqCnt(Long pstSn);

    /**
     * 좋아요 켜기 (꺼져 있던 행만)
     */
    int updateLikeOn(@Param("pstSn") Long pstSn, @Param("mbrSn") Long mbrSn);

    /**
     * 좋아요 행 최초 등록 (이미 있으면 0)
     */
    int insertLikeIgnore(@Param("pstSn") Long pstSn, @Param("mbrSn") Long mbrSn);

    /**
     * 좋아요 끄기 (켜져 있던 행만)
     */
    int updateLikeOff(@Param("pstSn") Long pstSn, @Param("mbrSn") Long mbrSn);

    /**
     * 좋아요 수 증감
     */
    int updateRcmdtnCnt(@Param("pstSn") Long pstSn, @Param("delta") int delta);
}
