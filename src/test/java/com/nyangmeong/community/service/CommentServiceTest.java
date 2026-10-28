package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.dto.CommentCreateRequest;
import com.nyangmeong.community.dto.CommentResponse;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.global.auth.OwnerAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [검사 - 댓글·대댓글 목록·등록·삭제]
 */
class CommentServiceTest {

    private static final Long MBR = 1L;
    private static final Long OTHER = 2L;
    private static final Long PST = 7L;
    private static final Long NEW_CMNT = 99L;
    private static final String NORMAL = "CMUC0003";

    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final CommentService service = new CommentService(commentMapper, postMapper, new OwnerAuthorizer());

    private Comment comment(Long cmntSn, Long prntCmntSn, Long pstSn, Long mbrSn) {
        Comment comment = new Comment();
        comment.setCmntSn(cmntSn);
        comment.setPrntCmntSn(prntCmntSn);
        comment.setPstSn(pstSn);
        comment.setMbrSn(mbrSn);
        comment.setCmntCn("댓글");
        comment.setRegDt(LocalDateTime.of(2026, 10, 26, 9, 0));
        return comment;
    }

    /** 등록한 댓글을 다시 조회하면 돌려주도록 준비 */
    private void stubInsert() {
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(new Post());
        when(postMapper.selectMbrNcnm(MBR)).thenReturn("닉네임");
        doAnswer(invocation -> {
            Comment arg = invocation.getArgument(0);
            arg.setCmntSn(NEW_CMNT);
            when(commentMapper.selectComment(NEW_CMNT))
                    .thenReturn(comment(NEW_CMNT, arg.getPrntCmntSn(), PST, MBR));
            return 1;
        }).when(commentMapper).insertComment(any(Comment.class));
    }

    private Long savedParent() {
        ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insertComment(saved.capture());
        return saved.getValue().getPrntCmntSn();
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void 최상위_댓글을_등록하면_부모_없이_저장하고_닉네임을_돌려준다() {
        stubInsert();

        CommentResponse res = service.create(MBR, PST, new CommentCreateRequest("댓글", null));

        assertThat(savedParent()).isNull();
        assertThat(res.cmntSn()).isEqualTo(NEW_CMNT);
        assertThat(res.mbrNcnm()).isEqualTo("닉네임");
    }

    @Test
    void 최상위_댓글에_답글을_달면_그_댓글이_부모() {
        stubInsert();
        when(commentMapper.selectComment(10L)).thenReturn(comment(10L, null, PST, OTHER));

        service.create(MBR, PST, new CommentCreateRequest("답글", 10L));

        assertThat(savedParent()).isEqualTo(10L);
    }

    @Test
    void 대댓글에_답글을_달면_원_댓글_스레드에_이어서_붙는다() {
        stubInsert();
        when(commentMapper.selectComment(20L)).thenReturn(comment(20L, 10L, PST, OTHER));

        service.create(MBR, PST, new CommentCreateRequest("답글의 답글", 20L));

        assertThat(savedParent()).isEqualTo(10L);
    }

    @Test
    void 다른_글이나_삭제된_댓글을_부모로_하면_404() {
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(new Post());
        when(commentMapper.selectComment(10L)).thenReturn(comment(10L, null, 8L, OTHER));

        assertError(() -> service.create(MBR, PST, new CommentCreateRequest("답글", 10L)),
                ErrorCode.COMMENT_NOT_FOUND);
        assertError(() -> service.create(MBR, PST, new CommentCreateRequest("답글", 11L)),
                ErrorCode.COMMENT_NOT_FOUND);
        verify(commentMapper, never()).insertComment(any(Comment.class));
    }

    @Test
    void 없는_글에는_댓글을_달_수_없다() {
        assertError(() -> service.create(MBR, PST, new CommentCreateRequest("댓글", null)),
                ErrorCode.POST_NOT_FOUND);
        assertError(() -> service.search(MBR, PST), ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 목록은_로그인_회원_번호로_본인_댓글을_표시한다() {
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(new Post());

        service.search(MBR, PST);

        verify(commentMapper).selectList(PST, MBR);
    }

    @Test
    void 본인_댓글은_삭제_처리한다() {
        when(commentMapper.selectComment(10L)).thenReturn(comment(10L, null, PST, MBR));

        service.delete(MBR, PST, 10L);

        verify(commentMapper).updateDeleted(10L);
    }

    @Test
    void 남의_댓글은_삭제할_수_없다() {
        when(commentMapper.selectComment(10L)).thenReturn(comment(10L, null, PST, OTHER));

        assertError(() -> service.delete(MBR, PST, 10L), ErrorCode.NOT_OWNER);
        verify(commentMapper, never()).updateDeleted(anyLong());
    }

    @Test
    void 다른_글의_댓글이거나_이미_삭제된_댓글이면_404() {
        when(commentMapper.selectComment(10L)).thenReturn(comment(10L, null, 8L, MBR));

        assertError(() -> service.delete(MBR, PST, 10L), ErrorCode.COMMENT_NOT_FOUND);
        assertError(() -> service.delete(MBR, PST, 11L), ErrorCode.COMMENT_NOT_FOUND);
    }
}
