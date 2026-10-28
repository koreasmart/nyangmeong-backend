package com.nyangmeong.community.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.community.domain.BoardType;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.PostImage;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.domain.PostStatusHistory;
import com.nyangmeong.community.dto.PostCreateRequest;
import com.nyangmeong.community.dto.PostImageSummary;
import com.nyangmeong.community.dto.PostLikeResponse;
import com.nyangmeong.community.dto.PostResponse;
import com.nyangmeong.community.dto.PostSearchCond;
import com.nyangmeong.community.dto.PostSummary;
import com.nyangmeong.community.dto.PostUpdateRequest;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.global.auth.OwnerAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.setting.SystemSettingService;

import lombok.RequiredArgsConstructor;

/**
 * [펫이야기 게시글 서비스]
 * - SCR-CMU-001 목록·상세·작성·수정·삭제·좋아요, 인기 포토·오늘의 건강팁
 * - 조회수 : 같은 회원은 하루 한 번만 반영 (PST_INQ_HSTRY)
 */
@Service
@RequiredArgsConstructor
public class PostService {

    static final int PAGE_SIZE = 10;
    static final String SORT_LATEST = "latest";
    static final String SORT_POPULAR = "popular";
    /** 인기 포토 대체 기간 (일), 마지막은 전체 기간 */
    static final List<Integer> POPULAR_PHOTO_DAYS = Arrays.asList(7, 30, null);

    private final PostMapper postMapper;
    private final FileStore fileStore;
    private final OwnerAuthorizer ownerAuthorizer;
    private final SystemSettingService systemSettingService;

    /**
     * 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<PostSummary> search(PostSearchCond cond, PageQuery pq) {
        String bbsCd = cond.getBbsCd() == null || cond.getBbsCd().isBlank() ? null : cond.getBbsCd();
        if (bbsCd != null && !BoardType.exists(bbsCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        boolean popularSort = isPopularSort(pq.sort());
        String keyword = cond.getKeyword() == null || cond.getKeyword().isBlank() ? null
                : escapeLike(cond.getKeyword().trim());

        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(PAGE_SIZE));
        List<PostSummary> rows = postMapper.selectList(bbsCd, keyword, popularSort, PostStatus.NORMAL.getCode());
        /** 인기순 정렬에서는 인기글 배지 미표시 */
        if (popularSort) {
            rows.forEach(row -> row.setPopular(false));
        }
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 오늘의 건강팁 조회
     */
    @Transactional(readOnly = true)
    public List<PostSummary> getHealthTips() {
        return postMapper.selectHealthTips(BoardType.PET_INFO.getCode(), PostStatus.NORMAL.getCode());
    }

    /**
     * 인기 포토 조회 (7일 → 30일 → 전체, 없으면 null)
     */
    @Transactional(readOnly = true)
    public PostResponse getPopularPhoto(Long mbrSn) {
        for (Integer days : POPULAR_PHOTO_DAYS) {
            Post post = postMapper.selectPopularPhoto(BoardType.PET_DAILY.getCode(), PostStatus.NORMAL.getCode(),
                    days);
            if (post != null) {
                return toResponse(mbrSn, post);
            }
        }
        return null;
    }

    /**
     * 게시글 상세 조회
     */
    @Transactional
    public PostResponse getPost(Long mbrSn, Long pstSn) {
        Post post = getNormalOrThrow(pstSn);
        /** 같은 회원·같은 날 이력이 이미 있으면 0건 */
        if (postMapper.insertViewIgnore(pstSn, mbrSn) > 0) {
            postMapper.updateInqCnt(pstSn);
            post.setInqCnt(post.getInqCnt() + 1);
        }
        return toResponse(mbrSn, post);
    }

