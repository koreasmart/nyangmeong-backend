package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.github.pagehelper.PageHelper;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.PostImage;
import com.nyangmeong.community.domain.PostStatusHistory;
import com.nyangmeong.community.dto.PostCreateRequest;
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
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;

/**
 * [검사 - 펫이야기 게시글 목록·상세·작성·수정·삭제·좋아요]
 */
class PostServiceTest {

    private static final Long MBR = 1L;
    private static final Long OTHER = 2L;
    private static final Long PST = 7L;
    private static final String NORMAL = "CMUC0003";

    private final PostMapper postMapper = mock(PostMapper.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private final PostService service = new PostService(postMapper, fileStore, new OwnerAuthorizer(),
            systemSettingService);

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    private Post post(Long mbrSn) {
        Post post = new Post();
        post.setPstSn(PST);
        post.setMbrSn(mbrSn);
        post.setBbsCd("CMUC0002");
        post.setPstSttsCd(NORMAL);
        post.setPstTtl("제목");
        post.setPstCn("내용");
        post.setInqCnt(10);
        post.setRcmdtnCnt(3);
        post.setRegDt(LocalDateTime.of(2026, 10, 26, 9, 0));
        return post;
    }

    private PostImage image(Long atchFileSn, int sortSeq) {
        PostImage image = new PostImage();
        image.setPstSn(PST);
        image.setAtchFileSn(atchFileSn);
        image.setSortSeq(sortSeq);
        return image;
    }

    private MultipartFile file(String name) {
        return new MockMultipartFile("images", name, "image/png", new byte[] {1});
    }

    private PostSummary summary(boolean popular) {
        PostSummary summary = new PostSummary();
        summary.setPstSn(PST);
        summary.setPopular(popular);
        return summary;
    }

    private void stubSystemSetting() {
        when(systemSettingService.get()).thenReturn(new SystemSetting());
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void 목록은_정상_글만_기본_10건으로_조회한다() {
        service.search(new PostSearchCond(), new PageQuery(null, null, null));

        verify(postMapper).selectList(null, null, false, NORMAL);
    }

    @Test
    void 게시판_필터와_키워드를_넘기고_키워드의_와일드카드는_이스케이프한다() {
        PostSearchCond cond = new PostSearchCond();
        cond.setBbsCd("CMUC0002");
        cond.setKeyword(" 50%_할인 ");

        service.search(cond, new PageQuery(null, null, "latest"));

        verify(postMapper).selectList("CMUC0002", "50\\%\\_할인", false, NORMAL);
    }

    @Test
    void 알_수_없는_게시판이나_정렬은_400() {
        PostSearchCond cond = new PostSearchCond();
        cond.setBbsCd("CMUC9999");
        assertError(() -> service.search(cond, new PageQuery(null, null, null)), ErrorCode.INVALID_INPUT_VALUE);

        assertError(() -> service.search(new PostSearchCond(), new PageQuery(null, null, "oldest")),
                ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 인기순이면_좋아요_순으로_조회하고_인기글_배지는_모두_뺀다() {
        when(postMapper.selectList(null, null, true, NORMAL)).thenReturn(List.of(summary(true), summary(true)));

        var res = service.search(new PostSearchCond(), new PageQuery(null, null, "popular"));

        assertThat(res.content()).extracting(PostSummary::getPopular).containsOnly(false);
    }

    @Test
    void 최신순이면_인기글_배지를_그대로_둔다() {
        when(postMapper.selectList(null, null, false, NORMAL)).thenReturn(List.of(summary(true)));

        var res = service.search(new PostSearchCond(), new PageQuery(null, null, null));

        assertThat(res.content().get(0).getPopular()).isTrue();
    }

    @Test
    void 인기_포토는_7일에_없으면_30일_그다음_전체_기간으로_넓힌다() {
        when(postMapper.selectPopularPhoto("CMUC0002", NORMAL, 7)).thenReturn(null);
        when(postMapper.selectPopularPhoto("CMUC0002", NORMAL, 30)).thenReturn(null);
        when(postMapper.selectPopularPhoto("CMUC0002", NORMAL, null)).thenReturn(post(OTHER));
        when(postMapper.selectImages(PST)).thenReturn(List.of(image(101L, 0)));

        PostResponse res = service.getPopularPhoto(MBR);

        assertThat(res.pstSn()).isEqualTo(PST);
        assertThat(res.imgs()).hasSize(1);
        verify(postMapper).selectPopularPhoto("CMUC0002", NORMAL, 7);
        verify(postMapper).selectPopularPhoto("CMUC0002", NORMAL, 30);
    }

    @Test
    void 인기_포토가_하나도_없으면_null() {
        assertThat(service.getPopularPhoto(MBR)).isNull();
    }

    @Test
    void 오늘의_건강팁은_펫정보_게시판에서_조회한다() {
        service.getHealthTips();

        verify(postMapper).selectHealthTips("CMUC0001", NORMAL);
    }

    @Test
    void 상세는_처음_열면_조회수가_오르고_응답에_반영된다() {
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(post(OTHER));
        when(postMapper.insertViewIgnore(PST, MBR)).thenReturn(1);
        when(postMapper.selectMbrNcnm(OTHER)).thenReturn("작성자");
        when(postMapper.countComments(PST)).thenReturn(2);
        when(postMapper.existsLike(PST, MBR)).thenReturn(true);

        PostResponse res = service.getPost(MBR, PST);

        verify(postMapper).updateInqCnt(PST);
        assertThat(res.inqCnt()).isEqualTo(11);
        assertThat(res.mbrNcnm()).isEqualTo("작성자");
        assertThat(res.cmntCnt()).isEqualTo(2);
        assertThat(res.liked()).isTrue();
        assertThat(res.owner()).isFalse();
    }

    @Test
    void 같은_회원이_같은_날_다시_열면_조회수는_그대로() {
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(post(MBR));
        when(postMapper.insertViewIgnore(PST, MBR)).thenReturn(0);

        PostResponse res = service.getPost(MBR, PST);

        verify(postMapper, never()).updateInqCnt(anyLong());
        assertThat(res.inqCnt()).isEqualTo(10);
        assertThat(res.owner()).isTrue();
    }

    @Test
    void 없거나_정상이_아닌_글의_상세는_404() {
        assertError(() -> service.getPost(MBR, PST), ErrorCode.POST_NOT_FOUND);
        verify(postMapper, never()).insertViewIgnore(anyLong(), anyLong());
    }

    @Test
    void 작성하면_정상_상태로_저장하고_이력과_이미지를_순서대로_남긴다() {
        stubSystemSetting();
        doAnswer(invocation -> {
            Post arg = invocation.getArgument(0);
            arg.setPstSn(PST);
            return 1;
        }).when(postMapper).insertPost(any(Post.class));
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(post(MBR));
        when(fileStore.storePublic(any(MultipartFile.class), anyString())).thenReturn(101L, 102L);

        service.create(MBR, new PostCreateRequest("CMUC0002", "제목", "내용", List.of(file("a.png"), file("b.png"))));

        ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insertPost(saved.capture());
        assertThat(saved.getValue().getPstSttsCd()).isEqualTo(NORMAL);
        assertThat(saved.getValue().getMbrSn()).isEqualTo(MBR);
        ArgumentCaptor<PostStatusHistory> history = ArgumentCaptor.forClass(PostStatusHistory.class);
        verify(postMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getBfrSttsCd()).isNull();
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo(NORMAL);
        ArgumentCaptor<PostImage> images = ArgumentCaptor.forClass(PostImage.class);
        verify(postMapper, times(2)).insertImage(images.capture());
        assertThat(images.getAllValues()).extracting(PostImage::getSortSeq).containsExactly(0, 1);
        assertThat(images.getAllValues()).extracting(PostImage::getAtchFileSn).containsExactly(101L, 102L);
    }

    @Test
    void 사진은_10장까지이고_11장이면_400() {
        stubSystemSetting();
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            files.add(file(i + ".png"));
        }

        assertError(() -> service.create(MBR, new PostCreateRequest("CMUC0001", "제목", "내용", files)),
                ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        verify(postMapper, never()).insertPost(any(Post.class));
    }

    @Test
    void 알_수_없는_게시판으로는_작성할_수_없다() {
        assertError(() -> service.create(MBR, new PostCreateRequest("CMUC9999", "제목", "내용", List.of())),
                ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 남의_글은_수정할_수_없다() {
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(post(OTHER));

        assertError(() -> service.update(MBR, PST, new PostUpdateRequest(null, "새 제목", null, null, null)),
                ErrorCode.NOT_OWNER);
        verify(postMapper, never()).updatePost(anyLong(), any(), any(), any(), anyString(), anyString());
    }

    @Test
    void 수정은_남길_사진에서_빠진_기존_사진을_지우고_새_사진을_뒤에_붙인다() {
        stubSystemSetting();
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(post(MBR));
        when(postMapper.selectImages(PST)).thenReturn(List.of(image(101L, 0), image(102L, 1)));
        when(postMapper.updatePost(eq(PST), any(), eq("새 제목"), any(), eq("1"), eq(NORMAL))).thenReturn(1);
        when(fileStore.storePublic(any(MultipartFile.class), anyString())).thenReturn(103L);

        service.update(MBR, PST,
                new PostUpdateRequest("CMUC0001", "새 제목", null, List.of(101L), List.of(file("c.png"))));

        verify(postMapper).updateImagesDeleted(PST, List.of(102L), "1");
        ArgumentCaptor<PostImage> added = ArgumentCaptor.forClass(PostImage.class);
        verify(postMapper).insertImage(added.capture());
        assertThat(added.getValue().getAtchFileSn()).isEqualTo(103L);
        assertThat(added.getValue().getSortSeq()).isEqualTo(1);
    }

    @Test
    void 수정해서_사진이_10장을_넘으면_400() {
        stubSystemSetting();
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(post(MBR));
        List<PostImage> existing = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            existing.add(image(100L + i, i));
        }
        when(postMapper.selectImages(PST)).thenReturn(existing);

        assertError(() -> service.update(MBR, PST,
                new PostUpdateRequest(null, null, null, null, List.of(file("a.png")))),
                ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
    }

    @Test
    void 남의_글은_삭제할_수_없다() {
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(post(OTHER));

        assertError(() -> service.delete(MBR, PST), ErrorCode.NOT_OWNER);
        verify(postMapper, never()).updateStatus(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void 삭제하면_삭제_상태로_바꾸고_이력을_남긴다() {
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(post(MBR));
        when(postMapper.updateStatus(PST, NORMAL, "CMUC0005", "1")).thenReturn(1);

        service.delete(MBR, PST);

        ArgumentCaptor<PostStatusHistory> history = ArgumentCaptor.forClass(PostStatusHistory.class);
        verify(postMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getBfrSttsCd()).isEqualTo(NORMAL);
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo("CMUC0005");
    }

    @Test
    void 좋아요를_처음_누르면_수가_하나_오른다() {
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(post(OTHER));
        when(postMapper.updateLikeOn(PST, MBR)).thenReturn(0);
        when(postMapper.insertLikeIgnore(PST, MBR)).thenReturn(1);
        when(postMapper.selectRcmdtnCnt(PST)).thenReturn(4);

        PostLikeResponse res = service.like(MBR, PST);

        verify(postMapper).updateRcmdtnCnt(PST, 1);
        assertThat(res.rcmdtnCnt()).isEqualTo(4);
    }

    @Test
    void 취소했던_좋아요를_다시_누르면_수가_하나_오른다() {
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(post(OTHER));
        when(postMapper.updateLikeOn(PST, MBR)).thenReturn(1);

        service.like(MBR, PST);

        verify(postMapper, never()).insertLikeIgnore(anyLong(), anyLong());
        verify(postMapper).updateRcmdtnCnt(PST, 1);
    }

    @Test
    void 이미_누른_좋아요를_또_눌러도_수는_그대로이고_현재_수를_돌려준다() {
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(post(OTHER));
        when(postMapper.selectRcmdtnCnt(PST)).thenReturn(3);

        PostLikeResponse res = service.like(MBR, PST);

        verify(postMapper, never()).updateRcmdtnCnt(anyLong(), anyInt());
        assertThat(res.rcmdtnCnt()).isEqualTo(3);
    }

    @Test
    void 좋아요_취소는_눌렀던_경우에만_수가_줄고_누르지_않았어도_정상() {
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(post(OTHER));
        when(postMapper.updateLikeOff(PST, MBR)).thenReturn(1, 0);

        service.unlike(MBR, PST);
        service.unlike(MBR, PST);

        verify(postMapper, times(1)).updateRcmdtnCnt(PST, -1);
    }
}