    /**
     * 게시글 작성
     */
    @Transactional
    public PostResponse create(Long mbrSn, PostCreateRequest req) {
        checkBoard(req.bbsCd());
        List<MultipartFile> images = attached(req.images());
        if (images.size() > maxImages()) {
            throw new CustomException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        }

        Post post = new Post();
        post.setMbrSn(mbrSn);
        post.setBbsCd(req.bbsCd());
        post.setPstSttsCd(PostStatus.NORMAL.getCode());
        post.setPstTtl(req.pstTtl());
        post.setPstCn(req.pstCn());
        post.setMdfrId(actorId(mbrSn));
        postMapper.insertPost(post);
        insertHistory(post.getPstSn(), mbrSn, null, PostStatus.NORMAL);

        int sortSeq = 0;
        for (MultipartFile image : images) {
            insertImage(post.getPstSn(), fileStore.storePublic(image, actorId(mbrSn)), sortSeq++, mbrSn);
        }
        return toResponse(mbrSn, getNormalOrThrow(post.getPstSn()));
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public PostResponse update(Long mbrSn, Long pstSn, PostUpdateRequest req) {
        Post post = getNormalOrThrow(pstSn);
        ownerAuthorizer.checkOwner(mbrSn, post.getMbrSn());
        if (req.bbsCd() != null) {
            checkBoard(req.bbsCd());
        }

        List<MultipartFile> images = attached(req.images());
        List<PostImage> remaining = new ArrayList<>(postMapper.selectImages(pstSn));
        List<Long> removed = new ArrayList<>();
        if (req.keepAtchFileSns() != null) {
            for (PostImage image : remaining) {
                if (!req.keepAtchFileSns().contains(image.getAtchFileSn())) {
                    removed.add(image.getAtchFileSn());
                }
            }
            remaining.removeIf(image -> removed.contains(image.getAtchFileSn()));
        }
        if (remaining.size() + images.size() > maxImages()) {
            throw new CustomException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        }

        if (postMapper.updatePost(pstSn, req.bbsCd(), req.pstTtl(), req.pstCn(), actorId(mbrSn),
                PostStatus.NORMAL.getCode()) == 0) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        if (!removed.isEmpty()) {
            postMapper.updateImagesDeleted(pstSn, removed, actorId(mbrSn));
        }
        int sortSeq = remaining.stream().mapToInt(PostImage::getSortSeq).max().orElse(-1) + 1;
        for (MultipartFile image : images) {
            insertImage(pstSn, fileStore.storePublic(image, actorId(mbrSn)), sortSeq++, mbrSn);
        }
        return toResponse(mbrSn, getNormalOrThrow(pstSn));
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void delete(Long mbrSn, Long pstSn) {
        Post post = getNormalOrThrow(pstSn);
        ownerAuthorizer.checkOwner(mbrSn, post.getMbrSn());
        if (postMapper.updateStatus(pstSn, PostStatus.NORMAL.getCode(), PostStatus.DELETED.getCode(),
                actorId(mbrSn)) == 0) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        insertHistory(pstSn, mbrSn, PostStatus.NORMAL, PostStatus.DELETED);
    }

    /**
     * 좋아요 등록 (이미 누른 상태여도 현재 좋아요 수 반환)
     */
    @Transactional
    public PostLikeResponse like(Long mbrSn, Long pstSn) {
        getNormalOrThrow(pstSn);
        int changed = postMapper.updateLikeOn(pstSn, mbrSn);
        if (changed == 0) {
            changed = postMapper.insertLikeIgnore(pstSn, mbrSn);
        }
        if (changed > 0) {
            postMapper.updateRcmdtnCnt(pstSn, 1);
        }
        return new PostLikeResponse(pstSn, postMapper.selectRcmdtnCnt(pstSn));
    }

    /**
     * 좋아요 취소 (누르지 않은 상태여도 정상 처리)
     */
    @Transactional
    public void unlike(Long mbrSn, Long pstSn) {
        getNormalOrThrow(pstSn);
        if (postMapper.updateLikeOff(pstSn, mbrSn) > 0) {
            postMapper.updateRcmdtnCnt(pstSn, -1);
        }
    }

    /**
     * 정상 상태 게시글 조회
     */
    private Post getNormalOrThrow(Long pstSn) {
        Post post = postMapper.selectPost(pstSn, PostStatus.NORMAL.getCode());
        if (post == null) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    private void checkBoard(String bbsCd) {
        if (!BoardType.exists(bbsCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private boolean isPopularSort(String sort) {
        if (sort == null || sort.isBlank() || SORT_LATEST.equals(sort)) {
            return false;
        }
        if (SORT_POPULAR.equals(sort)) {
            return true;
        }
        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    /**
     * LIKE 와일드카드 문자 이스케이프
     */
    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private int maxImages() {
        return systemSettingService.get().getPstImgMaxCnt().intValue();
    }

    private String actorId(Long mbrSn) {
        return String.valueOf(mbrSn);
    }

    /**
     * 선택하지 않은 빈 파일 파트를 뺀 이미지 목록
     */
    private List<MultipartFile> attached(List<MultipartFile> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream().filter(image -> image != null && !image.isEmpty()).toList();
    }

    private void insertImage(Long pstSn, Long atchFileSn, int sortSeq, Long mbrSn) {
        PostImage image = new PostImage();
        image.setPstSn(pstSn);
        image.setAtchFileSn(atchFileSn);
        image.setSortSeq(sortSeq);
        image.setRgtrId(actorId(mbrSn));
        image.setMdfrId(actorId(mbrSn));
        postMapper.insertImage(image);
    }

    private void insertHistory(Long pstSn, Long mbrSn, PostStatus before, PostStatus after) {
        PostStatusHistory history = new PostStatusHistory();
        history.setPstSn(pstSn);
        history.setMbrSn(mbrSn);
        history.setBfrSttsCd(before == null ? null : before.getCode());
        history.setAftrSttsCd(after.getCode());
        postMapper.insertStatusHistory(history);
    }

    /**
     * 응답 변환 (작성자 닉네임·댓글 수·이미지·좋아요·본인 여부 포함)
     */
    private PostResponse toResponse(Long mbrSn, Post post) {
        List<PostImageSummary> imgs = postMapper.selectImages(post.getPstSn()).stream()
                .map(PostImageSummary::from).toList();
        return PostResponse.from(post, postMapper.selectMbrNcnm(post.getMbrSn()),
                postMapper.countComments(post.getPstSn()), imgs, postMapper.existsLike(post.getPstSn(), mbrSn),
                mbrSn.equals(post.getMbrSn()));
    }
}
